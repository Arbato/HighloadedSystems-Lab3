package ru.itmo.notificationservice.repository

import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.r2dbc.repository.R2dbcRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import ru.itmo.notificationservice.model.entity.NotificationEntity
import ru.itmo.notificationservice.model.entity.SubscriptionEntity


@Repository
interface SubscriptionRepository : R2dbcRepository<SubscriptionEntity, Long> {
    
    /**
     * Получить подписки пользователя
     */
    fun findByUserId(userId: Long): Flux<SubscriptionEntity>
    
    /**
     * Проверить подписку на тип события
     */
    @Query("""
        SELECT * FROM subscriptions 
        WHERE user_id = :userId AND event_type = :eventType
    """)
    fun findByUserIdAndEventType(
        @Param("userId") userId: Long,
        @Param("eventType") eventType: String
    ): Mono<SubscriptionEntity>
}
