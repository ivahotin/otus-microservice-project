package com.example.delivery

import java.time.LocalDateTime
import java.util.UUID

data class CreateDeliveryCommand(
    val idempotencyKey: UUID,
    val type: String,
    val city: String,
    val deliveryDatetime: LocalDateTime
)