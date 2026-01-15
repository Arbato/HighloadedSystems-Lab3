package ru.itmo.market.kafka.event.product

import ru.itmo.market.kafka.event.domain.AbstractDomainEvent
import ru.itmo.market.kafka.event.domain.AggregateId
import ru.itmo.market.kafka.event.domain.EventId
import ru.itmo.market.kafka.event.ids.ProductId
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Event: Product information has been updated
 * Published by: Product Service
 * Consumed by: Order Service (price/info update), Notification Service
 */
data class ProductUpdatedEvent(
    val productId: Long,
    val shopId: Long,
    val name: String,
    val description: String,
    val price: BigDecimal,
    val imageUrl: String?,
    val fieldsChanged: Set<String>,  // "name", "price", "description", etc.
    override val eventId: EventId = EventId.generate(),
    override val aggregateId: AggregateId = ProductId(productId),
    override val timestamp: LocalDateTime = LocalDateTime.now(),
    override val correlationId: String = java.util.UUID.randomUUID().toString()
) : AbstractDomainEvent(
    eventId = eventId,
    aggregateId = aggregateId,
    aggregateType = "product",
    eventType = "product.updated",
    timestamp = timestamp,
    version = 1,
    correlationId = correlationId
)