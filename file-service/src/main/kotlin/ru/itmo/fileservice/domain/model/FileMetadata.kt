package ru.itmo.fileservice.domain.model

import java.time.Instant

data class FileMetadata(
    val id: String,
    val filename: String,
    val originalFilename: String,
    val contentType: String,
    val size: Long,
    val bucket: String,
    val uploadedAt: Instant = Instant.now()
)
