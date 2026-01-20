package ru.itmo.fileservice.domain.model.dto.response

data class FileListResponse(
    val files: List<FileResponse>,
    val total: Int
)
