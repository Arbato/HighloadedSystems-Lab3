package ru.itmo.orderservice.domain.model.dto.response

import java.math.BigDecimal
import java.time.LocalDateTime

data class OrderResponse(
    val id: Long,
    val userId: Long,
    val items: List<OrderItemResponse>,
    val totalPrice: BigDecimal,
    val status: String,
    val deliveryAddress: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
