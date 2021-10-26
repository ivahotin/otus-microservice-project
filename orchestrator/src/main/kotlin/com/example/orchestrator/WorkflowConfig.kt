package com.example.orchestrator

import com.example.orchestrator.clients.BillingClient
import com.example.orchestrator.clients.DeliveryClient
import com.example.orchestrator.clients.InventoryClient
import com.example.orchestrator.clients.OrderClient
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.temporal.client.WorkflowClient
import io.temporal.client.WorkflowClientOptions
import io.temporal.common.converter.DefaultDataConverter
import io.temporal.common.converter.JacksonJsonPayloadConverter
import io.temporal.serviceclient.WorkflowServiceStubs
import io.temporal.serviceclient.WorkflowServiceStubsOptions
import io.temporal.worker.WorkerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component

@Component
@Configuration
class WorkflowConfig(
    @Value("\${temporal.host}") private val temporalHost: String,
    @Value("\${temporal.port}") private val temporalPort: String,
    @Value("\${temporal.namespace}") private val temporalNamespace: String
) {

    private val temporalServiceAddress = "$temporalHost:$temporalPort"
    private val objectMapper = ObjectMapper().also {
        it.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        it.registerModule(JavaTimeModule())
        it.registerKotlinModule()
    }

    @Bean
    fun workflowServiceStubs(): WorkflowServiceStubs =
        WorkflowServiceStubs
            .newInstance(WorkflowServiceStubsOptions.newBuilder().setTarget(temporalServiceAddress).build())

    @Bean
    fun workflowClient(workflowServiceStubs: WorkflowServiceStubs): WorkflowClient =
        WorkflowClient.newInstance(
            workflowServiceStubs,
            WorkflowClientOptions
                .newBuilder()
                .setNamespace(temporalNamespace)
                .setDataConverter(DefaultDataConverter(JacksonJsonPayloadConverter(objectMapper)))
                .build()
        )

    @Bean
    fun workerFactory(workflowClient: WorkflowClient): WorkerFactory =
        WorkerFactory.newInstance(workflowClient)

    @Bean
    fun getActivityImpl(
        orderClient: OrderClient,
        billingClient: BillingClient,
        inventoryClient: InventoryClient,
        deliveryClient: DeliveryClient
    ): OrderActivities = OrderActivitiesImpl(orderClient, billingClient, inventoryClient, deliveryClient)
}