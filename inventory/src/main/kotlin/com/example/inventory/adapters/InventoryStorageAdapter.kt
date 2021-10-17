package com.example.inventory.adapters

import com.example.inventory.domain.Item
import com.example.inventory.domain.Reservation
import java.util.UUID

interface InventoryStorageAdapter {
    fun findItems(term: String, offsetId: Long? = null, limit: Int): List<Item>
    fun insertItem(title: String, description: String, quantity: Int, price: Int): Long
    fun getMyReservations(consumerId: UUID): List<Reservation>
    fun reserve(
        consumerId: UUID,
        idempotencyKey: UUID,
        items: List<Item>,
        subtotal: Int
    ): Long?
}