package com.example.order

import com.example.order.domain.OrderCreated
import com.example.order.domain.OrderCreationConflict
import com.example.order.dto.CreateOrderRequest
import com.example.order.dto.CreateOrderResponse
import com.example.order.queries.GetConsumerOrderByIdQuery
import com.example.order.queries.GetConsumerOrdersByConsumerIdQuery
import com.example.order.usecases.ApproveOrderCommand
import com.example.order.usecases.ConsumerOrderUseCases
import com.example.order.usecases.CreateOrderCommand
import java.util.UUID
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@RestController
class OrderController(
    private val consumerOrderUseCases: ConsumerOrderUseCases,
    private val getConsumerOrdersByConsumerIdQuery: GetConsumerOrdersByConsumerIdQuery,
    private val getConsumerOrderByIdQuery: GetConsumerOrderByIdQuery) {

    @PostMapping("/orders")
    fun createOrder(
        @RequestBody createOrderRequest: CreateOrderRequest,
        @RequestHeader("x-user-id") userId: UUID,
        @RequestHeader("If-Match", required = false) lastVersion: Long?,
    ): ResponseEntity<*> {
        val clientVersion = lastVersion ?: 0

        val command = CreateOrderCommand(
            consumerId = userId,
            version = clientVersion,
            delivery = createOrderRequest.delivery,
            items = createOrderRequest.items
        )
        return when (val orderCreationResult = consumerOrderUseCases.createOrder(command)) {
            is OrderCreationConflict -> ResponseEntity
                .status(HttpStatus.CONFLICT)
                .header("ETag", orderCreationResult.latestVersion.toString())
                .build<Any?>()
            is OrderCreated -> ResponseEntity
                .status(HttpStatus.OK)
                .header("ETag", orderCreationResult.latestVersion.toString())
                .body(CreateOrderResponse(orderId = orderCreationResult.orderId))
        }
    }

    @PutMapping("/orders/approvals")
    fun approveOrder(@RequestBody approveCommand: ApproveOrderCommand): ResponseEntity<*> {
        consumerOrderUseCases.approveOrder(approveCommand)
        return ResponseEntity
            .ok()
            .build<Any?>()
    }

    @PutMapping("/orders/declines/{orderId}")
    fun declineOrder(@PathVariable orderId: UUID): ResponseEntity<*> {
        consumerOrderUseCases.declineOrder(orderId)
        return ResponseEntity
            .ok()
            .build<Any?>()
    }

    @GetMapping("/orders")
    fun getConsumerOrders(@RequestHeader("x-user-id") userId: UUID): ResponseEntity<*> {
        val consumerOrderList = getConsumerOrdersByConsumerIdQuery.getConsumerOrdersByConsumerId(userId)

        return ResponseEntity
            .status(HttpStatus.OK)
            .header("ETag", consumerOrderList.latestVersion.toString())
            .body(consumerOrderList.consumerOrders)
    }

    @GetMapping("/orders/{orderId}")
    fun getConsumerOrderById(
        @PathVariable orderId: UUID,
        @RequestHeader("x-user-id") userId: UUID
    ): ResponseEntity<*> {
        val consumerOrder = getConsumerOrderByIdQuery.getConsumerOrderById(orderId, userId)
            ?: return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .build<Any?>()

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(consumerOrder)
    }
}