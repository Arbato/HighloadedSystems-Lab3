package ru.itmo.orderservice.application.dto.response

import java.math.BigDecimal

data class CartResponse(
    val orderId: Long,
    val items: List<OrderItemResponse>,
    val totalPrice: BigDecimal,
    val itemsCount: Int
)
