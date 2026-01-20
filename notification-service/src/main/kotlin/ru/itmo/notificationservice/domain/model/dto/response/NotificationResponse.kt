package ru.itmo.notificationservice.domain.model.dto.response

import java.time.LocalDateTime

data class NotificationResponse(
    val id: Long,
    val userId: Long,
    val type: String,
    val title: String,
    val message: String,
    val status: String,
    val channel: String,
    val referenceType: String?,
    val referenceId: Long?,
    val sentAt: LocalDateTime?,
    val createdAt: LocalDateTime
)

data class PaginatedNotificationResponse(
    val data: List<NotificationResponse>,
    val page: Int,
    val pageSize: Int,
    val totalElements: Long,
    val totalPages: Int
)
