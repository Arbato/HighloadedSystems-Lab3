package ru.itmo.market.kafka.event.user

import ru.itmo.market.kafka.event.domain.AbstractDomainEvent
import ru.itmo.market.kafka.event.domain.AggregateId
import ru.itmo.market.kafka.event.domain.EventId
import ru.itmo.market.kafka.event.ids.UserId
import java.time.LocalDateTime

data class UserRegisteredEvent(
    val userId: Long,
    val email: String,
    val username: String,
    val firstName: String,
    val lastName: String,
    override val eventId: EventId = EventId.generate(),
    override val aggregateId: AggregateId = UserId(userId),
    override val timestamp: LocalDateTime = LocalDateTime.now(),
    override val correlationId: String = java.util.UUID.randomUUID().toString()
) : AbstractDomainEvent(
    eventId = eventId,
    aggregateId = aggregateId,
    aggregateType = "user",
    eventType = "user.registered",
    timestamp = timestamp,
    version = 1,
    correlationId = correlationId
)