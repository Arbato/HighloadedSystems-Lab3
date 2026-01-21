package ru.itmo.notificationservice.service.application

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import ru.itmo.notificationservice.exception.NotificationException
import ru.itmo.notificationservice.model.dto.request.SubscriptionRequest
import ru.itmo.notificationservice.model.dto.response.NotificationListResponse
import ru.itmo.notificationservice.model.dto.response.NotificationResponse
import ru.itmo.notificationservice.model.dto.response.SubscriptionResponse
import ru.itmo.notificationservice.model.entity.NotificationEntity
import ru.itmo.notificationservice.model.entity.SubscriptionEntity
import ru.itmo.notificationservice.repository.NotificationRepository
import ru.itmo.notificationservice.repository.SubscriptionRepository
import java.time.LocalDateTime

@Service
class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val subscriptionRepository: SubscriptionRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun getUserNotifications(userId: Long, page: Int, pageSize: Int): Mono<NotificationListResponse> {
        return notificationRepository.findByUserId(userId)
            .collectList()
            .flatMap { notifications ->
                val total = notifications.size.toLong()
                val paginated = notifications
                    .sortedByDescending { it.createdAt }
                    .drop(page * pageSize)
                    .take(pageSize)

                notificationRepository.findUnreadByUserId(userId)
                    .collectList()
                    .map { unread ->
                        NotificationListResponse(
                            notifications = paginated.map { toResponse(it) },
                            total = total,
                            unreadCount = unread.size.toLong()
                        )
                    }
            }
            .doOnError { logger.error("Error getting user notifications", it) }
    }

    fun getUnreadNotifications(userId: Long): Mono<List<NotificationResponse>> {
        return notificationRepository.findUnreadByUserId(userId)
            .map { toResponse(it) }
            .collectList()
            .doOnError { logger.error("Error getting unread notifications", it) }
    }

    fun markAsRead(notificationId: Long, userId: Long): Mono<NotificationResponse> {
        return notificationRepository.findById(notificationId)
            .switchIfEmpty(Mono.error(NotificationException("Notification not found")))
            .flatMap { notification ->
                if (notification.userId != userId) {
                    Mono.error(NotificationException("Unauthorized"))
                } else {
                    val updated = notification.copy(
                        isRead = true,
                        readAt = LocalDateTime.now()
                    )
                    notificationRepository.save(updated)
                }
            }
            .map { toResponse(it) }
            .doOnError { logger.error("Error marking notification as read", it) }
    }

    fun markAllAsRead(userId: Long): Mono<Void> {
        return notificationRepository.markAllAsRead(userId)
            .doOnError { logger.error("Error marking all as read", it) }
    }

    fun deleteNotification(notificationId: Long, userId: Long): Mono<Void> {
        return notificationRepository.findById(notificationId)
            .switchIfEmpty(Mono.error(NotificationException("Notification not found")))
            .flatMap { notification ->
                if (notification.userId != userId) {
                    Mono.error(NotificationException("Unauthorized"))
                } else {
                    notificationRepository.deleteById(notificationId)
                }
            }
            .doOnError { logger.error("Error deleting notification", it) }
    }

    fun createSubscription(userId: Long, request: SubscriptionRequest): Mono<SubscriptionResponse> {
        return subscriptionRepository.findByUserIdAndEventType(userId, request.eventType)
            .flatMap { existing : SubscriptionEntity ->
                if (existing.isActive) {
                    Mono.just(existing)
                } else {
                    val updated = existing.copy(isActive = true, updatedAt = LocalDateTime.now())
                    subscriptionRepository.save(updated)
                }
            }
            .switchIfEmpty(
                Mono.fromCallable {
                    SubscriptionEntity(
                        userId = userId,
                        eventType = request.eventType,
                        channel = request.channel,
                        isActive = true
                    )
                }
                .flatMap { subscriptionRepository.save(it) }
            )
            .map { toSubscriptionResponse(it) }
            .doOnError { logger.error("Error creating subscription", it) }
    }

    fun getUserSubscriptions(userId: Long): Mono<List<SubscriptionResponse>> {
        return subscriptionRepository.findByUserId(userId)
            .map { toSubscriptionResponse(it) }
            .collectList()
            .doOnError { logger.error("Error getting subscriptions", it) }
    }

    fun deleteSubscription(subscriptionId: Long, userId: Long): Mono<Void> {
        return subscriptionRepository.findById(subscriptionId)
            .switchIfEmpty(Mono.error(NotificationException("Subscription not found")))
            .flatMap { subscription : SubscriptionEntity ->
                if (subscription.userId != userId) {
                    Mono.error(NotificationException("Unauthorized"))
                } else {
                    subscriptionRepository.deleteById(subscriptionId)
                }
            }
            .doOnError { logger.error("Error deleting subscription", it) }
    }

    private fun toResponse(entity: NotificationEntity): NotificationResponse {
        return NotificationResponse(
            id = entity.id!!,
            userId = entity.userId,
            eventType = entity.eventType,
            title = entity.title,
            message = entity.message,
            channel = entity.channel,
            isRead = entity.isRead,
            createdAt = entity.createdAt,
            readAt = entity.readAt
        )
    }

    private fun toSubscriptionResponse(entity: SubscriptionEntity): SubscriptionResponse {
        return SubscriptionResponse(
            id = entity.id!!,
            userId = entity.userId,
            eventType = entity.eventType,
            channel = entity.channel,
            isActive = entity.isActive
        )
    }
}
