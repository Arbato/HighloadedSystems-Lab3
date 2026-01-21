package ru.itmo.market.domain.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table(name = "moderation_actions")
data class ModerationAction(
    @Id
    val id: Long = 0,
    
    @Column("product_id")
    val productId: Long,
    
    @Column("moderator_id")
    val moderatorId: Long,
    
    @Column("action_type")
    val actionType: String, // APPROVE, REJECT, REVIEW
    
    @Column("reason")
    val reason: String? = null,
    
    @Column("created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    @Column("updated_at")
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
