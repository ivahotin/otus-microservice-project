package com.example.order.adapters

import com.example.order.domain.ConsumerOrder
import com.example.order.domain.ConsumerOrderStatus
import com.example.order.domain.CreatedOrderEvent
import com.example.order.domain.OrderCreationResult
import java.util.UUID

interface ConsumerOrderAdapter {
    fun createConsumerOrder(consumerOrder: ConsumerOrder, orderCreatedEvent: CreatedOrderEvent): OrderCreationResult
    fun getConsumerOrderById(orderId: UUID, consumerId: UUID): ConsumerOrder?
    fun getConsumerOrdersByConsumerId(consumerId: UUID): List<ConsumerOrder>
    fun updateOrderStatus(
        orderId: UUID,
        status: ConsumerOrderStatus,
        deliveryId: String? = null,
        reservationId: Long? = null,
        paymentId: Long? = null
    )
}