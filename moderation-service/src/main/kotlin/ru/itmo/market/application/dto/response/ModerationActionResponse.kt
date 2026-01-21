package ru.itmo.market.application.dto.response

import java.time.LocalDateTime

data class ModerationActionResponse(
    val id: Long,
    val productId: Long,
    val moderatorId: Long,
    val actionType: String,
    val reason: String?,
    val createdAt: LocalDateTime
)