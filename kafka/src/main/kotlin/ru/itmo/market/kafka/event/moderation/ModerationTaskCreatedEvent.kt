package ru.itmo.market.kafka.event.moderation

import ru.itmo.market.kafka.event.domain.AbstractDomainEvent
import ru.itmo.market.kafka.event.domain.AggregateId
import ru.itmo.market.kafka.event.domain.EventId
import ru.itmo.market.kafka.event.ids.ModerationTaskId
import java.time.LocalDateTime

/**
 * Event: Moderation task has been created
 * Published by: Moderation Service
 * Consumed by: Notification Service (notify moderators)
 */
data class ModerationTaskCreatedEvent(
    val taskId: Long,
    val productId: Long,
    val sellerId: Long,
    val productName: String,
    val reason: String,
    val priority: String = "NORMAL",
    override val eventId: EventId = EventId.generate(),
    override val aggregateId: AggregateId = ModerationTaskId(taskId),
    override val timestamp: LocalDateTime = LocalDateTime.now(),
    override val correlationId: String = java.util.UUID.randomUUID().toString()
) : AbstractDomainEvent(
    eventId = eventId,
    aggregateId = aggregateId,
    aggregateType = "moderation_task",
    eventType = "moderation.task.created",
    timestamp = timestamp,
    version = 1,
    correlationId = correlationId
)