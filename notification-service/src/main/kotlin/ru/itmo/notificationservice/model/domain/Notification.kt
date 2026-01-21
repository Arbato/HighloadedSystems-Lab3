package ru.itmo.notificationservice.model.domain

import java.time.LocalDateTime

data class Notification(
    val id: Long? = null,
    val userId: Long,
    val eventType: String,
    val title: String,
    val message: String,
    val isRead: Boolean = false,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val readAt: LocalDateTime? = null,
    val channel: NotificationChannel = NotificationChannel.IN_APP
)

enum class NotificationChannel {
    IN_APP,
    EMAIL,
    SMS,
    PUSH
}
