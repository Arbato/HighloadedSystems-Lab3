package ru.itmo.fileservice.model.dto.response

import java.time.LocalDateTime
import java.math.BigDecimal

data class FileStatisticsResponse(
    val totalFiles: Long,
    val totalStorageBytes: Long,
    val totalStorageMB: BigDecimal,
    val averageFileSizeBytes: Long,
    val filesByType: Map<String, Long>,
    val filesByStatus: Map<String, Long>,
    val mostDownloadedFiles: List<FileMetadataResponse>,
    val lastUploadedFiles: List<FileMetadataResponse>
)