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
class OrderEventListener(
    private val notificationService: NotificationService,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = ["order-events"],
        groupId = "notification-service-order-events"
    )
    fun handleOrderEvent(@Payload eventJson: String) {
        try {
            logger.info("Received order event: $eventJson")

            val event = objectMapper.readTree(eventJson)
            val eventType = event.get("event_type")?.asText() ?: return

            when (eventType) {
                "ORDER_CREATED" -> handleOrderCreated(event)
                "ORDER_SHIPPED" -> handleOrderShipped(event)
                "ORDER_DELIVERED" -> handleOrderDelivered(event)
                "ORDER_CANCELLED" -> handleOrderCancelled(event)
                else -> logger.warn("Unknown order event type: $eventType")
            }
        } catch (e: Exception) {
            logger.error("Error processing order event", e)
        }
    }

    private fun handleOrderCreated(event: com.fasterxml.jackson.databind.JsonNode) {
        val userId = event.get("user_id")?.asLong() ?: return
        val orderId = event.get("order_id")?.asLong() ?: return
        val totalPrice = event.get("total_price")?.asText() ?: "0"

        notificationService.createNotification(
            userId = userId,
            type = NotificationType.ORDER_CREATED,
            title = "Order Created",
            message = "Your order #$orderId has been created. Total: $totalPrice",
            channel = NotificationChannel.IN_APP,
            referenceType = "ORDER",
            referenceId = orderId
        ).subscribeOn(Schedulers.boundedElastic()).subscribe()

        logger.info("Created notification for order created: orderId=$orderId, userId=$userId")
    }

    private fun handleOrderShipped(event: com.fasterxml.jackson.databind.JsonNode) {
        val userId = event.get("user_id")?.asLong() ?: return
        val orderId = event.get("order_id")?.asLong() ?: return

        notificationService.createNotification(
            userId = userId,
            type = NotificationType.ORDER_SHIPPED,
            title = "Order Shipped",
            message = "Your order #$orderId has been shipped!",
            channel = NotificationChannel.IN_APP,
            referenceType = "ORDER",
            referenceId = orderId
        ).subscribeOn(Schedulers.boundedElastic()).subscribe()

        logger.info("Created notification for order shipped: orderId=$orderId, userId=$userId")
    }

    private fun handleOrderDelivered(event: com.fasterxml.jackson.databind.JsonNode) {
        val userId = event.get("user_id")?.asLong() ?: return
        val orderId = event.get("order_id")?.asLong() ?: return

        notificationService.createNotification(
            userId = userId,
            type = NotificationType.ORDER_DELIVERED,
            title = "Order Delivered",
            message = "Your order #$orderId has been delivered!",
            channel = NotificationChannel.IN_APP,
            referenceType = "ORDER",
            referenceId = orderId
        ).subscribeOn(Schedulers.boundedElastic()).subscribe()

        logger.info("Created notification for order delivered: orderId=$orderId, userId=$userId")
    }

    private fun handleOrderCancelled(event: com.fasterxml.jackson.databind.JsonNode) {
        val userId = event.get("user_id")?.asLong() ?: return
        val orderId = event.get("order_id")?.asLong() ?: return

        notificationService.createNotification(
            userId = userId,
            type = NotificationType.ORDER_CANCELLED,
            title = "Order Cancelled",
            message = "Your order #$orderId has been cancelled.",
            channel = NotificationChannel.IN_APP,
            referenceType = "ORDER",
            referenceId = orderId
        ).subscribeOn(Schedulers.boundedElastic()).subscribe()

        logger.info("Created notification for order cancelled: orderId=$orderId, userId=$userId")
    }
}
