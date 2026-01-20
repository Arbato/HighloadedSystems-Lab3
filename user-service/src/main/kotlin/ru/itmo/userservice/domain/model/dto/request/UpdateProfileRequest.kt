package ru.itmo.userservice.domain.model.dto.request

import jakarta.validation.constraints.*

data class UpdateProfileRequest(
    @field:Email(message = "Invalid email format")
    val email: String? = null,
    
    val firstName: String? = null,
    
    val lastName: String? = null
)
