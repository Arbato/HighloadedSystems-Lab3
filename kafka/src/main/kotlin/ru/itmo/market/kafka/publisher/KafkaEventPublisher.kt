package ru.itmo.market.kafka.publisher

import mu.KotlinLogging
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.Message
import org.springframework.messaging.support.MessageBuilder
import org.springframework.stereotype.Component
import ru.itmo.market.kafka.event.domain.DomainEvent
import java.util.concurrent.CompletableFuture


/**
 * Event publisher for publishing domain events to Kafka topics
 * Handles retries, error logging, and callback management
 */
@Component
class KafkaEventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, Any>
) {
    companion object {
        private val logger = KotlinLogging.logger {}
        private const val DEFAULT_PARTITION_KEY_PREFIX = "event"
        private const val MESSAGE_KEY = KafkaHeaders.KEY
        private const val PARTITION_ID = KafkaHeaders.PARTITION
    }


    /**
     * Publishes a single domain event to Kafka
     * 
     * @param event Domain event to publish
     * @param topicName Target topic name
     * @param partitionKey Optional partition key for routing (defaults to event aggregate ID)
     * @return CompletableFuture with send result
     */
    fun publishEvent(
        event: DomainEvent,
        topicName: String,
        partitionKey: String? = null
    ): CompletableFuture<Void> {
        val key = partitionKey ?: "${DEFAULT_PARTITION_KEY_PREFIX}-${event.aggregateId.value}"
        
        val message = MessageBuilder
            .withPayload(event)
            .setHeader(KafkaHeaders.TOPIC, topicName)
            .setHeader(MESSAGE_KEY, key)
            .setHeader("eventType", event.eventType)
            .setHeader("eventId", event.eventId.value)
            .setHeader("correlationId", event.correlationId)
            .setHeader("timestamp", event.timestamp.toString())
            .setHeader("aggregateType", event.aggregateType)
            .build()

        logger.info {
            "Publishing event: type=${event.eventType}, " +
            "eventId=${event.eventId}, " +
            "aggregateId=${event.aggregateId.value}, " +
            "topic=$topicName, " +
            "key=$key"
        }

        return kafkaTemplate
            .send(message)
            .thenAccept { result ->
                logger.debug {
                    "Event published successfully: " +
                    "topic=${result.recordMetadata.topic()}, " +
                    "partition=${result.recordMetadata.partition()}, " +
                    "offset=${result.recordMetadata.offset()}"
                }
            }
            .exceptionally { exception ->
                logger.error(exception) {
                    "Failed to publish event: " +
                    "type=${event.eventType}, " +
                    "eventId=${event.eventId}, " +
                    "aggregateId=${event.aggregateId.value}"
                }
                throw exception
            }
    }


    /**
     * Publishes multiple domain events in batch
     * Maintains order per partition
     * 
     * @param events List of domain events to publish
     * @param topicName Target topic name
     * @return CompletableFuture that completes when all events are sent
     */
    fun publishEvents(
        events: List<DomainEvent>,
        topicName: String
    ): CompletableFuture<Void> {
        logger.info { "Publishing batch of ${events.size} events to topic: $topicName" }

        val futures = events.map { event ->
            publishEvent(event, topicName)
        }

        return CompletableFuture.allOf(*futures.toTypedArray())
            .thenAccept {
                logger.info { "All ${events.size} events published successfully" }
            }
            .exceptionally { exception ->
                logger.error(exception) {
                    "Batch publish failed for topic: $topicName"
                }
                throw exception
            }
    }


    /**
     * Publishes event with custom headers
     * Useful for passing additional context
     * 
     * @param event Domain event to publish
     * @param topicName Target topic name
     * @param customHeaders Additional headers to add
     * @return CompletableFuture with send result
     */
    fun publishEventWithHeaders(
        event: DomainEvent,
        topicName: String,
        customHeaders: Map<String, String> = emptyMap()
    ): CompletableFuture<Void> {
        val messageBuilder = MessageBuilder
            .withPayload(event)
            .setHeader(KafkaHeaders.TOPIC, topicName)
            .setHeader(MESSAGE_KEY, event.aggregateId.value.toString())
            .setHeader("eventType", event.eventType)
            .setHeader("eventId", event.eventId.value)
            .setHeader("correlationId", event.correlationId)

        customHeaders.forEach { (key, value) ->
            messageBuilder.setHeader(key, value)
        }

        val message = messageBuilder.build()

        return kafkaTemplate
            .send(message)
            .thenAccept { result ->
                logger.debug {
                    "Event with custom headers published: " +
                    "topic=${result.recordMetadata.topic()}, " +
                    "offset=${result.recordMetadata.offset()}"
                }
            }
            .exceptionally { exception ->
                logger.error(exception) {
                    "Failed to publish event with custom headers: ${event.eventType}"
                }
                throw exception
            }
    }


    /**
     * Publishes event to specific partition
     * For use cases where partition control is needed
     * 
     * @param event Domain event to publish
     * @param topicName Target topic name
     * @param partition Target partition number
     * @return CompletableFuture with send result
     */
    fun publishEventToPartition(
        event: DomainEvent,
        topicName: String,
        partition: Int
    ): CompletableFuture<Void> {
        val message = MessageBuilder
            .withPayload(event)
            .setHeader(KafkaHeaders.TOPIC, topicName)
            .setHeader(PARTITION_ID, partition)
            .setHeader(MESSAGE_KEY, event.aggregateId.value.toString())
            .setHeader("eventType", event.eventType)
            .setHeader("correlationId", event.correlationId)
            .build()

        logger.info {
            "Publishing event to specific partition: " +
            "topic=$topicName, partition=$partition, eventType=${event.eventType}"
        }

        return kafkaTemplate
            .send(message)
            .thenAccept { result ->
                logger.debug {
                    "Event published to partition $partition: offset=${result.recordMetadata.offset()}"
                }
            }
            .exceptionally { exception ->
                logger.error(exception) {
                    "Failed to publish event to partition $partition"
                }
                throw exception
            }
    }
}
