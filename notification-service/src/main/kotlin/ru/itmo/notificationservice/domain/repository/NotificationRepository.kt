package ru.itmo.notificationservice.domain.repository

import org.springframework.data.domain.Pageable
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import ru.itmo.notificationservice.domain.model.entity.Notification
import ru.itmo.notificationservice.domain.model.entity.NotificationStatus

@Repository
interface NotificationRepository : ReactiveCrudRepository<Notification, Long> {

    fun findByUserIdOrderByCreatedAtDesc(userId: Long, pageable: Pageable): Flux<Notification>

    fun findByUserIdAndStatusOrderByCreatedAtDesc(userId: Long, status: NotificationStatus): Flux<Notification>

    fun countByUserId(userId: Long): Mono<Long>

    fun countByUserIdAndStatus(userId: Long, status: NotificationStatus): Mono<Long>
}
