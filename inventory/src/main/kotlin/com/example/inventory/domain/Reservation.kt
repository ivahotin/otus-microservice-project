package com.example.inventory.domain

import java.util.UUID

data class Reservation(
    val id: Long,
    val consumerId: UUID,
    val idempotencyKey: UUID,
    val items: List<Item>,
    val subtotal: Int
)
