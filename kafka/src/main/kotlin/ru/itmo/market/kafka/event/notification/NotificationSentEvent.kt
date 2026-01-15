package ru.itmo.market.kafka.event.notification

import ru.itmo.market.kafka.event.domain.AbstractDomainEvent
import ru.itmo.market.kafka.event.domain.AggregateId
import ru.itmo.market.kafka.event.domain.EventId
import ru.itmo.market.kafka.event.ids.NotificationId
import java.time.LocalDateTime

/**
 * Event: Notification has been successfully sent
 * Published by: Notification Service
 * Consumed by: Audit Service, Analytics
 */
data class NotificationSentEvent(
    val notificationId: Long,
    val type: String,  // "email", "sms", "push", "inapp"
    val recipient: String,
    val templateId: String? = null,
    val sentAt: LocalDateTime = LocalDateTime.now(),
    override val eventId: EventId = EventId.generate(),
    override val aggregateId: AggregateId = NotificationId(notificationId),
    override val timestamp: LocalDateTime = LocalDateTime.now(),
    override val correlationId: String = java.util.UUID.randomUUID().toString()
) : AbstractDomainEvent(
    eventId = eventId,
    aggregateId = aggregateId,
    aggregateType = "notification",
    eventType = "notification.sent",
    timestamp = timestamp,
    version = 1,
    correlationId = correlationId
)