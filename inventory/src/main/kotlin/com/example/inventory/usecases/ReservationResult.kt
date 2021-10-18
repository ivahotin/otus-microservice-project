package com.example.inventory.usecases

sealed interface ReservationResult
data class ReservationMade(val reservationId: Long): ReservationResult
object InsufficientAmount: ReservationResult