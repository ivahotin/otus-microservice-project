package com.example.inventory.usecases

data class AddItemCommand(
    val title: String,
    val description: String,
    val quantity: Int,
    val price: Int
)
