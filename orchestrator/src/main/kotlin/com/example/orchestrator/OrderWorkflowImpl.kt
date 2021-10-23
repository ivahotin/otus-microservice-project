package com.example.orchestrator

import com.example.orchestrator.dto.CreatedOrderEvent
import io.temporal.activity.ActivityOptions
import io.temporal.common.RetryOptions
import io.temporal.failure.ActivityFailure
import io.temporal.workflow.Saga
import io.temporal.workflow.Workflow
import java.time.Duration

class OrderWorkflowImpl: OrderWorkflow {

    private val activitiesOption = ActivityOptions.newBuilder()
        .setStartToCloseTimeout(Duration.ofSeconds(10))
        .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(1).build())
        .build()
    private val activities = Workflow.newActivityStub(OrderActivities::class.java, activitiesOption)

    override fun start(event: CreatedOrderEvent) {
        val sagaOptions = Saga.Options.Builder().setParallelCompensation(true).build()
        val saga = Saga(sagaOptions)

        try {
            activities.placeOrder()
            saga.addCompensation(activities::declineOrder, event.orderId)

            val reservationId = activities.reserveItems(event.orderId, event.items)
            saga.addCompensation(activities::cancelReservation, reservationId)

            val deliveryId = activities.reserveDelivery(event.orderId, event.delivery)
            saga.addCompensation(activities::cancelDelivery, deliveryId)

            activities.makePayment(event.consumerId, event.orderId, 100L)
            activities.confirmOrder(event.orderId)
        } catch (exc: ActivityFailure) {
            saga.compensate()
            throw exc
        }
    }

}