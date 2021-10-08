package com.example.order.domain

data class ConsumerOrderList(val consumerOrders: List<ConsumerOrder>) {
    val latestVersion = consumerOrders.maxOf { it.version }
}