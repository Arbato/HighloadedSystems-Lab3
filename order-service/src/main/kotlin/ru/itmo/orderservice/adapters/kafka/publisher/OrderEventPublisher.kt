// src/main/kotlin/ru/itmo/orderservice/kafka/publisher/OrderEventPublisher.kt
package ru.itmo.orderservice.adapters.kafka.publisher

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import ru.itmo.orderservice.application.dto.response.OrderResponse
import ru.itmo.orderservice.domain.enums.OrderStatus
import java.time.LocalDateTime

@Component
class OrderEventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, Any>,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(OrderEventPublisher::class.java)
    private val topic = "order-events"

    private fun publishEvent(
        event: Map<String, Any?>,
        eventType: String
    ): Mono<Unit> {
        val eventJson = objectMapper.writeValueAsString(event)
        val messageKey = "${eventType}-${event["order_id"] ?: event["user_id"]}"
        
        return Mono.fromFuture(kafkaTemplate.send(topic, messageKey, eventJson))
            .doOnSuccess { 
                logger.info("✅ $eventType published: orderId=${event["order_id"] ?: "userId=${event["user_id"]}"}")
            }
            .doOnError { ex ->
                logger.error("❌ Failed to publish $eventType for orderId=${event["order_id"] ?: event["user_id"]}", ex)
            }
            .then(Mono.just(Unit))
    }

    // 🛒 Cart Events
    fun publishItemAddedToCart(orderId: Long, userId: Long, productId: Long, quantity: Int): Mono<Unit> = 
        publishEvent(
            mapOf(
                "event_type" to "ITEM_ADDED_TO_CART",
                "order_id" to orderId,
                "user_id" to userId,
                "product_id" to productId,
                "quantity" to quantity,
                "timestamp" to LocalDateTime.now().toString()
            ),
            "ITEM_ADDED_TO_CART"
        )

    fun publishCartItemUpdated(orderId: Long, userId: Long, itemId: Long, quantity: Int): Mono<Unit> = 
        publishEvent(
            mapOf(
                "event_type" to "CART_ITEM_UPDATED",
                "order_id" to orderId,
                "user_id" to userId,
                "item_id" to itemId,
                "quantity" to quantity,
                "timestamp" to LocalDateTime.now().toString()
            ),
            "CART_ITEM_UPDATED"
        )

    fun publishItemRemovedFromCart(orderId: Long, userId: Long, itemId: Long): Mono<Unit> = 
        publishEvent(
            mapOf(
                "event_type" to "ITEM_REMOVED_FROM_CART",
                "order_id" to orderId,
                "user_id" to userId,
                "item_id" to itemId,
                "timestamp" to LocalDateTime.now().toString()
            ),
            "ITEM_REMOVED_FROM_CART"
        )

    fun publishCartCleared(userId: Long): Mono<Unit> = 
        publishEvent(
            mapOf(
                "event_type" to "CART_CLEARED",
                "user_id" to userId,
                "timestamp" to LocalDateTime.now().toString()
            ),
            "CART_CLEARED"
        )

    // 🧾 Order Events
    fun publishOrderCreated(order: OrderResponse): Mono<Unit> = 
        publishEvent(
            mapOf(
                "event_type" to "ORDER_CREATED",
                "order_id" to order.id,
                "user_id" to order.userId,
                "total_price" to order.totalPrice.toString(),
                "delivery_address" to order.deliveryAddress,
                "items_count" to order.items.size,
                "timestamp" to LocalDateTime.now().toString()
            ),
            "ORDER_CREATED"
        )
}
