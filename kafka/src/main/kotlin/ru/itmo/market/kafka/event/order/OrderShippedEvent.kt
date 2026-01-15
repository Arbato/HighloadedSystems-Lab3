package ru.itmo.market.kafka.event.order

import ru.itmo.market.kafka.event.domain.AbstractDomainEvent
import ru.itmo.market.kafka.event.domain.AggregateId
import ru.itmo.market.kafka.event.domain.EventId
import ru.itmo.market.kafka.event.ids.OrderId
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Event: Order has been shipped
 * Published by: Order Service
 * Consumed by: Notification Service (send shipping confirmation)
 */
data class OrderShippedEvent(
    val orderId: Long,
    val userId: Long,
    val userEmail: String,
    val trackingNumber: String?,
    val shippingCarrier: String?,
    val estimatedDeliveryDate: LocalDateTime?,
    override val eventId: EventId = EventId.generate(),
    override val aggregateId: AggregateId = OrderId(orderId),
    override val timestamp: LocalDateTime = LocalDateTime.now(),
    override val correlationId: String = java.util.UUID.randomUUID().toString()
) : AbstractDomainEvent(
    eventId = eventId,
    aggregateId = aggregateId,
    aggregateType = "order",
    eventType = "order.shipped",
    timestamp = timestamp,
    version = 1,
    correlationId = correlationId
)