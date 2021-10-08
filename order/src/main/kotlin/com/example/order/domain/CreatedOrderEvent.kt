package com.example.order.domain

import com.example.order.usecases.DeliveryDetail
import com.example.order.usecases.Item
import java.util.UUID

data class CreatedOrderEvent(
    val orderId: UUID,
    val items: List<Item>,
    val delivery: DeliveryDetail,
    val price: Long,
    val consumerId: UUID
)
