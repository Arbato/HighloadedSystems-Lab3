package ru.itmo.userservice.kafka.publisher

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.serializer.JsonSerializer
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import ru.itmo.userservice.model.dto.response.UserResponse
import java.time.LocalDateTime

@Component
class UserEventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, Any>,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(UserEventPublisher::class.java)
    private val topic = "user-events"

    private fun publishEvent(
        event: Map<String, Any?>, 
        eventType: String
    ): Mono<Unit> {
        val eventJson = objectMapper.writeValueAsString(event)
        val messageKey = "${eventType}-${event["user_id"]}"
        
        // ListenableFuture -> Mono<Unit>
        return Mono.fromFuture(kafkaTemplate.send(topic, messageKey, eventJson))
            .doOnSuccess { 
                logger.info("✅ $eventType published: userId=${event["user_id"]}")
            }
            .doOnError { ex ->
                logger.error("❌ Failed to publish $eventType for userId=${event["user_id"]}", ex)
            }
            .then(Mono.just(Unit))
    }

    fun publishUserRegistered(user: UserResponse): Mono<Unit> = publishEvent(
        mapOf(
            "event_type" to "USER_REGISTERED",
            "user_id" to user.id,
            "username" to user.username,
            "email" to user.email,
            "first_name" to user.firstName,
            "last_name" to user.lastName,
            "timestamp" to LocalDateTime.now().toString()
        ),
        "USER_REGISTERED"
    )

    fun publishProfileUpdated(
        userId: Long,
        email: String?,
        firstName: String?,
        lastName: String?
    ): Mono<Unit> = publishEvent(
        mapOf(
            "event_type" to "USER_PROFILE_UPDATED",
            "user_id" to userId,
            "email" to email,
            "first_name" to firstName,
            "last_name" to lastName,
            "timestamp" to LocalDateTime.now().toString()
        ),
        "USER_PROFILE_UPDATED"
    )

    fun publishUserDeleted(userId: Long, username: String): Mono<Unit> = publishEvent(
        mapOf(
            "event_type" to "USER_DELETED",
            "user_id" to userId,
            "username" to username,
            "timestamp" to LocalDateTime.now().toString()
        ),
        "USER_DELETED"
    )

    fun publishRoleAssigned(userId: Long, username: String, role: String): Mono<Unit> = publishEvent(
        mapOf(
            "event_type" to "USER_ROLE_ASSIGNED",
            "user_id" to userId,
            "username" to username,
            "role" to role,
            "timestamp" to LocalDateTime.now().toString()
        ),
        "USER_ROLE_ASSIGNED"
    )
}
