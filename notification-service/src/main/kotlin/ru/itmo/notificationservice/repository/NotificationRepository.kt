package ru.itmo.notificationservice.repository

import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.r2dbc.repository.R2dbcRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import ru.itmo.notificationservice.model.entity.NotificationEntity

@Repository
interface NotificationRepository : R2dbcRepository<NotificationEntity, Long> {
    
    /**
     * Получить все уведомления пользователя
     */
    fun findByUserId(userId: Long): Flux<NotificationEntity>
    
    /**
     * Получить только непрочитанные уведомления
     */
    @Query("""
        SELECT * FROM notifications 
        WHERE user_id = :userId AND is_read = false
        ORDER BY created_at DESC
    """)
    fun findUnreadByUserId(@Param("userId") userId: Long): Flux<NotificationEntity>
    
    /**
     * Отметить все уведомления как прочитанные
     */
    @Query("""
        UPDATE notifications 
        SET is_read = true, read_at = NOW()
        WHERE user_id = :userId AND is_read = false
    """)
    fun markAllAsRead(@Param("userId") userId: Long): Mono<Void>
    
    /**
     * Удалить старые уведомления (старше 30 дней)
     */
    @Query("""
        DELETE FROM notifications 
        WHERE created_at < NOW() - INTERVAL '30 days'
    """)
    fun deleteOldNotifications(): Mono<Void>
}