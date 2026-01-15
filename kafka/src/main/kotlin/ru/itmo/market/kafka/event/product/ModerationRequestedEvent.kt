package ru.itmo.market.kafka.event.product

import ru.itmo.market.kafka.event.domain.AbstractDomainEvent
import ru.itmo.market.kafka.event.domain.AggregateId
import ru.itmo.market.kafka.event.domain.EventId
import ru.itmo.market.kafka.event.ids.ProductId
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Event: Product moderation has been requested
 * Published by: Product Service (when new product created)
 * Consumed by: Moderation Service (creates moderation task)
 */
data class ModerationRequestedEvent(
    val productId: Long,
    val shopId: Long,
    val sellerId: Long,
    val name: String,
    val reason: String = "New product submitted for approval",
    override val eventId: EventId = EventId.generate(),
    override val aggregateId: AggregateId = ProductId(productId),
    override val timestamp: LocalDateTime = LocalDateTime.now(),
    override val correlationId: String = java.util.UUID.randomUUID().toString()
) : AbstractDomainEvent(
    eventId = eventId,
    aggregateId = aggregateId,
    aggregateType = "product",
    eventType = "moderation.requested",
    timestamp = timestamp,
    version = 1,
    correlationId = correlationId
)