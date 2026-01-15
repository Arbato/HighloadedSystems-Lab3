package ru.itmo.market.kafka.event.moderation

import ru.itmo.market.kafka.event.domain.AbstractDomainEvent
import ru.itmo.market.kafka.event.domain.AggregateId
import ru.itmo.market.kafka.event.domain.EventId
import ru.itmo.market.kafka.event.ids.ModerationTaskId
import java.time.LocalDateTime

/**
 * Event: Product needs revision from seller
 * Published by: Moderation Service
 * Consumed by: Product Service, Notification Service
 */
data class ProductRevisionRequestedEvent(
    val taskId: Long,
    val productId: Long,
    val sellerId: Long,
    val sellerEmail: String,
    val moderatorId: Long,
    val revisionNotes: String,
    val revisionDeadlineHours: Int = 48,
    override val eventId: EventId = EventId.generate(),
    override val aggregateId: AggregateId = ModerationTaskId(taskId),
    override val timestamp: LocalDateTime = LocalDateTime.now(),
    override val correlationId: String = java.util.UUID.randomUUID().toString()
) : AbstractDomainEvent(
    eventId = eventId,
    aggregateId = aggregateId,
    aggregateType = "moderation_task",
    eventType = "product.revision.requested",
    timestamp = timestamp,
    version = 1,
    correlationId = correlationId
)