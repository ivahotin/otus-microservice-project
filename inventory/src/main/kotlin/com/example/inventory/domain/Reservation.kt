package com.example.inventory.domain

import com.example.inventory.usecases.ReservationItem
import java.util.UUID

data class Reservation(
    val id: Long,
    val consumerId: UUID,
    val idempotencyKey: UUID,
    val items: List<ReservationItem>,
    val subtotal: Int,
    val isCancelled: Boolean
)
