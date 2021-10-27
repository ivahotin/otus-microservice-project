package com.example.order.domain

data class ConsumerOrderList(val consumerOrders: List<ConsumerOrder>) {
    val latestVersion = consumerOrders.maxOfOrNull { it.version } ?: 0
}