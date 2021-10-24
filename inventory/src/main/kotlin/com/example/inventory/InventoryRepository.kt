package com.example.inventory

import com.example.inventory.adapters.InventoryStorageAdapter
import com.example.inventory.domain.Item
import com.example.inventory.domain.Reservation
import com.example.inventory.usecases.ReservationItem
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.util.UUID
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

private const val findItemsQuery = "select id, title, description, quantity, price from items where title like ? and id > ? order by id asc limit ?"
private const val addItemStmt = "insert into items (title, description, quantity, price) values (?, ?, ?, ?) returning id"
private const val getMyReservationsQuery = "select id, consumer_id, idempotency_key, items, subtotal, is_cancelled from reservations where consumer_id = ?::uuid"
private const val updateItems = "update items set quantity = quantity - ? where id = ? and quantity >= ?"
private const val insertReservation = "insert into reservations (consumer_id, idempotency_key, items, subtotal) values (?::uuid, ?::uuid, ?::jsonb, ?) on conflict (idempotency_key) do nothing"
private const val getReservationByIdempotency = "select id from reservations where idempotency_key = ?::uuid"
private const val getReservationById = "select items, is_cancelled from reservations where id = ?"
private const val cancelReservation = "update reservations set is_cancelled = true where id = ?"
private const val returnItems = "update items set quantity = quantity + ? where id = ?"

@Repository
class InventoryRepository(private val jdbcTemplate: JdbcTemplate): InventoryStorageAdapter {

    private val objectMapper = ObjectMapper().also {
        it.registerModule(JavaTimeModule())
        it.registerKotlinModule()
    }
    private val reservationItemsTypeReference = object : TypeReference<List<ReservationItem>>() {}

    override fun findItems(term: String, offsetId: Long?, limit: Int): List<Item> {
        return jdbcTemplate.query(
            findItemsQuery,
            { rs, _ ->
                Item(
                    itemId = rs.getLong("id"),
                    title = rs.getString("title"),
                    description = rs.getString("description"),
                    quantity = rs.getInt("quantity"),
                    price = rs.getInt("price")
                )
            },
            "$term%",
            offsetId ?: 0L,
            limit
        )
    }

    override fun insertItem(title: String, description: String, quantity: Int, price: Int): Long {
        val keyHolder = GeneratedKeyHolder()
        jdbcTemplate.update(
            { conn ->
                val ps = conn.prepareStatement(addItemStmt, arrayOf("id"))
                ps.setString(1, title)
                ps.setString(2, description)
                ps.setInt(3, quantity)
                ps.setInt(4, price)
                ps
            },
            keyHolder
        )

        return keyHolder.key?.toLong() ?: throw Exception("Something goes wrong")
    }

    override fun getMyReservations(consumerId: UUID): List<Reservation> {
        return jdbcTemplate.query(
            getMyReservationsQuery,
            { rs, _ ->
                val serializedItems = rs.getString("items")
                val items = objectMapper.readValue(serializedItems, reservationItemsTypeReference)
                Reservation(
                    id = rs.getLong("id"),
                    consumerId = UUID.fromString(rs.getString("consumer_id")),
                    idempotencyKey = UUID.fromString(rs.getString("idempotency_key")),
                    subtotal = rs.getInt("subtotal"),
                    items = items,
                    isCancelled = rs.getBoolean("is_cancelled")
                )
            },
            consumerId
        )
    }

    @Transactional(rollbackFor = [InsufficientAmount::class, Throwable::class])
    override fun reserve(consumerId: UUID, idempotencyKey: UUID, items: List<ReservationItem>): Long? {
        val firstItem = items.first()

        val keyHolder = GeneratedKeyHolder()
        val inserted = jdbcTemplate.update(
            { conn ->
                val ps = conn.prepareStatement(insertReservation, arrayOf("id"))
                ps.setString(1, consumerId.toString())
                ps.setString(2, idempotencyKey.toString())
                ps.setString(3, objectMapper.writeValueAsString(listOf(firstItem)))
                ps.setInt(4, 0)
                ps
            },
            keyHolder
        )
        if (inserted == 0) {
            return jdbcTemplate.queryForObject(
                getReservationByIdempotency,
                { rs, _ -> rs.getLong("id") },
                idempotencyKey
            ) ?: throw Exception("Something goes wrong 2")
        }
        val reservationId = keyHolder.key?.toLong() ?: throw Exception("Something goes wrong")

        val rowsAffected = jdbcTemplate.update(
            updateItems,
            firstItem.quantity,
            firstItem.itemId,
            firstItem.quantity
        )
        if (rowsAffected == 0) {
            throw InsufficientAmount()
        }

        return reservationId
    }

    @Transactional(rollbackFor = [Throwable::class])
    override fun cancelReservation(reservationId: Long) {
        val (items, is_cancelled) = jdbcTemplate.queryForObject(
            getReservationById,
            { rs, _ ->
                val serializedItems = rs.getString("items")
                val items = objectMapper.readValue(serializedItems, reservationItemsTypeReference)
                Pair(items, rs.getBoolean("is_cancelled"))
            },
            reservationId
        ) ?: return

        if (is_cancelled) return

        val item = items.firstOrNull() ?: return
        val rowsAffected = jdbcTemplate.update(cancelReservation, reservationId)
        if (rowsAffected == 0) return

        jdbcTemplate.update(returnItems, item.quantity, item.itemId)
    }
}