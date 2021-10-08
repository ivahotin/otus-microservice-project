package com.example.order.usecases

import com.example.order.domain.OrderCreationResult
import java.util.UUID

interface ConsumerOrderUseCases {
    fun createOrder(createCommand: CreateOrderCommand): OrderCreationResult
    fun declineOrder(orderId: UUID)
    fun approveOrder(approveOrderCommand: ApproveOrderCommand)
}