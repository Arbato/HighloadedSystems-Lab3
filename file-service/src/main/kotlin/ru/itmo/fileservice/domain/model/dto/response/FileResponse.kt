package ru.itmo.fileservice.domain.model.dto.response

import java.time.Instant

data class FileResponse(
    val id: String,
    val filename: String,
    val originalFilename: String,
    val contentType: String,
    val size: Long,
    val uploadedAt: Instant,
    val downloadUrl: String
)
