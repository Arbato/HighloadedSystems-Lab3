package ru.itmo.market.infrastructure.repository

import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.r2dbc.repository.R2dbcRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import ru.itmo.market.domain.entity.ModerationAudit

@Repository
interface ModerationAuditRepository : R2dbcRepository<ModerationAudit, Long> {
    
    fun findByProductIdOrderByCreatedAtDesc(productId: Long): Flux<ModerationAudit>
    
    fun findByModeratorIdOrderByCreatedAtDesc(moderatorId: Long): Flux<ModerationAudit>
    
    @Query("""
        SELECT * FROM moderation_audit 
        WHERE product_id = :productId 
        AND new_status = :status 
        ORDER BY created_at DESC 
        LIMIT 1
    """)
    fun findLatestStatusChange(productId: Long, status: String): Flux<ModerationAudit>
}
