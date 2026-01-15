package ru.itmo.market.kafka.consumer

import mu.KotlinLogging
import org.springframework.kafka.support.Acknowledgment
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import ru.itmo.market.kafka.event.domain.DomainEvent
import java.time.LocalDateTime

/**
 * Event consumer state for tracking processing
 */
interface EventConsumerState {
    fun recordSuccess(event: DomainEvent, processingTimeMs: Long)
    fun recordFailure(event: DomainEvent, exception: Exception)
    fun getMetrics(): ConsumerMetrics
}