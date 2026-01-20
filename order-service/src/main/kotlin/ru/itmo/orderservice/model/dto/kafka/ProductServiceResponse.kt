package ru.itmo.orderservice.model.dto.kafka

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal

data class ProductServiceResponse(
    @JsonProperty("request_id")
    val requestId: String,

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
    val imageUrl: String? = null,

    @JsonProperty("shop_id")
    val shopId: Long,

    @JsonProperty("seller_id")
    val sellerId: Long = 0,

    @JsonProperty("status")
    val status: String,

    @JsonProperty("rejection_reason")
    val rejectionReason: String? = null,

    @JsonProperty("created_at")
    val createdAt: String? = null,

    @JsonProperty("updated_at")
    val updatedAt: String? = null
)
