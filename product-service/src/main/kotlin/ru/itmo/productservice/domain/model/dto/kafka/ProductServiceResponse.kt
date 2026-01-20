package ru.itmo.productservice.domain.model.dto.kafka

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

    @JsonProperty("products")
    val products: List<ProductData>? = null,

    @JsonProperty("total_elements")
    val totalElements: Long? = null,

    @JsonProperty("total_pages")
    val totalPages: Int? = null,

    @JsonProperty("page")
    val page: Int? = null,

    @JsonProperty("page_size")
    val pageSize: Int? = null,

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

    @JsonProperty("shop_id")
    val shopId: Long,

    @JsonProperty("status")
    val status: String
)
