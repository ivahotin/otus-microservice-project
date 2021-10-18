package com.example.inventory

import com.example.inventory.domain.Reservation
import com.example.inventory.dto.AddItemResponse
import com.example.inventory.dto.ItemsResponse
import com.example.inventory.dto.ReserveResponse
import com.example.inventory.queries.InventoryQueries
import com.example.inventory.queries.ItemsQuery
import com.example.inventory.usecases.AddItemCommand
import com.example.inventory.usecases.InsufficientAmount
import com.example.inventory.usecases.InventoryUseCases
import com.example.inventory.usecases.ReservationCommand
import com.example.inventory.usecases.ReservationMade
import java.util.UUID
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class InventoryController(
    private val inventoryUseCases: InventoryUseCases,
    private val inventoryQueries: InventoryQueries
) {

    @GetMapping("/items")
    fun getItems(
        @RequestParam("term") term: String,
        @RequestParam("cursor") cursor: Long?,
        @RequestParam("limit") limit: Int
    ): ItemsResponse {
        val query = ItemsQuery(term, cursor, limit)
        val items = inventoryQueries.findItems(query)
        val nextCursor = items.maxOfOrNull { it.itemId } ?: 0L
        return ItemsResponse(
            items,
            query.cursor,
            nextCursor
        )
    }

    @PostMapping("/items")
    fun addItems(@RequestBody addItemRequest: AddItemCommand): AddItemResponse =
        AddItemResponse(inventoryUseCases.addItem(addItemRequest))

    @PostMapping("/items/reservations")
    fun reserveItems(
        @RequestHeader("idempotency-key") idempotencyKey: UUID,
        @RequestBody reservationCommand: ReservationCommand
    ): ResponseEntity<*> {
        return when (val result = inventoryUseCases.reserveItems(idempotencyKey, reservationCommand)) {
            is InsufficientAmount -> ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).build<Any?>()
            is ReservationMade -> ResponseEntity.ok(ReserveResponse(
                reservationId = result.reservationId
            ))
        }
    }

    @PostMapping("/items/reservations/cancellations/{reservationId}")
    fun cancelReservation(@PathVariable("reservationId") reservationId: Long) {
        inventoryUseCases.cancelReservation(reservationId)
    }

    @GetMapping("/items/reservations")
    fun getAllUsersReservations(@RequestHeader("x-user-id") consumerId: UUID): List<Reservation> =
        inventoryQueries.getMyReservations(consumerId)
}