package ru.itmo.userservice.application.dto.response

import java.time.LocalDateTime

data class ErrorResponse(
    val message: String,
    val status: Int,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val path: String? = null,
    val errors: List<String>? = null
)
