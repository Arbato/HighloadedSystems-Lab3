package ru.itmo.userservice.domain.model.dto.request

import jakarta.validation.constraints.*

data class RegisterRequest(
    @field:NotBlank(message = "Username is required")
    @field:Size(min = 4, max = 32, message = "Username must be between 4 and 32 characters")
    val username: String,
    
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Invalid email format")
    val email: String,
    
    @field:NotBlank(message = "Password is required")
    @field:Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
    val password: String,
    
    @field:NotBlank(message = "First name is required")
    val firstName: String,
    
    @field:NotBlank(message = "Last name is required")
    val lastName: String
)
