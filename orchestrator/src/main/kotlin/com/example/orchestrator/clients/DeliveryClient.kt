package com.example.orchestrator.clients

import com.example.orchestrator.dto.DeliveryDetail
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer
import java.io.Serializable
import java.time.LocalDateTime
import java.util.UUID
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
class DeliveryClient(
    @Value("\${delivery.url}") private val deliveryUrl: String,
    @Value("\${delivery.port}") private val deliveryPort: String,
    private val restTemplate: RestTemplate
) {

    data class ReservationRequest(
        val type: String,
        val city: String,
        @JsonDeserialize(using = LocalDateTimeDeserializer::class)
        @JsonSerialize(using = LocalDateTimeSerializer::class)
        val deliveryDatetime: LocalDateTime
    ): Serializable

    data class ReservationResponse(val deliveryId: Long): Serializable

    fun reserveDelivery(orderId: UUID, delivery: DeliveryDetail): Long {
        val url = "http://$deliveryUrl:$deliveryPort/deliveries"
        val httpHeaders = HttpHeaders().also {
            it.add("idempotency-key", orderId.toString())
        }
        val request = HttpEntity<ReservationRequest>(
            ReservationRequest(
                delivery.type,
                delivery.city,
                delivery.datetime
            ),
            httpHeaders
        )

        return restTemplate.postForEntity(url, request, ReservationResponse::class.java).body?.deliveryId
            ?: throw Exception("Something goes wrong")
    }

    fun cancelDelivery(deliveryId: Long) {
        val url = "http://$deliveryUrl:$deliveryPort/deliveries/$deliveryId/cancellation"
        restTemplate.postForLocation(url, null)
    }
}