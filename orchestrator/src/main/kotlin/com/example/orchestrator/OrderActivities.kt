package com.example.orchestrator

import com.example.orchestrator.dto.DeliveryDetail
import com.example.orchestrator.dto.Item
import io.temporal.activity.ActivityInterface
import io.temporal.activity.ActivityMethod
import java.util.UUID

@ActivityInterface
interface OrderActivities {

    @ActivityMethod
    fun placeOrder()
    @ActivityMethod
    fun declineOrder(orderId: UUID)

    @ActivityMethod
    fun reserveItems(orderId: UUID, items: List<Item>): Long
    @ActivityMethod
    fun cancelReservation(reservationId: Long)

    @ActivityMethod
    fun reserveDelivery(orderId: UUID, delivery: DeliveryDetail): Long
    @ActivityMethod
    fun cancelDelivery(deliveryId: Long)

    @ActivityMethod
    fun makePayment(consumerId: UUID, orderId: UUID, amount: Long)

    @ActivityMethod
    fun confirmOrder(orderId: UUID)
}