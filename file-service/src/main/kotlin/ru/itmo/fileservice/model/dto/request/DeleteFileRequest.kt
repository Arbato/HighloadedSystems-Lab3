package ru.itmo.fileservice.model.dto.request

import jakarta.validation.constraints.*


data class DeleteFileRequest(
    @field:Positive(message = "File ID must be positive")
    val fileId: Long,

    @field:NotNull(message = "Soft delete flag is required")
    val softDelete: Boolean = true
)