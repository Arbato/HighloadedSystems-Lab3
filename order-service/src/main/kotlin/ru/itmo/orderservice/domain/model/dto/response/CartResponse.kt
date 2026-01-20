package ru.itmo.orderservice.domain.model.dto.response

import java.math.BigDecimal

data class CartResponse(
    val orderId: Long,
    val items: List<OrderItemResponse>,
    val totalPrice: BigDecimal,
    val itemsCount: Int
)
