package ru.itmo.fileservice.model.dto.response

import java.time.LocalDateTime
import java.math.BigDecimal

data class FileUploadResponse(
    val fileId: Long,
    val fileKey: String,
    val fileName: String,
    val fileType: String,
    val fileSizeBytes: Long,
    val contentType: String,
    val status: String,
    val downloadUrl: String,
    val expiresAt: LocalDateTime?,
    val uploadedAt: LocalDateTime,
    val message: String = "File uploaded successfully"
)