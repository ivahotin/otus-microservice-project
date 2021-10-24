package com.example.orchestrator.clients

import com.example.orchestrator.SagaException
import io.temporal.workflow.Saga
import java.io.Serializable
import java.util.UUID
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate

@Component
class BillingClient(
    @Value("\${billing.url}") private val billingUrl: String,
    @Value("\${billing.port}") private val billingPort: String,
    private val restTemplate: RestTemplate
) {

    data class Payment(val amount: Long): Serializable
    data class TransactionResponse(val transactionId: Long): Serializable

    fun makePayment(consumerId: UUID, orderId: UUID, totalAmount: Long): Long {
        val url = "http://$billingUrl:$billingPort/payments/credits"
        val httpHeaders = HttpHeaders()
        httpHeaders.add("x-user-id", consumerId.toString())
        httpHeaders.add("idempotency-key", orderId.toString())
        val request = HttpEntity<Payment>(Payment(totalAmount), httpHeaders)
        try {
            return restTemplate.postForEntity(url, request, TransactionResponse::class.java)
                .body
                ?.transactionId
                ?: throw Exception("Something goes wrong")
        } catch (exc: HttpClientErrorException) {
            when (exc.statusCode) {
                HttpStatus.PRECONDITION_FAILED -> throw SagaException("Not enough money")
                HttpStatus.BAD_REQUEST -> throw SagaException("Bad payment request")
                else -> throw exc
            }
        }
    }
}