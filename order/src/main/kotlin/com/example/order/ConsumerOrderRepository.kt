package com.example.order

import com.example.order.adapters.ConsumerOrderAdapter
import com.example.order.domain.ConsumerOrder
import com.example.order.domain.ConsumerOrderStatus
import com.example.order.domain.CreatedOrderEvent
import com.example.order.domain.OrderCreated
import com.example.order.domain.OrderCreationConflict
import com.example.order.domain.OrderCreationResult
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import java.util.UUID
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

private const val getOrderByQuery = "select id, consumer_id, price, version, status, delivery_id, reservation_id, payment_id from orders where id = ?::uuid and consumer_id = ?::uuid"
private const val getOrdersByConsumerId = "select id, consumer_id, price, version, status, delivery_id, reservation_id, payment_id from orders where consumer_id = ?::uuid"
private const val addConsumerOrder = "insert into orders (id, consumer_id, price, version, status, delivery_id, reservation_id, payment_id) values (?::uuid, ?::uuid, ?, ?, ?, null, null, null) on conflict (consumer_id, version) do nothing"
private const val addEvent = "insert into consumer_order_outbox_event (order_id, payload) values (?::uuid, ?::jsonb)"
private const val updateOrder = "update orders set status = ?, delivery_id = ?, reservation_id = ?, payment_id = ? where id = ?::uuid"

@Repository
class ConsumerOrderRepository(private val jdbcTemplate: JdbcTemplate): ConsumerOrderAdapter {

    private val objectMapper = ObjectMapper().also {
        it.registerModule(JavaTimeModule())
    }

    @Transactional(rollbackFor = [Exception::class])
    override fun createConsumerOrder(
        consumerOrder: ConsumerOrder,
        orderCreatedEvent: CreatedOrderEvent
    ): OrderCreationResult {
        val isInserted = jdbcTemplate.update(
            addConsumerOrder,
            consumerOrder.id,
            consumerOrder.consumerId,
            consumerOrder.price,
            consumerOrder.version + 1,
            consumerOrder.status.toString()
        ) > 0

        if (!isInserted) {
            val latestConsumerVersion = getLatestVersionForConsumerId(consumerOrder.consumerId)
            return OrderCreationConflict(latestVersion = latestConsumerVersion)
        }
        val serializedEvent = objectMapper.writeValueAsString(orderCreatedEvent)
        jdbcTemplate.update(addEvent, consumerOrder.id, serializedEvent)

        return OrderCreated(consumerOrder.id, consumerOrder.version + 1)
    }

    override fun getConsumerOrderById(orderId: UUID, consumerId: UUID): ConsumerOrder? {
        return try {
            jdbcTemplate
                .queryForObject(
                    getOrderByQuery,
                    { rs, _ ->
                        ConsumerOrder(
                            id = UUID.fromString(rs.getString("id")),
                            consumerId = UUID.fromString(rs.getString("consumer_id")),
                            price = rs.getLong("price"),
                            version = rs.getLong("version"),
                            status = ConsumerOrderStatus.valueOf(rs.getString("status")),
                            deliveryId = rs.getString("delivery_id"),
                            reservationId = rs.getLong("reservation_id"),
                            paymentId = rs.getLong("payment_id")
                        )
                    },
                    orderId,
                    consumerId
                )
        } catch (exc: EmptyResultDataAccessException) {
            null
        }
    }

    override fun getConsumerOrdersByConsumerId(consumerId: UUID): List<ConsumerOrder> {
        return jdbcTemplate.query(
            getOrdersByConsumerId,
            { rs, _ ->
                ConsumerOrder(
                    id = UUID.fromString(rs.getString("id")),
                    consumerId = UUID.fromString(rs.getString("consumer_id")),
                    price = rs.getLong("price"),
                    version = rs.getLong("version"),
                    status = ConsumerOrderStatus.valueOf(rs.getString("status")),
                    deliveryId = rs.getString("delivery_id"),
                    reservationId = rs.getLong("reservation_id"),
                    paymentId = rs.getLong("payment_id")
                )
            },
            consumerId
        )
    }

    override fun updateOrderStatus(
        orderId: UUID,
        status: ConsumerOrderStatus,
        deliveryId: String?,
        reservationId: Long?,
        paymentId: Long?
    ) {
        jdbcTemplate.update(
            updateOrder,
            status.toString(),
            deliveryId,
            reservationId,
            paymentId,
            orderId
        )
    }

    private fun getLatestVersionForConsumerId(consumerId: UUID): Long =
        try {
            jdbcTemplate.queryForObject(
                "select version as latest_version from orders where consumer_id = ?::uuid order by version desc limit 1",
                { rs, _ -> rs.getLong("latest_version") },
                consumerId
            ) ?: 0
        } catch (exc: EmptyResultDataAccessException) {
            0
        }
}
