package com.example.order.domain

import java.util.UUID

sealed interface OrderCreationResult

data class OrderCreated(val orderId: UUID, val latestVersion: Long): OrderCreationResult
data class OrderCreationConflict(val latestVersion: Long): OrderCreationResult