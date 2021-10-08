package com.example.order.queries

import com.example.order.domain.ConsumerOrderList
import java.util.UUID

interface GetConsumerOrdersByConsumerIdQuery {
    fun getConsumerOrdersByConsumerId(consumerId: UUID): ConsumerOrderList
}