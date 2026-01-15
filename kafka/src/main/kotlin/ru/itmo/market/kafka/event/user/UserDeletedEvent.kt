package ru.itmo.market.kafka.event.user

import ru.itmo.market.kafka.event.domain.AbstractDomainEvent
import ru.itmo.market.kafka.event.domain.AggregateId
import ru.itmo.market.kafka.event.domain.EventId
import ru.itmo.market.kafka.event.ids.UserId
import java.time.LocalDateTime

/**
 * Event: User has been deleted
 * Published by: User Service
 * Consumed by: Product Service (cascade delete), Order Service (cleanup)
 */
data class UserDeletedEvent(
    val userId: Long,
    val email: String,
    val reason: String = "User requested deletion",
    override val eventId: EventId = EventId.generate(),
    override val aggregateId: AggregateId = UserId(userId),
    override val timestamp: LocalDateTime = LocalDateTime.now(),
    override val correlationId: String = java.util.UUID.randomUUID().toString()
) : AbstractDomainEvent(
    eventId = eventId,
    aggregateId = aggregateId,
    aggregateType = "user",
    eventType = "user.deleted",
    timestamp = timestamp,
    version = 1,
    correlationId = correlationId
)