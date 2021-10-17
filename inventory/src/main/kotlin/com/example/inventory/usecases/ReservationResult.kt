package com.example.inventory.usecases

sealed interface ReservationResult
data class ReservationMade(val reservationId: Long, val subtotal: Int): ReservationResult
object InsufficientAmount: ReservationResult