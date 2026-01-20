package ru.itmo.notificationservice.domain.model.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("notifications")
data class Notification(
    @Id
    val id: Long = 0,

    @Column("user_id")
    val userId: Long,

    @Column("type")
    val type: NotificationType,

    @Column("title")
    val title: String,

    @Column("message")
    val message: String,

    @Column("status")
    val status: NotificationStatus = NotificationStatus.PENDING,

    @Column("channel")
    val channel: NotificationChannel = NotificationChannel.IN_APP,

    @Column("reference_type")
    val referenceType: String? = null,

    @Column("reference_id")
    val referenceId: Long? = null,

    @Column("sent_at")
    val sentAt: LocalDateTime? = null,

    @Column("created_at")
    val createdAt: LocalDateTime = LocalDateTime.now()
)

enum class NotificationType {
    ORDER_CREATED,
    ORDER_SHIPPED,
    ORDER_DELIVERED,
    ORDER_CANCELLED,
    PRODUCT_APPROVED,
    PRODUCT_REJECTED,
    SYSTEM
}

enum class NotificationStatus {
    PENDING,
    SENT,
    FAILED,
    READ
}

enum class NotificationChannel {
    IN_APP,
    EMAIL,
    PUSH,
    SMS
}
