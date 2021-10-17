package com.example.inventory.usecases

import com.example.inventory.domain.Item
import java.util.UUID
import javax.validation.constraints.Size

data class ReservationCommand(
    val consumerId: UUID,
    @Size(min = 1, max = 3)
    val items: List<Item>
)
