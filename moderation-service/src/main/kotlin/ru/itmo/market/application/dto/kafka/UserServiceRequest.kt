package ru.itmo.market.application.dto.kafka

import com.fasterxml.jackson.annotation.JsonProperty

data class UserServiceRequest<T>(
    @JsonProperty("request_type")
    val requestType: String, 
    
    @JsonProperty("payload")
    val payload: T,
    
    @JsonProperty("timestamp")
    val timestamp: Long = System.currentTimeMillis()
)

data class GetUserPayload(
    @JsonProperty("user_id")
    val userId: Long
)
