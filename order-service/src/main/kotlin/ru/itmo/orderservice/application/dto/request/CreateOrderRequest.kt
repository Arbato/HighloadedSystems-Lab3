package ru.itmo.orderservice.application.dto.request

import jakarta.validation.constraints.NotBlank

data class CreateOrderRequest(
    @field:NotBlank(message = "Delivery address is required")
    val deliveryAddress: String
)
