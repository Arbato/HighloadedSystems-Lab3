package ru.itmo.orderservice.adapter.out.messaging.kafka.client

import com.fasterxml.jackson.annotation.JsonProperty

data class ProductServiceRequest(
    @JsonProperty("request_id")
    val requestId: String,

    @JsonProperty("request_type")
    val requestType: String,  // "GET_PRODUCT_BY_ID"

    @JsonProperty("product_id")
    val productId: Long? = null,

    @JsonProperty("timestamp")
    val timestamp: Long = System.currentTimeMillis()
)
