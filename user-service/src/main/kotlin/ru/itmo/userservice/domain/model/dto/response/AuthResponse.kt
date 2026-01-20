package ru.itmo.userservice.domain.model.dto.response

data class AuthResponse(
    val token: String,
    val userId: Long,
    val username: String,
    val roles: Set<String>
)
