package ru.itmo.fileservice.domain.model.dto.response

data class UploadResponse(
    val id: String,
    val filename: String,
    val size: Long,
    val contentType: String,
    val downloadUrl: String,
    val message: String = "File uploaded successfully"
)
