package com.example.billing

import com.example.billing.domain.BillingAccount
import java.util.UUID
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

private const val insertCreditTxStatement = "insert into transactions (idempotency_key, created_at, amount, is_cancelled) values (?::uuid, now(), -?, false) on conflict (idempotency_key) do nothing"
private const val insertDebitTxStatement = "insert into transactions (idempotency_key, created_at, amount, is_cancelled) values (?::uuid, now(), ?, false) on conflict (idempotency_key) do nothing"
private const val getTxIdByIdempotencyKey = "select id from transactions where idempotency_key = ?::uuid"

@Repository
class BillingRepository(val jdbcTemplate: JdbcTemplate) {

    @Transactional(rollbackFor = [InsufficientAmount::class])
    fun credit(idempotencyKey: String, consumerId: UUID, amount: Long): PaymentOperationResult {
        val keyHolder = GeneratedKeyHolder()
        val inserted = jdbcTemplate.update(
            { conn ->
                val ps = conn.prepareStatement(insertCreditTxStatement, arrayOf("id"))
                ps.setString(1, idempotencyKey)
                ps.setLong(2, amount)
                ps
            },
            keyHolder
        )
        if (inserted == 0) {
            val txId = jdbcTemplate.queryForObject(
                getTxIdByIdempotencyKey,
                { rs, _ -> rs.getLong("id") },
                idempotencyKey
            ) ?: throw Exception("Something goes wrong")
            return PaymentWasMadeBefore(txId)
        }
        val txId = keyHolder.key?.toLong() ?: throw Exception("Something goes wrong")

        val rowsAffected = jdbcTemplate.update(
            "update billing_accounts set amount = amount - ? where consumer_id = ?::uuid and amount >= ?",
            amount,
            consumerId,
            amount
        )

        if (rowsAffected > 0) {
            return PaymentMade(txId)
        }

        throw InsufficientAmount
    }

    @Transactional
    fun debit(idempotencyKey: String, consumerId: UUID, amount: Long): PaymentOperationResult {
        val keyHolder = GeneratedKeyHolder()
        val inserted = jdbcTemplate.update(
            { conn ->
                val ps = conn.prepareStatement(insertDebitTxStatement, arrayOf("id"))
                ps.setString(1, idempotencyKey)
                ps.setLong(2, amount)
                ps
            },
            keyHolder
        )
        if (inserted == 0) {
            val txId = jdbcTemplate.queryForObject(
                getTxIdByIdempotencyKey,
                { rs, _ -> rs.getLong("id") },
                idempotencyKey
            ) ?: throw Exception("Something goes wrong")
            return PaymentWasMadeBefore(txId)
        }
        val txId = keyHolder.key?.toLong() ?: throw Exception("Something goes wrong")

        jdbcTemplate.update(
            "update billing_accounts set amount = amount + ? where consumer_id = ?::uuid",
            amount,
            consumerId
        )
        return PaymentMade(txId)
    }

    fun getBillingAccountByOwnerId(consumerId: UUID): BillingAccount? {
        return try {
            jdbcTemplate.queryForObject(
                "select * from billing_accounts where consumer_id = ?::uuid",
                { rs, _ ->
                    BillingAccount(UUID.fromString(rs.getString("consumer_id")), rs.getLong("amount"))
                },
                consumerId
            )
        } catch (exc: EmptyResultDataAccessException) {
            throw BillingNotFoundException()
        }
    }
}