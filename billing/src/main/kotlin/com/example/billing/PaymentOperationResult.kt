package com.example.billing

sealed interface PaymentOperationResult
object PaymentWasMadeBefore: PaymentOperationResult
object InsufficientAmount: PaymentOperationResult, Throwable()
object PaymentMade: PaymentOperationResult
