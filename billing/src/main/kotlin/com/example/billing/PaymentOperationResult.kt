package com.example.billing

sealed interface PaymentOperationResult
data class PaymentWasMadeBefore(val transactionId: Long): PaymentOperationResult
object InsufficientAmount: PaymentOperationResult, Throwable()
data class PaymentMade(val transactionId: Long): PaymentOperationResult
