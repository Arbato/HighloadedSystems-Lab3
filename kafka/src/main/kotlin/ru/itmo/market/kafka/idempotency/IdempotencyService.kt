package ru.itmo.market.kafka.idempotency

import jakarta.persistence.*
import mu.KotlinLogging
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class IdempotencyService(
    private val processedEventRepository: ProcessedEventRepository
) {
    companion object {
        private val logger = KotlinLogging.logger { }
    }

    fun isEventProcessed(
        eventId: String,
        consumerGroup: String
    ): Boolean {
        return processedEventRepository.findByEventIdAndConsumerGroup(eventId, consumerGroup) != null
    }

    @Transactional
    fun markEventAsProcessed(
        eventId: String,
        eventType: String,
        aggregateId: Long,
        aggregateType: String,
        consumerGroup: String,
        processingTimeMs: Long = 0
    ): ProcessedEvent {
        try {
            val existing = processedEventRepository.findByEventId(eventId)
            if (existing != null) {
                logger.debug { "Event already processed: $eventId" }
                return existing
            }

            val processedEvent = ProcessedEvent(
                eventId = eventId,
                eventType = eventType,
                aggregateId = aggregateId,
                aggregateType = aggregateType,
                consumerGroup = consumerGroup,
                processingTimeMs = processingTimeMs
            )

            val saved = processedEventRepository.save(processedEvent)
            logger.info { "Event marked as processed. EventId: $eventId" }
            return saved
        } catch (e: Exception) {
            logger.error(e) { "Error marking event as processed: $eventId" }
            throw e
        }
    }

    fun getProcessedEvent(eventId: String): ProcessedEvent? {
        return processedEventRepository.findByEventId(eventId)
    }

    @Transactional
    fun cleanupOldProcessedEvents(daysToKeep: Long = 30): Int {
        val before = LocalDateTime.now().minusDays(daysToKeep)
        val deleted = processedEventRepository.deleteOlderThan(before)
        logger.info { "Cleaned up $deleted processed events" }
        return deleted
    }
}
