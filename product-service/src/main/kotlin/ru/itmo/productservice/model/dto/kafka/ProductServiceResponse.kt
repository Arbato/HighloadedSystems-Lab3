package ru.itmo.productservice.model.dto.kafka

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.LocalDateTime

data class ProductServiceResponse(
    
    @JsonProperty("success")
    val success: Boolean,
    
    @JsonProperty("error_message")
    val errorMessage: String? = null,
    
    @JsonProperty("product")
    val product: ProductData? = null,
    
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
    
    @JsonProperty("image_url")
    val imageUrl: String?,
    
    @JsonProperty("shop_id")
    val shopId: Long,
    
    @JsonProperty("seller_id")
    val sellerId: Long,
    
    @JsonProperty("status")
    val status: String,
    
    @JsonProperty("rejection_reason")
    val rejectionReason: String?,
    
    @JsonProperty("average_rating")
    val averageRating: Double?,
    
    @JsonProperty("comments_count")
    val commentsCount: Long?,
    
    @JsonProperty("created_at")
    val createdAt: LocalDateTime,
    
    @JsonProperty("updated_at")
    val updatedAt: LocalDateTime
)
