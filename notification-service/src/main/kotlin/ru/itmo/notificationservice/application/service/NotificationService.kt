package ru.itmo.notificationservice.application.service

import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import ru.itmo.notificationservice.domain.model.dto.response.NotificationResponse
import ru.itmo.notificationservice.domain.model.dto.response.PaginatedNotificationResponse
import ru.itmo.notificationservice.domain.model.entity.Notification
import ru.itmo.notificationservice.domain.model.entity.NotificationChannel
import ru.itmo.notificationservice.domain.model.entity.NotificationStatus
import ru.itmo.notificationservice.domain.model.entity.NotificationType
import ru.itmo.notificationservice.domain.repository.NotificationRepository
import java.time.LocalDateTime

@Service
class NotificationService(
    private val notificationRepository: NotificationRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun createNotification(
        userId: Long,
        type: NotificationType,
        title: String,
        message: String,
        channel: NotificationChannel = NotificationChannel.IN_APP,
        referenceType: String? = null,
        referenceId: Long? = null
    ): Mono<Notification> {
        val notification = Notification(
            userId = userId,
            type = type,
            title = title,
            message = message,
            channel = channel,
            referenceType = referenceType,
            referenceId = referenceId,
            status = NotificationStatus.SENT,
            sentAt = LocalDateTime.now()
        )

        return notificationRepository.save(notification)
            .doOnSuccess { saved ->
                logger.info("Created notification: id=${saved.id}, userId=$userId, type=$type")
            }
    }

    fun getUserNotifications(userId: Long, page: Int, pageSize: Int): Mono<PaginatedNotificationResponse> {
        val pageable = PageRequest.of(page - 1, pageSize)

        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
            .map { it.toResponse() }
            .collectList()
            .zipWith(notificationRepository.countByUserId(userId))
            .map { tuple ->
                val notifications = tuple.t1
                val totalElements = tuple.t2
                val totalPages = ((totalElements + pageSize - 1) / pageSize).toInt()

                PaginatedNotificationResponse(
                    data = notifications,
                    page = page,
                    pageSize = pageSize,
                    totalElements = totalElements,
                    totalPages = totalPages
                )
            }
    }

    fun getUnreadNotifications(userId: Long): Flux<NotificationResponse> {
        return notificationRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, NotificationStatus.SENT)
            .map { it.toResponse() }
    }

    fun markAsRead(notificationId: Long, userId: Long): Mono<NotificationResponse> {
        return notificationRepository.findById(notificationId)
            .filter { it.userId == userId }
            .flatMap { notification ->
                notificationRepository.save(notification.copy(status = NotificationStatus.READ))
            }
            .map { it.toResponse() }
    }

    fun getUnreadCount(userId: Long): Mono<Long> {
        return notificationRepository.countByUserIdAndStatus(userId, NotificationStatus.SENT)
    }

    private fun Notification.toResponse() = NotificationResponse(
        id = id,
        userId = userId,
        type = type.name,
        title = title,
        message = message,
        status = status.name,
        channel = channel.name,
        referenceType = referenceType,
        referenceId = referenceId,
        sentAt = sentAt,
        createdAt = createdAt
    )
}
