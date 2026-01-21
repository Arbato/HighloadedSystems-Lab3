package ru.itmo.fileservice.model.dto.response

import java.time.LocalDateTime
import java.math.BigDecimal

data class BulkDeleteResponse(
    val deletedCount: Int,
    val failedCount: Int,
    val failedFileIds: List<Long>,
    val message: String
)