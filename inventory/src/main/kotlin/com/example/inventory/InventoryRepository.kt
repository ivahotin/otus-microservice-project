package com.example.inventory

import com.example.inventory.adapters.InventoryStorageAdapter
import com.example.inventory.domain.Item
import com.example.inventory.domain.Reservation
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import java.util.UUID
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

private const val findItemsQuery = "select id, title, description, quantity, price from items where title like ? and id > ? order by id asc limit ?"
private const val addItemStmt = "insert into items (title, description, quantity, price) values (?, ?, ?, ?) returning id"
private const val getMyReservationsQuery = "select id, consumer_id, idempotency_key, items, subtotal from reservations where consumer_id = ?::uuid"
private const val updateItems = "update items set quantity = quantity - ? where id = ? and quantity >= ?"
private const val insertReservation = "insert into reservations (consumer_id, idempotency_key, items, subtotal) values (?::uuid, ?::uuid, ?::jsonb, ?)"

@Repository
class InventoryRepository(private val jdbcTemplate: JdbcTemplate): InventoryStorageAdapter {

    private val objectMapper = ObjectMapper().also {
        it.registerModule(JavaTimeModule())
    }
    private val itemsTypeReference = object : TypeReference<List<Item>>() {}

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
            offsetId,
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
                val items = objectMapper.readValue(serializedItems, itemsTypeReference)
                Reservation(
                    id = rs.getLong("id"),
                    consumerId = UUID.fromString(rs.getString("consumer_id")),
                    idempotencyKey = UUID.fromString(rs.getString("idempotency_key")),
                    subtotal = rs.getInt("subtotal"),
                    items = items
                )
            },
            consumerId
        )
    }

    @Transactional(rollbackFor = [Throwable::class])
    override fun reserve(consumerId: UUID, idempotencyKey: UUID, items: List<Item>, subtotal: Int): Long? {
        val firstItem = items.first()

        val rowsAffected = jdbcTemplate.update(
            updateItems,
            firstItem.quantity,
            firstItem.itemId,
            firstItem.quantity
        )

        if (rowsAffected == 0) {
            return null
        }

        val keyHolder = GeneratedKeyHolder()
        jdbcTemplate.update(
            { conn ->
                val ps = conn.prepareStatement(insertReservation, arrayOf("id"))
                ps.setString(1, consumerId.toString())
                ps.setString(2, idempotencyKey.toString())
                ps.setString(3, objectMapper.writeValueAsString(firstItem))
                ps.setInt(4, subtotal)
                ps
            },
            keyHolder
        )

        return keyHolder.key?.toLong() ?: throw Exception("Something goes wrong")
    }
}