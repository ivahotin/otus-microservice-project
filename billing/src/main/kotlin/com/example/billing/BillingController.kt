package com.example.billing

import com.example.billing.domain.BillingAccount
import java.util.UUID
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@RestController
class BillingController(private val billingRepository: BillingRepository) {

    @PostMapping("/payments/credits")
    fun credit(
        @RequestHeader("x-user-id") userId: UUID,
        @RequestHeader("idempotency-key") idempotencyKey: String,
        @RequestBody payment: PaymentRequest
    ): ResponseEntity<*> {
        val operationResult = try {
            billingRepository.credit(idempotencyKey, userId, payment.amount)
        } catch (exc: Throwable) {
            return when (exc) {
                is InsufficientAmount -> ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).build<Any?>()
                else -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build<Any?>()
            }
        }

        return when (operationResult) {
            is PaymentMade -> ResponseEntity.ok(TransactionResponse(operationResult.transactionId))
            is PaymentWasMadeBefore -> ResponseEntity.ok(TransactionResponse(operationResult.transactionId))
            is InsufficientAmount -> ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).build<Any?>()
        }
    }

    @PostMapping("/payments/debits")
    fun debit(
        @RequestHeader("x-user-id") userId: UUID,
        @RequestHeader("idempotency-key") idempotencyKey: String,
        @RequestBody payment: PaymentRequest
    ): ResponseEntity<*> {
        return when (val res = billingRepository.debit(idempotencyKey, userId, payment.amount)) {
            is PaymentMade -> ResponseEntity.ok(TransactionResponse(res.transactionId))
            is PaymentWasMadeBefore -> ResponseEntity.ok(TransactionResponse(res.transactionId))
            else -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build<Any?>()
        }
    }

    @GetMapping("/payments")
    fun getConsumerPayments(@RequestHeader("x-user-id") userId: UUID): BillingAccount? =
        billingRepository.getBillingAccountByOwnerId(consumerId = userId)
}