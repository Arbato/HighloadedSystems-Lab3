package ru.itmo.market.kafka.event.product

import ru.itmo.market.kafka.event.domain.AbstractDomainEvent
import ru.itmo.market.kafka.event.domain.AggregateId
import ru.itmo.market.kafka.event.domain.EventId
import ru.itmo.market.kafka.event.ids.*
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Event: Product inventory has changed
 * Published by: Product Service
 * Consumed by: Order Service (stock check), Notification Service
 */
data class InventoryChangedEvent(
    val productId: Long,
    val quantity: Long,
    val previousQuantity: Long,
    val reason: String = "Unknown",  // "purchase", "restock", "damaged", etc.
    override val eventId: EventId = EventId.generate(),
    override val aggregateId: AggregateId = ProductId(productId),
    override val timestamp: LocalDateTime = LocalDateTime.now(),
    override val correlationId: String = java.util.UUID.randomUUID().toString()
) : AbstractDomainEvent(
    eventId = eventId,
    aggregateId = aggregateId,
    aggregateType = "product",
    eventType = "inventory.changed",
    timestamp = timestamp,
    version = 1,
    correlationId = correlationId
)