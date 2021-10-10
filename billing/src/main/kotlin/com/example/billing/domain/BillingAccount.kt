package com.example.billing.domain

import java.util.UUID

data class BillingAccount(
    val consumerId: UUID,
    val amount: Long
)
