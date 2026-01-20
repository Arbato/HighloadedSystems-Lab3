package ru.itmo.productservice.model.dto.kafka

import com.fasterxml.jackson.annotation.JsonProperty

data class UserServiceRequest(
    @JsonProperty("request_id")
    val requestId: String,
    
    @JsonProperty("request_type")
    val requestType: String,  // "GET_USER_BY_ID"
    
    @JsonProperty("user_id")
    val userId: Long? = null,
    
    @JsonProperty("user_ids")
    val userIds: List<Long>? = null,
    
    @JsonProperty("timestamp")
    val timestamp: Long = System.currentTimeMillis()
)
