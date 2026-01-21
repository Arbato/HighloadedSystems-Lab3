package ru.itmo.userservice.model.dto.kafka

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

data class UserServiceResponse<T>(
    @JsonProperty("success")
    val success: Boolean,

    @JsonProperty("request_type")
    val requestType: String? = null,
    
    @JsonProperty("error_message")
    val errorMessage: String? = null,
    
    @JsonProperty("data")
    val data: T? = null,
    
    @JsonProperty("timestamp")
    val timestamp: Long = System.currentTimeMillis()
)