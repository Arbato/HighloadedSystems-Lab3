package ru.itmo.productservice.domain.model.dto.response

import java.math.BigDecimal
import java.time.LocalDateTime

data class ProductResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val price: BigDecimal,
    val imageUrl: String?,
    val shopId: Long,
    val sellerId: Long,
    val status: String,
    val rejectionReason: String?,
    val averageRating: Double? = null,
    val commentsCount: Long? = null,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
