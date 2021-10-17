package com.example.inventory.dto

import com.example.inventory.domain.Item

data class ItemsResponse(
    val items: List<Item>,
    val prevCursor: Long? = null,
    val nextCursor: Long
)
