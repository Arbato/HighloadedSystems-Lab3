package ru.itmo.market.domain.model.dto.kafka

import com.fasterxml.jackson.annotation.JsonProperty

data class ProductServiceRequest(
    @JsonProperty("request_id")
    val requestId: String,

    @JsonProperty("request_type")
    val requestType: String,

    @JsonProperty("product_id")
    val productId: Long? = null,

    @JsonProperty("moderator_id")
    val moderatorId: Long? = null,

    @JsonProperty("reason")
    val reason: String? = null,

    @JsonProperty("page")
    val page: Int? = null,

    @JsonProperty("page_size")
    val pageSize: Int? = null,

    @JsonProperty("timestamp")
    val timestamp: Long = System.currentTimeMillis()
)
