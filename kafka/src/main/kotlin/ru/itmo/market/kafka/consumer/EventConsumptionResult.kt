package ru.itmo.market.kafka.consumer

import mu.KotlinLogging
import org.springframework.kafka.support.Acknowledgment
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import ru.itmo.market.kafka.event.domain.DomainEvent
import java.time.LocalDateTime

/**
 * Event consumer result tracking
 */
data class EventConsumptionResult(
    val eventId: String,
    val eventType: String,
    val success: Boolean,
    val processingTimeMs: Long,
    val error: String? = null,
    val timestamp: LocalDateTime = LocalDateTime.now()
)