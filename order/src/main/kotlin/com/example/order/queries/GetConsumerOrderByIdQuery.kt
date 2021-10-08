package com.example.order.queries

import com.example.order.domain.ConsumerOrder
import java.util.UUID

interface GetConsumerOrderByIdQuery {
    fun getConsumerOrderById(orderId: UUID, consumerId: UUID): ConsumerOrder?
}