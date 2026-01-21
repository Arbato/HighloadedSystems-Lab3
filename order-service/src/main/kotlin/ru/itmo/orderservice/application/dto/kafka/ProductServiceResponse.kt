package ru.itmo.orderservice.application.dto.kafka

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.LocalDateTime

data class ProductServiceResponse<T>(
    @JsonProperty("success")
    val success: Boolean,
    @JsonProperty("request_type")
    val requestType: String? = null,
    @JsonProperty("errorMessage")
    val errorMessage: String? = null,
    @JsonProperty("data")
    val data: T? = null,
    @JsonProperty("timestamp")
    val timestamp: Long = System.currentTimeMillis()
)

data class ProductData(
    @JsonProperty("id")
    val id: Long,
    @JsonProperty("name")
    val name: String,
    @JsonProperty("description")
    val description: String?,
    @JsonProperty("price")
    val price: BigDecimal,
    @JsonProperty("imageUrl")
    val imageUrl: String?,
    @JsonProperty("shopId")
    val shopId: Long,
    @JsonProperty("sellerId")
    val sellerId: Long,
    @JsonProperty("status")
    val status: String,
    @JsonProperty("rejectionReason")
    val rejectionReason: String?,
    @JsonProperty("averageRating")
    val averageRating: Double? = null,
    @JsonProperty("commentsCount")
    val commentsCount: Long? = null,
    @JsonProperty("createdAt")
    val createdAt: LocalDateTime,
    @JsonProperty("updatedAt")
    val updatedAt: LocalDateTime
)

// ========== PAGINATED RESPONSE (GENERIC) ==========

data class PaginatedResponse<T>(
    @JsonProperty("data")
    val data: List<T>,
    @JsonProperty("page")
    val page: Int,
    @JsonProperty("pageSize")
    val pageSize: Int,
    @JsonProperty("totalElements")
    val totalElements: Long,
    @JsonProperty("totalPages")
    val totalPages: Int
)