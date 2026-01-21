package ru.itmo.market.infrastructure.repository

import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.r2dbc.repository.R2dbcRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import ru.itmo.market.domain.entity.ModerationAction

@Repository
interface ModerationActionRepository : R2dbcRepository<ModerationAction, Long> {
    
    fun findByProductIdAndActionType(productId: Long, actionType: String): Mono<ModerationAction>
    
    @Query("""
        SELECT * FROM moderation_actions 
        WHERE product_id = :productId 
        ORDER BY created_at DESC 
        LIMIT 1
    """)
    fun findLatestByProductId(productId: Long): Mono<ModerationAction>
    
    fun findByModeratorIdOrderByCreatedAtDesc(moderatorId: Long): Flux<ModerationAction>
    
    @Query("""
        SELECT * FROM moderation_actions 
        WHERE action_type = :actionType 
        ORDER BY created_at DESC 
        LIMIT :limit OFFSET :offset
    """)
    fun findByActionTypePaginated(actionType: String, limit: Int, offset: Int): Flux<ModerationAction>
}
