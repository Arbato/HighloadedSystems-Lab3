package ru.itmo.fileservice.model.dto.response

import java.time.LocalDateTime
import java.math.BigDecimal

data class PaginatedFilesResponse(
    val data: List<FileMetadataResponse>,
    val page: Int,
    val pageSize: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean
)