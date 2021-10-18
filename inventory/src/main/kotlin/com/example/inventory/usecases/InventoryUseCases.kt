package com.example.inventory.usecases

import java.util.UUID

interface InventoryUseCases {
    fun addItem(command: AddItemCommand): Long
    fun reserveItems(idempotencyKey: UUID, command: ReservationCommand): ReservationResult
    fun cancelReservation(reservationId: Long)
}