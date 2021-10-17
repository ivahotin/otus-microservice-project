package com.example.inventory

import com.example.inventory.adapters.InventoryStorageAdapter
import com.example.inventory.domain.Item
import com.example.inventory.domain.Reservation
import com.example.inventory.queries.InventoryQueries
import com.example.inventory.queries.ItemsQuery
import com.example.inventory.usecases.AddItemCommand
import com.example.inventory.usecases.InsufficientAmount
import com.example.inventory.usecases.InventoryUseCases
import com.example.inventory.usecases.ReservationCommand
import com.example.inventory.usecases.ReservationMade
import com.example.inventory.usecases.ReservationResult
import java.util.UUID
import org.springframework.stereotype.Service

@Service
class InventoryService(private val storageAdapter: InventoryStorageAdapter): InventoryUseCases, InventoryQueries {

    override fun findItems(query: ItemsQuery): List<Item> =
        storageAdapter.findItems(query.term, query.cursor, query.limit)

    override fun addItem(command: AddItemCommand): Long =
        storageAdapter.insertItem(command.title, command.description, command.quantity, command.price)

    override fun reserveItems(idempotencyKey: UUID, command: ReservationCommand): ReservationResult {
        val subtotal = command.items.sumOf { it.price * it.quantity }
        val reservationId = storageAdapter.reserve(command.consumerId, idempotencyKey, command.items, subtotal)
        return if (reservationId == null) InsufficientAmount else ReservationMade(reservationId, subtotal)
    }

    override fun getMyReservations(consumerId: UUID): List<Reservation> =
        storageAdapter.getMyReservations(consumerId)
}