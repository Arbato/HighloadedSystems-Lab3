package ru.itmo.orderservice.domain.model.dto.request

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

data class AddToCartRequest(
    @field:NotNull(message = "Product ID is required")
    @field:Min(1, message = "Product ID must be greater than 0")
    val productId: Long,
    
    @field:Min(1, message = "Quantity must be at least 1")
    val quantity: Int = 1
)
