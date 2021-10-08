package com.example.order.dto

import com.example.order.usecases.DeliveryDetail
import com.example.order.usecases.Item
import java.util.UUID

data class CreateOrderRequest(
    val consumerId: UUID,
    val delivery: DeliveryDetail,
    val items: List<Item>
)
