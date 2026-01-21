// src/main/kotlin/ru/itmo/notificationservice/kafka/consumer/OrderNotificationEventConsumer.kt
package ru.itmo.notificationservice.kafka.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import reactor.core.scheduler.Schedulers
import ru.itmo.notificationservice.service.application.OrderNotificationUseCase
import java.time.Duration

@Component
class OrderNotificationEventConsumer(
    private val orderNotificationUseCase: OrderNotificationUseCase,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = ["order-events"],
        groupId = "notification-service-consumer",
        properties = [
            "spring.json.trusted.packages=*",
            "spring.json.use.type.info.headers=false"
        ]
    )
    fun handleOrderEvent(
        @Payload eventJson: String,
        @Header(KafkaHeaders.RECEIVED_PARTITION) partition: Int,
        @Header(KafkaHeaders.OFFSET) offset: Long,
        ack: Acknowledgment?
    ) {
        try {
            logger.info("🛒 Received order event from partition=$partition, offset=$offset")
            
            val event = objectMapper.readValue(eventJson, Map::class.java) as Map<String, Any?>
            val eventType = event["event_type"] as String
            
            when (eventType) {
                "ITEM_ADDED_TO_CART" -> handleItemAddedToCart(event)
                "CART_ITEM_UPDATED" -> handleCartItemUpdated(event)
                "ITEM_REMOVED_FROM_CART" -> handleItemRemovedFromCart(event)
                "CART_CLEARED" -> handleCartCleared(event)
                "ORDER_CREATED" -> handleOrderCreated(event)
            }
            
            ack?.acknowledge()
            logger.info("✅ Successfully processed order event: $eventType")
            
        } catch (e: Exception) {
            logger.error("❌ Error processing order notification event", e)
            ack?.nack(Duration.ofSeconds(1000))
        }
    }

    private fun handleItemAddedToCart(event: Map<String, Any?>) {
        val orderId = (event["order_id"] as Number).toLong()
        val userId = (event["user_id"] as Number).toLong()
        val productId = (event["product_id"] as Number).toLong()
        val quantity = (event["quantity"] as Number).toInt()
        
        orderNotificationUseCase.notifyItemAddedToCart(orderId, userId, productId, quantity)
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe(
                { logger.info("🛒 Item added to cart notification sent: orderId=$orderId") }
            )
    }

    private fun handleCartItemUpdated(event: Map<String, Any?>) {
        val orderId = (event["order_id"] as Number).toLong()
        val userId = (event["user_id"] as Number).toLong()
        val itemId = (event["item_id"] as Number).toLong()
        val quantity = (event["quantity"] as Number).toInt()
        
        orderNotificationUseCase.notifyCartItemUpdated(orderId, userId, itemId, quantity)
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe(
                { logger.info("🔄 Cart item updated notification sent: orderId=$orderId") }
            )
    }

    private fun handleItemRemovedFromCart(event: Map<String, Any?>) {
        val orderId = (event["order_id"] as Number).toLong()
        val userId = (event["user_id"] as Number).toLong()
        val itemId = (event["item_id"] as Number).toLong()
        
        orderNotificationUseCase.notifyItemRemovedFromCart(orderId, userId, itemId)
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe(
                { logger.info("🗑️ Item removed from cart notification sent: orderId=$orderId") }
            )
    }

    private fun handleCartCleared(event: Map<String, Any?>) {
        val userId = (event["user_id"] as Number).toLong()
        
        orderNotificationUseCase.notifyCartCleared(userId)
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe(
                { logger.info("🧹 Cart cleared notification sent: userId=$userId") }
            )
    }

    private fun handleOrderCreated(event: Map<String, Any?>) {
        val orderId = (event["order_id"] as Number).toLong()
        val userId = (event["user_id"] as Number).toLong()
        val totalPrice = event["total_price"] as String
        val itemsCount = (event["items_count"] as Number).toInt()
        
        orderNotificationUseCase.notifyOrderCreated(orderId, userId, totalPrice, itemsCount)
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe(
                { logger.info("📦 Order created notification sent: orderId=$orderId") }
            )
    }
}
