package ru.itmo.notificationservice.kafka.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import reactor.core.scheduler.Schedulers
import ru.itmo.notificationservice.model.events.*
import ru.itmo.notificationservice.service.application.UserNotificationUseCase
import java.time.Duration

@Component
class UserNotificationEventConsumer(
    private val userNotificationUseCase: UserNotificationUseCase,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = ["user-events"],
        groupId = "notification-service-consumer",
        properties = [
            "spring.json.trusted.packages=*",
            "spring.json.use.type.info.headers=false"
        ]
    )
    fun handleUserEvent(
        @Payload eventJson: String,
        @Header(KafkaHeaders.RECEIVED_PARTITION) partition: Int,
        @Header(KafkaHeaders.OFFSET) offset: Long,
        ack: Acknowledgment?
    ) {
        try {
            logger.info("Received user event from partition=$partition, offset=$offset")
            
            val event = objectMapper.readValue(eventJson, NotificationEvent::class.java)
            
            when (event) {
                is UserRegisteredEvent -> handleUserRegistered(event)
                is UserProfileUpdatedEvent -> handleUserProfileUpdated(event)
                is UserDeletedEvent -> handleUserDeleted(event)
                is UserRoleAssignedEvent -> handleUserRoleAssigned(event)
            }
            
            ack?.acknowledge()
            logger.info("Successfully processed event: ${event.eventType}")
            
        } catch (e: Exception) {
            logger.error("Error processing notification event", e)
            // Отправить в DLQ при ошибке
            ack?.nack(Duration.ofSeconds(1000))
        }
    }

    private fun handleUserRegistered(event: UserRegisteredEvent) {
        userNotificationUseCase.notifyUserRegistration(event)
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe(
                { logger.info("User registration notification sent: userId=${event.userId}") },
                { logger.error("Failed to send registration notification", it) }
            )
    }

    private fun handleUserProfileUpdated(event: UserProfileUpdatedEvent) {
        userNotificationUseCase.notifyProfileUpdate(event)
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe(
                { logger.info("Profile update notification sent: userId=${event.userId}") },
                { logger.error("Failed to send profile update notification", it) }
            )
    }

    private fun handleUserDeleted(event: UserDeletedEvent) {
        userNotificationUseCase.notifyUserDeletion(event)
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe(
                { logger.info("User deletion notification sent: userId=${event.userId}") },
                { logger.error("Failed to send deletion notification", it) }
            )
    }

    private fun handleUserRoleAssigned(event: UserRoleAssignedEvent) {
        userNotificationUseCase.notifyRoleAssignment(event)
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe(
                { logger.info("Role assignment notification sent: userId=${event.userId}") },
                { logger.error("Failed to send role assignment notification", it) }
            )
    }
}
