package com.example.delivery

import java.sql.Timestamp
import java.util.UUID
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Repository

private const val updateStatement = "insert into deliveries (idempotency_key, type, city, delivery_datetime) values (?::uuid, ?, ?, ?) on conflict (idempotency_key) do nothing"
private const val getDeliveryByIdStatement = "select id, idempotency_key, type, city, delivery_datetime, is_cancelled from deliveries where id = ?"
private const val cancelStatement = "update deliveries set is_cancelled = true where id = ?"

@Repository
class DeliveryRepository(private val jdbcTemplate: JdbcTemplate) {

    fun reserveDelivery(createDeliveryCommand: CreateDeliveryCommand): Long {
        val keyHolder = GeneratedKeyHolder()
        jdbcTemplate.update(
            { conn ->
                val ps = conn.prepareStatement(updateStatement, arrayOf("id"))
                ps.setString(1, createDeliveryCommand.idempotencyKey.toString())
                ps.setString(2, createDeliveryCommand.type)
                ps.setString(3, createDeliveryCommand.city)
                ps.setTimestamp(4, Timestamp.valueOf(createDeliveryCommand.deliveryDatetime))
                ps
            },
            keyHolder
        )

        return keyHolder.key?.toLong() ?: getDeliveryIdByIdempotencyKey(createDeliveryCommand.idempotencyKey)
    }

    fun cancelDelivery(deliveryId: Long) {
        jdbcTemplate.update(cancelStatement, deliveryId)
    }

    fun getDeliveryById(deliveryId: Long): Delivery? {
        return try {
            jdbcTemplate.queryForObject(
                getDeliveryByIdStatement,
                {
                    rs, _ -> Delivery(
                        id = rs.getLong("id"),
                        idempotencyKey = UUID.fromString(rs.getString("idempotency_key")),
                        type = rs.getString("type"),
                        city = rs.getString("city"),
                        deliveryDatetime = rs.getTimestamp("delivery_datetime").toLocalDateTime(),
                        isCancelled = rs.getBoolean("is_cancelled")
                    )
                },
                deliveryId
            )
        } catch (exc: EmptyResultDataAccessException) {
            return null
        }
    }

    private fun getDeliveryIdByIdempotencyKey(idempotencyKey: UUID): Long =
        jdbcTemplate.queryForObject(
            "select id from deliveries where idempotency_key = ?::uuid",
            {
                rs, _ -> rs.getLong("id")
            },
            idempotencyKey
        ) ?: -1L
}