package ru.itmo.market.domain.model.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table(name = "moderation_audit")
data class ModerationAudit(
    @Id
    val id: Long = 0,
    
    @Column("action_id")
    val actionId: Long,
    
    @Column("product_id")
    val productId: Long,
    
    @Column("moderator_id")
    val moderatorId: Long,
    
    @Column("old_status")
    val oldStatus: String,
    
    @Column("new_status")
    val newStatus: String,
    
    @Column("ip_address")
    val ipAddress: String? = null,
    
    @Column("created_at")
    val createdAt: LocalDateTime = LocalDateTime.now()
)
