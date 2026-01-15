package ru.itmo.market.kafka.event.notification

import ru.itmo.market.kafka.event.domain.AbstractDomainEvent
import ru.itmo.market.kafka.event.domain.AggregateId
import ru.itmo.market.kafka.event.domain.EventId
import ru.itmo.market.kafka.event.ids.NotificationId
import java.time.LocalDateTime

/**
 * Event: Push notification should be sent
 * Published by: Services requesting notification
 * Consumed by: Notification Service (send push)
 */
data class SendPushNotificationEvent(
    val notificationId: Long,
    val userId: Long,
    val deviceToken: String,
    val title: String,
    val body: String,
    val actionUrl: String? = null,
    val priority: String = "NORMAL",
    override val eventId: EventId = EventId.generate(),
    override val aggregateId: AggregateId = NotificationId(notificationId),
    override val timestamp: LocalDateTime = LocalDateTime.now(),
    override val correlationId: String = java.util.UUID.randomUUID().toString()
) : AbstractDomainEvent(
    eventId = eventId,
    aggregateId = aggregateId,
    aggregateType = "notification",
    eventType = "notification.push.send",
    timestamp = timestamp,
    version = 1,
    correlationId = correlationId
)