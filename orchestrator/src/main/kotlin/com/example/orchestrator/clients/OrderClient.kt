package com.example.orchestrator.clients

import java.io.Serializable
import java.util.UUID
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
class OrderClient(
    @Value("\${order.url}") private val orderUrl: String,
    @Value("\${order.port}") private val orderPort: String,
    private val restTemplate: RestTemplate
) {

    data class Approval(
        val orderId: UUID,
        val deliveryId: String,
        val paymentId: Long,
        val reservationId: Long
    ): Serializable

    fun confirmOrder(orderId: UUID, deliveryId: Long, paymentId: Long, reservationId: Long) {
        val url = "http://$orderUrl:$orderPort/orders/approvals"
        val request = HttpEntity<Approval>(Approval(orderId, deliveryId.toString(), paymentId, reservationId))
        restTemplate.put(url, request)
    }

    fun declineOrder(orderId: UUID) {
        val url = "http://$orderUrl:$orderPort/orders/declines/$orderId"
        restTemplate.put(url, "null")
    }
}