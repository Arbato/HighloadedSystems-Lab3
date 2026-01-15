package ru.itmo.market.kafka.event.notification

import ru.itmo.market.kafka.event.domain.AbstractDomainEvent
import ru.itmo.market.kafka.event.domain.AggregateId
import ru.itmo.market.kafka.event.domain.EventId
import ru.itmo.market.kafka.event.ids.NotificationId
import java.time.LocalDateTime

/**
 * Event: In-app notification should be created
 * Published by: Services requesting notification
 * Consumed by: Notification Service (store in DB)
 */
data class CreateInAppNotificationEvent(
    val notificationId: Long,
    val userId: Long,
    val title: String,
    val message: String,
    val actionUrl: String? = null,
    val type: String = "INFO",  // INFO, WARNING, ERROR, SUCCESS
    override val eventId: EventId = EventId.generate(),
    override val aggregateId: AggregateId = NotificationId(notificationId),
    override val timestamp: LocalDateTime = LocalDateTime.now(),
    override val correlationId: String = java.util.UUID.randomUUID().toString()
) : AbstractDomainEvent(
    eventId = eventId,
    aggregateId = aggregateId,
    aggregateType = "notification",
    eventType = "notification.inapp.create",
    timestamp = timestamp,
    version = 1,
    correlationId = correlationId
)