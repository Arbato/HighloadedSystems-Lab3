package ru.itmo.market.model.dto.kafka

import com.fasterxml.jackson.annotation.JsonProperty

data class UserServiceRequest(
    @JsonProperty("request_id")
    val requestId: String,

    @JsonProperty("request_type")
    val requestType: String,

    @JsonProperty("user_id")
    val userId: Long? = null,

    @JsonProperty("timestamp")
    val timestamp: Long = System.currentTimeMillis()
)
