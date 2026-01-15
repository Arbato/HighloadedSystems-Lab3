package ru.itmo.market.kafka.event.product

import ru.itmo.market.kafka.event.domain.AbstractDomainEvent
import ru.itmo.market.kafka.event.domain.AggregateId
import ru.itmo.market.kafka.event.domain.EventId
import ru.itmo.market.kafka.event.ids.ProductId
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Event: Product has been created and needs moderation
 * Published by: Product Service
 * Consumed by: Moderation Service (create moderation task), Notification Service
 */
data class ProductCreatedEvent(
    val productId: Long,
    val shopId: Long,
    val sellerId: Long,
    val name: String,
    val description: String,
    val price: BigDecimal,
    val imageUrl: String?,
    val status: String = "PENDING",
    override val eventId: EventId = EventId.generate(),
    override val aggregateId: AggregateId = ProductId(productId),
    override val timestamp: LocalDateTime = LocalDateTime.now(),
    override val correlationId: String = java.util.UUID.randomUUID().toString()
) : AbstractDomainEvent(
    eventId = eventId,
    aggregateId = aggregateId,
    aggregateType = "product",
    eventType = "product.created",
    timestamp = timestamp,
    version = 1,
    correlationId = correlationId
)