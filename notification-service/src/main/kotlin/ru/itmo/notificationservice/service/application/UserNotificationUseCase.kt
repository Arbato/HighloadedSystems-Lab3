package ru.itmo.notificationservice.service.application

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import ru.itmo.notificationservice.model.domain.Notification
import ru.itmo.notificationservice.model.domain.NotificationChannel
import ru.itmo.notificationservice.model.events.*
import ru.itmo.notificationservice.repository.NotificationRepository
import ru.itmo.notificationservice.model.entity.NotificationEntity
import java.time.LocalDateTime

@Service
class UserNotificationUseCase(
    private val notificationRepository: NotificationRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun notifyUserRegistration(event: UserRegisteredEvent): Mono<Void> {
        val notification = NotificationEntity(
            userId = event.userId,
            eventType = "USER_REGISTERED",
            title = "Добро пожаловать!",
            message = "Привет, ${event.firstName}! Ваш аккаунт успешно создан. Добро пожаловать на нашу платформу!",
            isRead = false,
            channel = "IN_APP",
            createdAt = LocalDateTime.now()
        )
        
        return notificationRepository.save(notification)
            .then()
            .doOnError { logger.error("Failed to save registration notification for user ${event.userId}", it) }
    }

    fun notifyProfileUpdate(event: UserProfileUpdatedEvent): Mono<Void> {
        val changes = buildString {
            if (event.email != null) append("Email, ")
            if (event.firstName != null) append("Имя, ")
            if (event.lastName != null) append("Фамилия")
        }.trimEnd(',').trim()

        val notification = NotificationEntity(
            userId = event.userId,
            eventType = "USER_PROFILE_UPDATED",
            title = "Профиль обновлен",
            message = "Ваш профиль был обновлен: $changes",
            isRead = false,
            channel = "IN_APP",
            createdAt = LocalDateTime.now()
        )
        
        return notificationRepository.save(notification)
            .then()
            .doOnError { logger.error("Failed to save profile update notification for user ${event.userId}", it) }
    }

    fun notifyUserDeletion(event: UserDeletedEvent): Mono<Void> {
        val notification = NotificationEntity(
            userId = event.userId,
            eventType = "USER_DELETED",
            title = "Аккаунт удален",
            message = "Ваш аккаунт был удален. Если это произошло по ошибке, пожалуйста, свяжитесь с поддержкой.",
            isRead = false,
            channel = "IN_APP",
            createdAt = LocalDateTime.now()
        )
        
        return notificationRepository.save(notification)
            .then()
            .doOnError { logger.error("Failed to save deletion notification for user ${event.userId}", it) }
    }

    fun notifyRoleAssignment(event: UserRoleAssignedEvent): Mono<Void> {
        val roleTranslation = when (event.role) {
            "SELLER" -> "Продавец"
            "MODERATOR" -> "Модератор"
            else -> event.role
        }

        val notification = NotificationEntity(
            userId = event.userId,
            eventType = "USER_ROLE_ASSIGNED",
            title = "Новая роль",
            message = "Вам была выдана роль: $roleTranslation. Теперь у вас есть доступ к дополнительным функциям.",
            isRead = false,
            channel = "IN_APP",
            createdAt = LocalDateTime.now()
        )
        
        return notificationRepository.save(notification)
            .then()
            .doOnError { logger.error("Failed to save role assignment notification for user ${event.userId}", it) }
    }
}
