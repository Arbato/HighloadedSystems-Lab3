package ru.itmo.market.kafka.event.notification

import ru.itmo.market.kafka.event.domain.AbstractDomainEvent
import ru.itmo.market.kafka.event.domain.AggregateId
import ru.itmo.market.kafka.event.domain.EventId
import ru.itmo.market.kafka.event.ids.NotificationId
import java.time.LocalDateTime

/**
 * Event: SMS notification should be sent
 * Published by: Services requesting notification
 * Consumed by: Notification Service (send SMS)
 */
data class SendSmsNotificationEvent(
    val notificationId: Long,
    val recipientPhone: String,
    val message: String,
    val templateId: String? = null,
    val variables: Map<String, String> = emptyMap(),
    val priority: String = "NORMAL",
    override val eventId: EventId = EventId.generate(),
    override val aggregateId: AggregateId = NotificationId(notificationId),
    override val timestamp: LocalDateTime = LocalDateTime.now(),
    override val correlationId: String = java.util.UUID.randomUUID().toString()
) : AbstractDomainEvent(
    eventId = eventId,
    aggregateId = aggregateId,
    aggregateType = "notification",
    eventType = "notification.sms.send",
    timestamp = timestamp,
    version = 1,
    correlationId = correlationId
)