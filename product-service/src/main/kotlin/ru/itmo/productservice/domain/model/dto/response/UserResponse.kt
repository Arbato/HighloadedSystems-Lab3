package ru.itmo.productservice.domain.model.dto.response

import java.time.LocalDateTime

data class UserResponse(
    val id: Long,
    val username: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val roles: Set<String>,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
