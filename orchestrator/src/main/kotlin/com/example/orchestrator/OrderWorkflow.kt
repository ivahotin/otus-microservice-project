package com.example.orchestrator

import io.temporal.workflow.WorkflowInterface
import io.temporal.workflow.WorkflowMethod

@WorkflowInterface
interface OrderWorkflow {

    @WorkflowMethod
    fun start(event: CreatedOrderEvent)

    companion object {
        const val taskList = "orders"
    }
}