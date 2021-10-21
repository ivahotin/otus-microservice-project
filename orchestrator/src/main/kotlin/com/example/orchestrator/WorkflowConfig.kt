package com.example.orchestrator

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
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component

@Component
@Configuration
class WorkflowConfig {

    private val temporalServiceAddress = "127.0.0.1:7233"
    private val temporalNamespace = "default"
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
}