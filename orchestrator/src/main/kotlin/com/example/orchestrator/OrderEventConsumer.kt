package com.example.orchestrator

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.temporal.api.enums.v1.WorkflowIdReusePolicy
import io.temporal.client.WorkflowClient
import io.temporal.client.WorkflowExecutionAlreadyStarted
import io.temporal.client.WorkflowOptions
import io.temporal.common.RetryOptions
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

data class Payload(
    @JsonProperty("order_id")
    val orderId: UUID,
    val payload: String
)

data class Message(
    val payload: Payload
)

data class DeliveryDetail(
    val type: String,
    val city: String,
    @JsonDeserialize(using = LocalDateTimeDeserializer::class)
    @JsonSerialize(using = LocalDateTimeSerializer::class)
    val datetime: LocalDateTime
)

data class Item(
    val itemId: Long,
    val title: String,
    val description: String,
    val quantity: Int,
    val price: Int
)

data class CreatedOrderEvent(
    val orderId: UUID,
    val items: List<Item>,
    val delivery: DeliveryDetail,
    val price: Long,
    val consumerId: UUID
)

@Component
class OrderEventConsumer(private val workflowClient: WorkflowClient) {

    private val objectMapper = ObjectMapper().also {
        it.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        it.registerModule(JavaTimeModule())
        it.registerKotlinModule()
    }
    private val messageTypeToken = object : TypeReference<Message>() {}
    private val payloadTypeToken = object : TypeReference<CreatedOrderEvent>() {}
    private val retryOptions = RetryOptions.newBuilder()
        .setMaximumAttempts(4)
        .setBackoffCoefficient(2.0)
        .build()

    @KafkaListener(
        topics = ["\${spring.kafka.consumer.topic}"],
        groupId = "\${spring.kafka.consumer.group-id}"
    )
    fun processMessage(messages: List<String>, ack: Acknowledgment) {
        messages.map { msg ->
            val deserializedMessage = objectMapper.readValue(msg, messageTypeToken)
            val deserializedPayload = objectMapper.readValue(deserializedMessage.payload.payload, payloadTypeToken)

            val workflow = workflowClient.newWorkflowStub(
                OrderWorkflow::class.java,
                WorkflowOptions.newBuilder()
                    .setRetryOptions(retryOptions)
                    .setWorkflowIdReusePolicy(WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_ALLOW_DUPLICATE_FAILED_ONLY)
                    .setWorkflowId(deserializedPayload.orderId.toString())
                    .setWorkflowExecutionTimeout(Duration.ofMinutes(10))
                    .setTaskQueue(OrderWorkflow.taskList)
                    .build()
            )
            try {
                WorkflowClient.start(workflow::start, deserializedPayload)
            } catch (exc: WorkflowExecutionAlreadyStarted) { }
        }
        println("Message received by consumer 1: $messages")
        ack.acknowledge()
    }
}