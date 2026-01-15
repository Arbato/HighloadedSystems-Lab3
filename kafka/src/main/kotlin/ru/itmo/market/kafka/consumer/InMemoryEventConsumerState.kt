package ru.itmo.market.kafka.consumer

import mu.KotlinLogging
import org.springframework.kafka.support.Acknowledgment
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import ru.itmo.market.kafka.event.domain.DomainEvent
import java.time.LocalDateTime

/**
 * In-memory implementation of EventConsumerState
 */
class InMemoryEventConsumerState : EventConsumerState {
    private var totalProcessed: Long = 0
    private var totalFailed: Long = 0
    private var totalProcessingTimeMs: Long = 0
    private var lastProcessedTime: LocalDateTime? = null
    private var lastFailureTime: LocalDateTime? = null
    private var lastErrorMessage: String? = null

    override fun recordSuccess(event: DomainEvent, processingTimeMs: Long) {
        totalProcessed++
        totalProcessingTimeMs += processingTimeMs
        lastProcessedTime = LocalDateTime.now()
    }

    override fun recordFailure(event: DomainEvent, exception: Exception) {
        totalFailed++
        lastFailureTime = LocalDateTime.now()
        lastErrorMessage = exception.message
    }

    override fun getMetrics(): ConsumerMetrics {
        val averageTime = if (totalProcessed > 0) {
            totalProcessingTimeMs.toDouble() / totalProcessed
        } else {
            0.0
        }

        return ConsumerMetrics(
            totalProcessed = totalProcessed,
            totalFailed = totalFailed,
            averageProcessingTimeMs = averageTime,
            lastProcessedTime = lastProcessedTime,
            lastFailureTime = lastFailureTime,
            lastErrorMessage = lastErrorMessage
        )
    }
}