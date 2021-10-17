package com.example.inventory.queries

data class ItemsQuery(
    val term: String,
    val cursor: Long? = null,
    val limit: Int = 10
)
