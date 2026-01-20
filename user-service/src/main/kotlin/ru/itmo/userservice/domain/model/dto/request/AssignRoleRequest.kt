package ru.itmo.userservice.domain.model.dto.request

import jakarta.validation.constraints.NotBlank

data class AssignRoleRequest(
    val userId: Long,

    @field:NotBlank(message = "Role is required")
    val role: String
)
