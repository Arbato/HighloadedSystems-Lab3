package ru.itmo.productservice.model.dto.kafka

import com.fasterxml.jackson.annotation.JsonProperty

data class ProductServiceRequest<T>(
    @JsonProperty("request_type")
    val requestType: String,
    
    @JsonProperty("payload")
    val payload: T,
    
    @JsonProperty("timestamp")
    val timestamp: Long = System.currentTimeMillis()
)

data class GetProductPayload(
    @JsonProperty("product_id")
    val productId: Long
)

data class GetProductsPayload(
    @JsonProperty("page")
    val page: Int = 1,
    
    @JsonProperty("page_size")
    val pageSize: Int = 20
)

data class ApproveProductPayload(
    @JsonProperty("product_id")
    val productId: Long,
    
    @JsonProperty("moderator_id")
    val moderatorId: Long
)

data class RejectProductPayload(
    @JsonProperty("product_id")
    val productId: Long,
    
    @JsonProperty("moderator_id")
    val moderatorId: Long,
    
    @JsonProperty("reason")
    val reason: String
)

