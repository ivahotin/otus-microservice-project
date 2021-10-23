package com.example.orchestrator.dto

data class Item(
    val itemId: Long,
    val title: String,
    val description: String,
    val quantity: Int,
    val price: Int
)