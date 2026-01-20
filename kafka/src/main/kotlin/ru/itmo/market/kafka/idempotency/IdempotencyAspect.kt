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
class IdempotencyAspect(
    private val idempotencyService: IdempotencyService
) {
    companion object {
        private val logger = KotlinLogging.logger { }
    }

    fun checkAndMarkProcessed(
        eventId: String,
        eventType: String,
        aggregateId: Long,
        aggregateType: String,
        consumerGroup: String
    ): Boolean {
        if (idempotencyService.isEventProcessed(eventId, consumerGroup)) {
            logger.debug { "Skipping duplicate event: $eventId" }
            return false
        }

        try {
            idempotencyService.markEventAsProcessed(
                eventId, eventType, aggregateId, aggregateType, consumerGroup
            )
            return true
        } catch (e: Exception) {
            logger.error(e) { "Error checking idempotency" }
            return true
        }
    }
}