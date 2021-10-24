package com.example.orchestrator

import com.example.orchestrator.clients.BillingClient
import com.example.orchestrator.clients.DeliveryClient
import com.example.orchestrator.clients.InventoryClient
import com.example.orchestrator.clients.OrderClient
import com.example.orchestrator.dto.DeliveryDetail
import com.example.orchestrator.dto.Item
import java.util.UUID

class OrderActivitiesImpl(
    private val orderClient: OrderClient,
    private val billingClient: BillingClient,
    private val inventoryClient: InventoryClient,
    private val deliveryClient: DeliveryClient
) : OrderActivities {
    override fun placeOrder() {}

    override fun declineOrder(orderId: UUID) {
        orderClient.declineOrder(orderId)
    }

    override fun reserveItems(consumerId: UUID, orderId: UUID, items: List<Item>): Long =
        inventoryClient.makeReservation(consumerId, orderId, items)

    override fun cancelReservation(reservationId: Long) {
        inventoryClient.cancelReservation(reservationId)
    }

    override fun reserveDelivery(orderId: UUID, delivery: DeliveryDetail): Long =
        deliveryClient.reserveDelivery(orderId, delivery)

    override fun cancelDelivery(deliveryId: Long) {
        deliveryClient.cancelDelivery(deliveryId)
    }

    override fun makePayment(consumerId: UUID, orderId: UUID, items: List<Item>): Long {
        val totalAmount = items.sumOf { it.price * it.quantity }.toLong()
        return billingClient.makePayment(consumerId, orderId, totalAmount)
    }

    override fun confirmOrder(orderId: UUID, delivery: Long, paymentId: Long, reservationId: Long) {
        orderClient.confirmOrder(orderId, delivery, paymentId, reservationId)
    }

}