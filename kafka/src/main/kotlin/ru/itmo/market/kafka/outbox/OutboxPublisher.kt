package ru.itmo.market.kafka.outbox

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.retry.annotation.Retry
import mu.KotlinLogging
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import ru.itmo.market.kafka.event.domain.DomainEvent
import ru.itmo.market.kafka.publisher.KafkaEventPublisher
import java.time.LocalDateTime

@Component
@EnableScheduling
class OutboxPublisher(
    private val outboxRepository: OutboxRepository,
    private val kafkaEventPublisher: KafkaEventPublisher,
    private val objectMapper: ObjectMapper
) {
    companion object {
        private val logger = KotlinLogging.logger {}
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val BATCH_SIZE = 50
    }

    @Scheduled(fixedDelay = 5000)
    @CircuitBreaker(name = "outbox-publisher", fallbackMethod = "handleFailure")
    @Retry(name = "outbox-publisher")
    fun publishUnpublishedEvents() {
        try {
            val events = outboxRepository.findByPublishedFalseOrderByCreatedAtAsc()
            if (events.isEmpty()) return

            logger.info { "Publishing ${events.size} events from outbox" }

            events.chunked(BATCH_SIZE).forEach { batch ->
                batch.forEach { event ->
                    try {
                        publishEvent(event)
                        outboxRepository.markAsPublished(event.id!!, LocalDateTime.now())
                    } catch (e: Exception) {
                        outboxRepository.incrementAttempts(event.id!!, LocalDateTime.now())
                        logger.warn(e) { "Failed to publish outbox event: ${event.id}" }
                    }
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Outbox publishing failed" }
        }
    }

    @Scheduled(fixedDelay = 60000)
    fun retryFailedEvents() {
        try {
            val failedEvents = outboxRepository.findFailedEvents(MAX_RETRY_ATTEMPTS, LocalDateTime.now().minusMinutes(1))
            failedEvents.forEach { event ->
                try {
                    publishEvent(event)
                    outboxRepository.markAsPublished(event.id!!, LocalDateTime.now())
                    logger.info { "Retried event published: ${event.id}" }
                } catch (e: Exception) {
                    outboxRepository.incrementAttempts(event.id!!, LocalDateTime.now())
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Retry failed events error" }
        }
    }

    @Scheduled(cron = "0 0 2 * * *")
    fun cleanupPublishedEvents() {
        try {
            val deleted = outboxRepository.deletePublishedOlderThan(LocalDateTime.now().minusDays(7))
            logger.info { "Deleted $deleted old published events" }
        } catch (e: Exception) {
            logger.error(e) { "Cleanup error" }
        }
    }

    private fun publishEvent(event: OutboxEvent) {
        val topic = getTopic(event.eventType)
        kafkaEventPublisher.publishEvent(
            event.toKafkaEvent(),
            topic,
            event.aggregateId.toString()
        ).join()
    }

    private fun OutboxEvent.toKafkaEvent(): DomainEvent {
        return objectMapper.readValue(payload, DomainEvent::class.java)
    }

    private fun getTopic(eventType: String) = eventType

    fun handleFailure(exception: Exception) {
        logger.error(exception) { "Outbox publisher circuit breaker opened" }
    }
}

@Component
class OutboxService(
    private val outboxRepository: OutboxRepository,
    private val objectMapper: ObjectMapper
) {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    @Transactional
    fun saveEvent(event: DomainEvent) {
        try {
            val outboxEvent = OutboxEvent(
                eventType = event.eventType,
                aggregateId = event.aggregateId.value,
                aggregateType = event.aggregateType,
                payload = objectMapper.writeValueAsString(event),
                correlationId = event.correlationId
            )
            outboxRepository.save(outboxEvent)
            logger.debug { "Event saved to outbox: ${event.eventType}" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to save event to outbox" }
            throw e
        }
    }
}
