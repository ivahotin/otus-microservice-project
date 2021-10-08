package com.example.order.domain

import java.util.UUID

data class ConsumerOrder(
    val id: UUID,
    val consumerId: UUID,
    val price: Long,
    val version: Long,
    val status: ConsumerOrderStatus,
    val deliveryId: String? = null,
    val reservationId: Long? = null,
    val paymentId: Long? = null,
)
