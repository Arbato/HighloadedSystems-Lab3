package ru.itmo.market.kafka.event.notification

import ru.itmo.market.kafka.event.domain.AbstractDomainEvent
import ru.itmo.market.kafka.event.domain.AggregateId
import ru.itmo.market.kafka.event.domain.EventId
import ru.itmo.market.kafka.event.ids.NotificationId
import java.time.LocalDateTime

/**
 * Event: Email notification should be sent
 * Published by: Services requesting notification
 * Consumed by: Notification Service (send email)
 */
data class SendEmailNotificationEvent(
    val notificationId: Long,
    val recipientEmail: String,
    val recipientName: String,
    val templateId: String,  // "order_confirmation", "user_welcome", etc.
    val subject: String,
    val variables: Map<String, String>,
    val priority: String = "NORMAL",  // NORMAL, HIGH, LOW
    val retryCount: Int = 0,
    override val eventId: EventId = EventId.generate(),
    override val aggregateId: AggregateId = NotificationId(notificationId),
    override val timestamp: LocalDateTime = LocalDateTime.now(),
    override val correlationId: String = java.util.UUID.randomUUID().toString()
) : AbstractDomainEvent(
    eventId = eventId,
    aggregateId = aggregateId,
    aggregateType = "notification",
    eventType = "notification.email.send",
    timestamp = timestamp,
    version = 1,
    correlationId = correlationId
)