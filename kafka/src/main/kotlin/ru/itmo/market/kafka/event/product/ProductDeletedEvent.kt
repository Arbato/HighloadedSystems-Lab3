package ru.itmo.market.kafka.event.product

import ru.itmo.market.kafka.event.domain.AbstractDomainEvent
import ru.itmo.market.kafka.event.domain.AggregateId
import ru.itmo.market.kafka.event.domain.EventId
import ru.itmo.market.kafka.event.ids.ProductId
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Event: Product has been deleted
 * Published by: Product Service
 * Consumed by: Order Service (mark as unavailable), Notification Service
 */
data class ProductDeletedEvent(
    val productId: Long,
    val shopId: Long,
    val reason: String = "Seller deleted product",
    override val eventId: EventId = EventId.generate(),
    override val aggregateId: AggregateId = ProductId(productId),
    override val timestamp: LocalDateTime = LocalDateTime.now(),
    override val correlationId: String = java.util.UUID.randomUUID().toString()
) : AbstractDomainEvent(
    eventId = eventId,
    aggregateId = aggregateId,
    aggregateType = "product",
    eventType = "product.deleted",
    timestamp = timestamp,
    version = 1,
    correlationId = correlationId
)