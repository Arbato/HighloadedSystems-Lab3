package ru.itmo.notificationservice.model.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("notifications")
data class NotificationEntity(
    @Id
    @Column("id")
    val id: Long? = null,
    
    @Column("user_id")
    val userId: Long,
    
    @Column("event_type")
    val eventType: String,
    
    @Column("title")
    val title: String,
    
    @Column("message")
    val message: String,
    
    @Column("channel")
    val channel: String = "IN_APP",
    
    @Column("is_read")
    val isRead: Boolean = false,
    
    @Column("created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    @Column("read_at")
    val readAt: LocalDateTime? = null
)

@Table("subscriptions")
data class SubscriptionEntity(
    @Id
    @Column("id")
    val id: Long? = null,
    
    @Column("user_id")
    val userId: Long,
    
    @Column("event_type")
    val eventType: String,
    
    @Column("channel")
    val channel: String = "IN_APP",
    
    @Column("is_active")
    val isActive: Boolean = true,
    
    @Column("created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    @Column("updated_at")
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
