package ru.itmo.orderservice.model.dto.kafka

import com.fasterxml.jackson.annotation.JsonProperty

data class ProductServiceRequest(
    
    @JsonProperty("request_type")
    val requestType: String,  
    
    @JsonProperty("product_id")
    val productId: Long? = null,
    
    @JsonProperty("timestamp")
    val timestamp: Long = System.currentTimeMillis()
)

