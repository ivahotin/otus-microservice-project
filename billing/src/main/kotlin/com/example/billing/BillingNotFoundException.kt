package com.example.billing

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "Billing account not found")
class BillingNotFoundException : RuntimeException()