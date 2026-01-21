package ru.itmo.fileservice.model.dto.response

import java.time.LocalDateTime
import java.math.BigDecimal

data class FileMetadataResponse(
    val id: Long,
    val userId: Long,
    val fileKey: String,
    val fileName: String,
    val fileType: String,
    val contentType: String,
    val fileSizeBytes: Long,
    val fileSizeMB: BigDecimal,
    val status: String,
    val isPublic: Boolean,
    val downloadCount: Long,
    val checksumMd5: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val expiresAt: LocalDateTime?,
    val metadata: Map<String, String>?
)