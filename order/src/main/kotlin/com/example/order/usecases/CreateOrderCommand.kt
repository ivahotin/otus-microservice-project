package com.example.order.usecases

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer
import java.time.LocalDateTime
import java.util.UUID

data class CreateOrderCommand(
    val consumerId: UUID,
    val delivery: DeliveryDetail,
    val items: List<Item>,
    val version: Long
)

data class DeliveryDetail(
    val type: String,
    val city: String,
    @JsonDeserialize(using = LocalDateTimeDeserializer::class)
    @JsonSerialize(using = LocalDateTimeSerializer::class)
    val datetime: LocalDateTime
)

data class Item(
    val itemId: Long,
    val title: String,
    val description: String,
    val quantity: Int,
    val price: Int
)