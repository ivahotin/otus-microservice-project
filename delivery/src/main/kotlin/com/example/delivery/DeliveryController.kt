package com.example.delivery

import com.example.delivery.dto.CreateDeliveryRequest
import com.example.delivery.dto.CreateDeliveryResponse
import java.util.UUID
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@RestController
class DeliveryController(private val deliveryRepository: DeliveryRepository) {

    @PostMapping("/deliveries")
    fun createDelivery(
        @RequestHeader("idempotency-key") idempotencyKey: UUID,
        @RequestBody request: CreateDeliveryRequest
    ): CreateDeliveryResponse {
        val command = CreateDeliveryCommand(
            idempotencyKey = idempotencyKey,
            type = request.type,
            city = request.city,
            deliveryDatetime = request.deliveryDatetime
        )
        val deliveryId = deliveryRepository.reserveDelivery(command)

        return CreateDeliveryResponse(deliveryId = deliveryId)
    }

    @GetMapping("/deliveries/{deliveryId}")
    fun getDeliveryById(@PathVariable deliveryId: Long): ResponseEntity<*> =
        when (val delivery = deliveryRepository.getDeliveryById(deliveryId)) {
            null -> ResponseEntity.status(HttpStatus.NOT_FOUND).build<Any?>()
            else -> ResponseEntity.ok(delivery)
        }
}