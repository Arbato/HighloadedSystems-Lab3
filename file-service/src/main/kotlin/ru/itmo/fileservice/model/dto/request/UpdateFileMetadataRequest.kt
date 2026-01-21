package ru.itmo.fileservice.model.dto.request

import jakarta.validation.constraints.*


data class UpdateFileMetadataRequest(
    @field:Positive(message = "File ID must be positive")
    val fileId: Long,

    @field:Size(max = 255)
    val fileName: String? = null,

    val isPublic: Boolean? = null,

    val metadata: Map<String, String>? = null
)
