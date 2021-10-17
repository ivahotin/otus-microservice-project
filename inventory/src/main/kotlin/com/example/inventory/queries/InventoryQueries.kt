package com.example.inventory.queries

import com.example.inventory.domain.Item
import com.example.inventory.domain.Reservation
import java.util.UUID

interface InventoryQueries {
    fun getMyReservations(consumerId: UUID): List<Reservation>
    fun findItems(query: ItemsQuery): List<Item>
}