package ru.itmo.productservice.domain.model.dto.kafka

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

data class UserServiceResponse(
    @JsonProperty("request_id")
    val requestId: String,
    
    @JsonProperty("success")
    val success: Boolean,
    
    @JsonProperty("error_message")
    val errorMessage: String? = null,
    
    @JsonProperty("user")
    val user: UserData? = null,
    
    @JsonProperty("timestamp")
    val timestamp: Long = System.currentTimeMillis()
)

data class UserData(
    @JsonProperty("id")
    val id: Long,
    
    @JsonProperty("username")
    val username: String,
    
    @JsonProperty("email")
    val email: String,
    
    @JsonProperty("first_name")
    val firstName: String,
    
    @JsonProperty("last_name")
    val lastName: String,
    
    @JsonProperty("roles")
    val roles: Set<String>,
    
    @JsonProperty("created_at")
    val createdAt: LocalDateTime,
    
    @JsonProperty("updated_at")
    val updatedAt: LocalDateTime
)
