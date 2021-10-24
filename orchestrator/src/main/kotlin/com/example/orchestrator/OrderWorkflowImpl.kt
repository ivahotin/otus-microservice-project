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
        .setStartToCloseTimeout(Duration.ofSeconds(1000))
        .setRetryOptions(RetryOptions
            .newBuilder()
            .setMaximumAttempts(1)
            .setDoNotRetry(SagaException::class.simpleName.toString())
            .build()
        )
        .build()
    private val activities = Workflow.newActivityStub(OrderActivities::class.java, activitiesOption)

    override fun start(event: CreatedOrderEvent) {
        val sagaOptions = Saga.Options.Builder().setParallelCompensation(true).build()
        val saga = Saga(sagaOptions)
        try {
            activities.placeOrder()
            saga.addCompensation(activities::declineOrder, event.orderId)

            val reservationId = activities.reserveItems(event.consumerId, event.orderId, event.items)
            saga.addCompensation(activities::cancelReservation, reservationId)

            val deliveryId = activities.reserveDelivery(event.orderId, event.delivery)
            saga.addCompensation(activities::cancelDelivery, deliveryId)

            val paymentId = activities.makePayment(event.consumerId, event.orderId, event.items)
            activities.confirmOrder(event.orderId, deliveryId, paymentId, reservationId)
        } catch (exc: ActivityFailure) {
            saga.compensate()
            throw exc
        }
    }
}