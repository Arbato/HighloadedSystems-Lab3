package ru.itmo.market.kafka.consumer.implementation

import mu.KotlinLogging
import org.springframework.kafka.support.Acknowledgment
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import ru.itmo.market.kafka.event.domain.DomainEvent
import java.time.LocalDateTime

/**
 * Base interface for event consumers
 * All event consumers should implement this interface
 */
interface DomainEventConsumer {
    /**
     * Handles a domain event
     * 
     * @param event The domain event to handle
     * @param acknowledgment Manual acknowledgment for Kafka
     * @param topic The topic from which the message was received
     * @param partition The partition from which the message was received
     * @param offset The offset of the message
     * @throws Exception if event handling fails
     */
    suspend fun handle(
        event: DomainEvent,
        acknowledgment: Acknowledgment?,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
        @Header(KafkaHeaders.RECEIVED_PARTITION) partition: Int,
        @Header(KafkaHeaders.OFFSET) offset: Long
    )
}