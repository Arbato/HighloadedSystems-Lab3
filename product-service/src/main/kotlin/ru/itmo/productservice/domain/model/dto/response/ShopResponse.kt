package ru.itmo.productservice.domain.model.dto.response

import java.time.LocalDateTime

data class ShopResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val avatarUrl: String?,
    val sellerId: Long,
    val sellerName: String? = null,
    val productsCount: Long? = null,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
