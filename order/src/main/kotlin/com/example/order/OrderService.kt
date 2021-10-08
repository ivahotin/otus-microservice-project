package com.example.order

import com.example.order.adapters.ConsumerOrderAdapter
import com.example.order.domain.ConsumerOrder
import com.example.order.domain.ConsumerOrderList
import com.example.order.domain.ConsumerOrderStatus
import com.example.order.domain.CreatedOrderEvent
import com.example.order.domain.OrderCreationResult
import com.example.order.queries.GetConsumerOrderByIdQuery
import com.example.order.queries.GetConsumerOrdersByConsumerIdQuery
import com.example.order.usecases.ApproveOrderCommand
import com.example.order.usecases.ConsumerOrderUseCases
import com.example.order.usecases.CreateOrderCommand
import java.util.UUID
import org.springframework.stereotype.Service

@Service
class OrderService(private val consumerOrderAdapter: ConsumerOrderAdapter) :
    GetConsumerOrderByIdQuery,
    GetConsumerOrdersByConsumerIdQuery,
    ConsumerOrderUseCases
{
    override fun getConsumerOrderById(orderId: UUID, consumerId: UUID): ConsumerOrder? =
        consumerOrderAdapter.getConsumerOrderById(orderId, consumerId)

    override fun getConsumerOrdersByConsumerId(consumerId: UUID): ConsumerOrderList {
        val consumerOrders = consumerOrderAdapter.getConsumerOrdersByConsumerId(consumerId)
        return ConsumerOrderList(consumerOrders)
    }

    override fun declineOrder(orderId: UUID) {
        consumerOrderAdapter.updateOrderStatus(
            orderId = orderId,
            status = ConsumerOrderStatus.FAILED,
            deliveryId = null,
            paymentId = null,
            reservationId = null
        )
    }

    override fun approveOrder(approveOrderCommand: ApproveOrderCommand) {
        consumerOrderAdapter.updateOrderStatus(
            orderId = approveOrderCommand.orderId,
            status = ConsumerOrderStatus.PROCESSED,
            deliveryId = approveOrderCommand.deliveryId,
            paymentId = approveOrderCommand.paymentId,
            reservationId = approveOrderCommand.reservationId
        )
    }

    override fun createOrder(createCommand: CreateOrderCommand): OrderCreationResult {
        val consumerOrder = ConsumerOrder(
            id = UUID.randomUUID(),
            consumerId = createCommand.consumerId,
            price = createCommand.items.sumOf { it.price * it.quantity }.toLong(),
            version = createCommand.version,
            status = ConsumerOrderStatus.PENDING,
            deliveryId = null,
            reservationId = null,
            paymentId = null
        )
        val orderCreatedEvent = CreatedOrderEvent(
            orderId = consumerOrder.id,
            consumerId = consumerOrder.consumerId,
            price = consumerOrder.price,
            delivery = createCommand.delivery,
            items = createCommand.items
        )

        return consumerOrderAdapter.createConsumerOrder(consumerOrder, orderCreatedEvent)
    }
}