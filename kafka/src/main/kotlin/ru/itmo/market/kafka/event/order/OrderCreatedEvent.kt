package ru.itmo.market.kafka.event.order

import ru.itmo.market.kafka.event.domain.AbstractDomainEvent
import ru.itmo.market.kafka.event.domain.AggregateId
import ru.itmo.market.kafka.event.domain.EventId
import ru.itmo.market.kafka.event.ids.OrderId
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Event: Order has been created
 * Published by: Order Service
 * Consumed by: Product Service (update inventory), Notification Service (send confirmation)
 */
data class OrderCreatedEvent(
    val orderId: Long,
    val userId: Long,
    val userEmail: String,
    val totalPrice: BigDecimal,
    val itemsCount: Int,
    val status: String = "PENDING",
    override val eventId: EventId = EventId.generate(),
    override val aggregateId: AggregateId = OrderId(orderId),
    override val timestamp: LocalDateTime = LocalDateTime.now(),
    override val correlationId: String = java.util.UUID.randomUUID().toString()
) : AbstractDomainEvent(
    eventId = eventId,
    aggregateId = aggregateId,
    aggregateType = "order",
    eventType = "order.created",
    timestamp = timestamp,
    version = 1,
    correlationId = correlationId
)