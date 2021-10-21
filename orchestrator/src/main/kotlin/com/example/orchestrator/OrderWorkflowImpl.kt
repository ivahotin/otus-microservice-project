package com.example.orchestrator

class OrderWorkflowImpl: OrderWorkflow {

    override fun start(event: CreatedOrderEvent) {
        println("Start")
    }

}