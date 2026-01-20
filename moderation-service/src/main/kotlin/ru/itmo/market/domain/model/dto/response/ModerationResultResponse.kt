package ru.itmo.market.domain.model.dto.response

import java.time.LocalDateTime

data class ModerationResultResponse(
    val productId: Long,
    val productName: String,
    val action: String, // APPROVE, REJECT
    val reason: String?,
    val moderatorId: Long,
    val newStatus: String,
    val timestamp: LocalDateTime
)