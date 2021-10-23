package com.example.orchestrator

import com.example.orchestrator.dto.DeliveryDetail
import com.example.orchestrator.dto.Item
import java.util.UUID

class OrderActivitiesImpl : OrderActivities {

    override fun placeOrder() {
        TODO("Not yet implemented")
    }

    override fun declineOrder(orderId: UUID) {
        TODO("Not yet implemented")
    }

    override fun reserveItems(orderId: UUID, items: List<Item>): Long {
        TODO("Not yet implemented")
    }

    override fun cancelReservation(reservationId: Long) {
        TODO("Not yet implemented")
    }

    override fun reserveDelivery(orderId: UUID, delivery: DeliveryDetail): Long {
        TODO("Not yet implemented")
    }

    override fun cancelDelivery(deliveryId: Long) {
        TODO("Not yet implemented")
    }

    override fun makePayment(consumerId: UUID, orderId: UUID, amount: Long) {
        TODO("Not yet implemented")
    }

    override fun confirmOrder(orderId: UUID) {
        TODO("Not yet implemented")
    }

}