package com.example.orchestrator.clients

import com.example.orchestrator.SagaException
import com.example.orchestrator.dto.Item
import java.io.Serializable
import java.util.UUID
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate

@Component
class InventoryClient(
    @Value("\${inventory.url}") private val inventoryUrl: String,
    @Value("\${inventory.port}") private val inventoryPort: String,
    private val restTemplate: RestTemplate
) {

    data class ItemReservation(val itemId: Long, val quantity: Int): Serializable
    data class Reservation(
        val consumerId: UUID,
        val items: List<ItemReservation>
    ): Serializable

    data class ReservationResult(val reservationId: Long)

    fun cancelReservation(reservationId: Long) {
        val url = "http://$inventoryUrl:$inventoryPort/items/reservations/cancellations/$reservationId"
        restTemplate.postForLocation(url, null)
    }

    fun makeReservation(consumerId: UUID, orderId: UUID, items: List<Item>): Long {
        val url = "http://$inventoryUrl:$inventoryPort/items/reservations"
        val httpHeaders = HttpHeaders().also {
            it.add("idempotency-key", orderId.toString())
        }
        val request = HttpEntity<Reservation>(
            Reservation(
                consumerId,
                items = items.map { ItemReservation(it.itemId, it.quantity) }
            ),
            httpHeaders
        )

        return try {
            val requestBody = restTemplate.postForEntity(url, request, ReservationResult::class.java).body
            requestBody?.reservationId ?: throw Exception("Something goes wrong")
        } catch (exc: HttpClientErrorException) {
            when (exc.statusCode) {
                HttpStatus.PRECONDITION_FAILED -> throw SagaException("Not enough inventory")
                else -> throw Exception("Something goes wrong")
            }
        }
    }
}