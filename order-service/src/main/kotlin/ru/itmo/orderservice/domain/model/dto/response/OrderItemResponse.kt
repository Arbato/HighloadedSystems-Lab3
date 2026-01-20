package ru.itmo.orderservice.domain.model.dto.response

import java.math.BigDecimal
import java.time.LocalDateTime

data class OrderItemResponse(
    val id: Long,
    val productId: Long,
    val productName: String,
    val productPrice: BigDecimal,
    val quantity: Int,
    val price: BigDecimal,  // Price per unit
    val subtotal: BigDecimal,  // Total for this item
    val createdAt: LocalDateTime
)
