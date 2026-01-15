package ru.itmo.market.kafka.event.moderation

import ru.itmo.market.kafka.event.domain.AbstractDomainEvent
import ru.itmo.market.kafka.event.domain.AggregateId
import ru.itmo.market.kafka.event.domain.EventId
import ru.itmo.market.kafka.event.ids.ModerationTaskId
import java.time.LocalDateTime

/**
 * Event: Product has been rejected by moderator
 * Published by: Moderation Service
 * Consumed by: Product Service (update product status), Notification Service
 */
data class ProductRejectedEvent(
    val taskId: Long,
    val productId: Long,
    val sellerId: Long,
    val sellerEmail: String,
    val moderatorId: Long,
    val reason: String,
    val rejectionReason: String,
    override val eventId: EventId = EventId.generate(),
    override val aggregateId: AggregateId = ModerationTaskId(taskId),
    override val timestamp: LocalDateTime = LocalDateTime.now(),
    override val correlationId: String = java.util.UUID.randomUUID().toString()
) : AbstractDomainEvent(
    eventId = eventId,
    aggregateId = aggregateId,
    aggregateType = "moderation_task",
    eventType = "product.rejected",
    timestamp = timestamp,
    version = 1,
    correlationId = correlationId
)