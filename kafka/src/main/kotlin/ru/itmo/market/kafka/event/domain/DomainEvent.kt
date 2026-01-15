package ru.itmo.market.kafka.event.domain

import java.time.LocalDateTime
import java.util.UUID

/**
 * Base interface for all domain events
 * Events are immutable, represent something that already happened in the business domain
 */
interface DomainEvent {
    val eventId: EventId
    val aggregateId: AggregateId
    val aggregateType: String
    val eventType: String
    val timestamp: LocalDateTime
    val version: Int
    val correlationId: String  // For distributed tracing
}