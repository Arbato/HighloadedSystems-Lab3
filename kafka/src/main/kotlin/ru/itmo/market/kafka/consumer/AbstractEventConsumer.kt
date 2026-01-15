package ru.itmo.market.kafka.consumer

import mu.KotlinLogging
import org.springframework.kafka.support.Acknowledgment
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import ru.itmo.market.kafka.event.domain.DomainEvent
import java.time.LocalDateTime

abstract class AbstractEventConsumer {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    protected fun logConsumption(
        event: DomainEvent,
        topic: String,
        partition: Int,
        offset: Long
    ) {
        logger.info {
            "Consuming event: " +
            "type=${event.eventType}, " +
            "eventId=${event.eventId}, " +
            "aggregateId=${event.aggregateId}, " +
            "topic=$topic, " +
            "partition=$partition, " +
            "offset=$offset, " +
            "timestamp=${event.timestamp}"
        }
    }

    protected fun logSuccess(
        event: DomainEvent,
        processingTimeMs: Long
    ) {
        logger.debug {
            "Event processed successfully: " +
            "type=${event.eventType}, " +
            "eventId=${event.eventId}, " +
            "processingTimeMs=$processingTimeMs"
        }
    }

    protected fun logError(
        event: DomainEvent,
        exception: Exception,
        topic: String,
        partition: Int,
        offset: Long
    ) {
        logger.error(exception) {
            "Failed to process event: " +
            "type=${event.eventType}, " +
            "eventId=${event.eventId}, " +
            "aggregateId=${event.aggregateId}, " +
            "topic=$topic, " +
            "partition=$partition, " +
            "offset=$offset"
        }
    }

    protected fun getCorrelationId(event: DomainEvent): String {
        return event.correlationId
    }
}