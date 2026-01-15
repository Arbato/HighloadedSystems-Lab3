package ru.itmo.market.kafka.event.domain

import java.time.LocalDateTime
import java.util.UUID

/**
 * Abstract base class for domain events
 * Provides common functionality for all events
 */
abstract class AbstractDomainEvent(
    override val eventId: EventId,
    override val aggregateId: AggregateId,
    override val aggregateType: String,
    override val eventType: String,
    override val timestamp: LocalDateTime = LocalDateTime.now(),
    override val version: Int = 1,
    override val correlationId: String = UUID.randomUUID().toString()
) : DomainEvent {
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AbstractDomainEvent) return false
        return eventId == other.eventId
    }
    
    override fun hashCode(): Int = eventId.hashCode()
    
    override fun toString(): String = 
        "$eventType(eventId=$eventId, aggregateId=$aggregateId, timestamp=$timestamp)"
}