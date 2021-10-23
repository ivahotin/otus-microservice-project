package com.example.orchestrator.dto

import java.util.UUID

data class CreatedOrderEvent(
    val orderId: UUID,
    val items: List<Item>,
    val delivery: DeliveryDetail,
    val price: Long,
    val consumerId: UUID
)