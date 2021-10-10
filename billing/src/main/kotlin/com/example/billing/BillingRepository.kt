package com.example.billing

import com.example.billing.domain.BillingAccount
import java.util.UUID
import org.springframework.dao.DuplicateKeyException
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class BillingRepository(
    val jdbcTemplate: NamedParameterJdbcTemplate
) {

    @Transactional(rollbackFor = [InsufficientAmount::class])
    fun credit(idempotencyKey: String, consumerId: UUID, amount: Long): PaymentOperationResult {
        try {
            jdbcTemplate.update(
                "insert into transactions (idempotency_key, created_at, amount, is_cancelled) values (:key::uuid, now(), -:amount, false)",
                mapOf("key" to idempotencyKey, "amount" to amount)
            )
        } catch (exc: DuplicateKeyException) {
            return PaymentWasMadeBefore
        }

        val rowsAffected = jdbcTemplate.update(
            "update billing_accounts set amount = amount - :amount where consumer_id = :id::uuid and amount >= :amount",
            mapOf("amount" to amount, "id" to consumerId)
        )

        if (rowsAffected > 0) {
            return PaymentMade
        }

        throw InsufficientAmount
    }

    @Transactional
    fun debit(idempotencyKey: String, consumerId: UUID, amount: Long): PaymentOperationResult {
        try {
            jdbcTemplate.update(
                "insert into transactions (idempotency_key, created_at, amount, is_cancelled) values (:key::uuid, now(), :amount, false)",
                mapOf("key" to idempotencyKey, "amount" to amount)
            )
        } catch (exc: DuplicateKeyException) {
            return PaymentWasMadeBefore
        }

        jdbcTemplate.update(
            "update billing_accounts set amount = amount + :amount where consumer_id = :id::uuid",
            mapOf("id" to consumerId, "amount" to amount, "idempotencyKey" to idempotencyKey)
        )
        return PaymentMade
    }

    fun getBillingAccountByOwnerId(consumerId: UUID): BillingAccount? {
        return try {
            jdbcTemplate.queryForObject(
                "select * from billing_accounts where consumer_id = :id::uuid",
                mapOf("id" to consumerId)
            ) {
                rs, _ ->
                BillingAccount(UUID.fromString(rs.getString("consumer_id")), rs.getLong("amount"))
            }
        } catch (exc: EmptyResultDataAccessException) {
            throw BillingNotFoundException()
        }
    }
}