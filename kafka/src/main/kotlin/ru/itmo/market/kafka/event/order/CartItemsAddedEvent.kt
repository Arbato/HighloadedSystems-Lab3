package ru.itmo.market.kafka.event.order

import ru.itmo.market.kafka.event.domain.AbstractDomainEvent
import ru.itmo.market.kafka.event.domain.AggregateId
import ru.itmo.market.kafka.event.domain.EventId
import ru.itmo.market.kafka.event.ids.CartId
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Event: Order items have been added to cart
 * Published by: Order Service
 * Consumed by: Product Service (stock reservation)
 */
data class CartItemsAddedEvent(
    val cartId: Long,
    val userId: Long,
    val itemsCount: Int,
    val totalValue: BigDecimal,
    override val eventId: EventId = EventId.generate(),
    override val aggregateId: AggregateId = CartId(cartId),
    override val timestamp: LocalDateTime = LocalDateTime.now(),
    override val correlationId: String = java.util.UUID.randomUUID().toString()
) : AbstractDomainEvent(
    eventId = eventId,
    aggregateId = aggregateId,
    aggregateType = "cart",
    eventType = "cart.items.added",
    timestamp = timestamp,
    version = 1,
    correlationId = correlationId
)