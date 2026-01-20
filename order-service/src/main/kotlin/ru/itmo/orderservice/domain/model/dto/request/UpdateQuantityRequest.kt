package ru.itmo.orderservice.domain.model.dto.request

import jakarta.validation.constraints.Min

data class UpdateQuantityRequest(
    @field:Min(0, message = "Quantity must be at least 0 (0 to remove item)")
    val quantity: Int
)
