package ru.itmo.notificationservice.adapter.out.messaging.kafka.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import reactor.core.scheduler.Schedulers
import ru.itmo.notificationservice.application.service.NotificationService
import ru.itmo.notificationservice.domain.model.entity.NotificationChannel
import ru.itmo.notificationservice.domain.model.entity.NotificationType

@Component
@ConditionalOnProperty(name = ["spring.kafka.enabled"], havingValue = "true", matchIfMissing = true)
class ModerationEventListener(
    private val notificationService: NotificationService,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = ["moderation-events"],
        groupId = "notification-service-moderation-events"
    )
    fun handleModerationEvent(@Payload eventJson: String) {
        try {
            logger.info("Received moderation event: $eventJson")

            val event = objectMapper.readTree(eventJson)
            val eventType = event.get("event_type")?.asText() ?: return

            when (eventType) {
                "PRODUCT_APPROVED" -> handleProductApproved(event)
                "PRODUCT_REJECTED" -> handleProductRejected(event)
                else -> logger.warn("Unknown moderation event type: $eventType")
            }
        } catch (e: Exception) {
            logger.error("Error processing moderation event", e)
        }
    }

    private fun handleProductApproved(event: com.fasterxml.jackson.databind.JsonNode) {
        val sellerId = event.get("seller_id")?.asLong() ?: return
        val productId = event.get("product_id")?.asLong() ?: return
        val productName = event.get("product_name")?.asText() ?: "Product"

        notificationService.createNotification(
            userId = sellerId,
            type = NotificationType.PRODUCT_APPROVED,
            title = "Product Approved",
            message = "Your product \"$productName\" has been approved and is now visible to customers!",
            channel = NotificationChannel.IN_APP,
            referenceType = "PRODUCT",
            referenceId = productId
        ).subscribeOn(Schedulers.boundedElastic()).subscribe()

        logger.info("Created notification for product approved: productId=$productId, sellerId=$sellerId")
    }

    private fun handleProductRejected(event: com.fasterxml.jackson.databind.JsonNode) {
        val sellerId = event.get("seller_id")?.asLong() ?: return
        val productId = event.get("product_id")?.asLong() ?: return
        val productName = event.get("product_name")?.asText() ?: "Product"
        val reason = event.get("reason")?.asText() ?: "No reason provided"

        notificationService.createNotification(
            userId = sellerId,
            type = NotificationType.PRODUCT_REJECTED,
            title = "Product Rejected",
            message = "Your product \"$productName\" has been rejected. Reason: $reason",
            channel = NotificationChannel.IN_APP,
            referenceType = "PRODUCT",
            referenceId = productId
        ).subscribeOn(Schedulers.boundedElastic()).subscribe()

        logger.info("Created notification for product rejected: productId=$productId, sellerId=$sellerId")
    }
}
