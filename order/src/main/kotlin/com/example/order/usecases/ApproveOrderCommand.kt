package com.example.order.usecases

import java.util.UUID

data class ApproveOrderCommand(
    val orderId: UUID,
    val deliveryId: String,
    val paymentId: Long,
    val reservationId: Long
)