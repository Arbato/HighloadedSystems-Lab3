package ru.itmo.market.kafka.consumer

import mu.KotlinLogging
import org.springframework.kafka.support.Acknowledgment
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import ru.itmo.market.kafka.event.domain.DomainEvent
import java.time.LocalDateTime

/**
 * Event consumer metrics
 */
data class ConsumerMetrics(
    val totalProcessed: Long = 0,
    val totalFailed: Long = 0,
    val averageProcessingTimeMs: Double = 0.0,
    val lastProcessedTime: LocalDateTime? = null,
    val lastFailureTime: LocalDateTime? = null,
    val lastErrorMessage: String? = null
)