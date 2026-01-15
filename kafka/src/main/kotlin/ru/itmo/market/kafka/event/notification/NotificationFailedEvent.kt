package ru.itmo.market.kafka.event.notification

import ru.itmo.market.kafka.event.domain.AbstractDomainEvent
import ru.itmo.market.kafka.event.domain.AggregateId
import ru.itmo.market.kafka.event.domain.EventId
import ru.itmo.market.kafka.event.ids.NotificationId
import java.time.LocalDateTime

/**
 * Event: Notification sending has failed
 * Published by: Notification Service
 * Consumed by: Retry Service, Audit Service
 */
data class NotificationFailedEvent(
    val notificationId: Long,
    val type: String,
    val recipient: String,
    val errorMessage: String,
    val errorCode: String,
    val retryable: Boolean = true,
    override val eventId: EventId = EventId.generate(),
    override val aggregateId: AggregateId = NotificationId(notificationId),
    override val timestamp: LocalDateTime = LocalDateTime.now(),
    override val correlationId: String = java.util.UUID.randomUUID().toString()
) : AbstractDomainEvent(
    eventId = eventId,
    aggregateId = aggregateId,
    aggregateType = "notification",
    eventType = "notification.failed",
    timestamp = timestamp,
    version = 1,
    correlationId = correlationId
)