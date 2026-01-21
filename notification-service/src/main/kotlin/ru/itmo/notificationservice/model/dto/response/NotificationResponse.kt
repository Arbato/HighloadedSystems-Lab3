package ru.itmo.notificationservice.model.dto.response

import java.time.LocalDateTime

data class NotificationResponse(
    val id: Long,
    val userId: Long,
    val eventType: String,
    val title: String,
    val message: String,
    val channel: String,
    val isRead: Boolean,
    val createdAt: LocalDateTime,
    val readAt: LocalDateTime? = null
)

data class NotificationListResponse(
    val notifications: List<NotificationResponse>,
    val total: Long,
    val unreadCount: Long
)

data class SubscriptionResponse(
    val id: Long,
    val userId: Long,
    val eventType: String,
    val channel: String,
    val isActive: Boolean
)
