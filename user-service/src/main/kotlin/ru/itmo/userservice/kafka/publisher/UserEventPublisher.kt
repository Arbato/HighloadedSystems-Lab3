package ru.itmo.userservice.kafka.publisher

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.support.MessageBuilder
import org.springframework.stereotype.Component
import ru.itmo.userservice.model.dto.response.UserResponse
import java.time.LocalDateTime

@Component
class UserEventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val topic = "user-events"

    /**
     * Публикует событие регистрации нового пользователя
     * Событие: USER_REGISTERED
     * Слушатель: notification-service
     */
    fun publishUserRegistered(user: UserResponse) {
        val event = mapOf(
            "event_type" to "USER_REGISTERED",
            "user_id" to user.id,
            "username" to user.username,
            "email" to user.email,
            "first_name" to user.firstName,
            "last_name" to user.lastName,
            "timestamp" to LocalDateTime.now().toString()
        )
        
        publishEvent(event, "USER_REGISTERED")
    }

    /**
     * Публикует событие обновления профиля пользователя
     * Событие: USER_PROFILE_UPDATED
     */
    fun publishProfileUpdated(
        userId: Long,
        email: String?,
        firstName: String?,
        lastName: String?
    ) {
        val event = mapOf(
            "event_type" to "USER_PROFILE_UPDATED",
            "user_id" to userId,
            "email" to email,
            "first_name" to firstName,
            "last_name" to lastName,
            "timestamp" to LocalDateTime.now().toString()
        )
        
        publishEvent(event, "USER_PROFILE_UPDATED")
    }

    /**
     * Публикует событие удаления пользователя
     * Событие: USER_DELETED
     */
    fun publishUserDeleted(userId: Long, username: String) {
        val event = mapOf(
            "event_type" to "USER_DELETED",
            "user_id" to userId,
            "username" to username,
            "timestamp" to LocalDateTime.now().toString()
        )
        
        publishEvent(event, "USER_DELETED")
    }

    /**
     * Публикует событие назначения роли пользователю
     * Событие: USER_ROLE_ASSIGNED
     */
    fun publishRoleAssigned(userId: Long, username: String, role: String) {
        val event = mapOf(
            "event_type" to "USER_ROLE_ASSIGNED",
            "user_id" to userId,
            "username" to username,
            "role" to role,
            "timestamp" to LocalDateTime.now().toString()
        )
        
        publishEvent(event, "USER_ROLE_ASSIGNED")
    }

    /**
     * Отправить событие в Kafka
     */
    private fun publishEvent(event: Map<String, Any?>, eventType: String) {
        try {
            val eventJson = objectMapper.writeValueAsString(event)
            val message = MessageBuilder.withPayload(eventJson)
                .setHeader(KafkaHeaders.TOPIC, topic)
                .setHeader("event_type", eventType)
                .setHeader("timestamp", System.currentTimeMillis())
                .build()
            
            kafkaTemplate.send(message)
            logger.info("Published event to Kafka: eventType=$eventType, topic=$topic")
            
        } catch (e: Exception) {
            logger.error("Failed to publish event: eventType=$eventType", e)
            // Не выбрасываем ошибку, чтобы не сломать основной процесс
        }
    }
}
