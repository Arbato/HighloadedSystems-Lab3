package ru.itmo.orderservice.adapter.out.messaging.kafka.producer

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
@ConditionalOnProperty(name = ["spring.kafka.enabled"], havingValue = "true", matchIfMissing = true)
class OrderEventProducer(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val ORDER_EVENTS_TOPIC = "order-events"
    }

    fun publishOrderCreated(orderId: Long, userId: Long, totalPrice: BigDecimal, itemsCount: Int) {
        val event = mapOf(
            "event_type" to "ORDER_CREATED",
            "order_id" to orderId,
            "user_id" to userId,
            "total_price" to totalPrice.toString(),
            "items_count" to itemsCount,
            "timestamp" to System.currentTimeMillis()
        )
        publishEvent(event, "ORDER_CREATED")
    }

    fun publishOrderShipped(orderId: Long, userId: Long) {
        val event = mapOf(
            "event_type" to "ORDER_SHIPPED",
            "order_id" to orderId,
            "user_id" to userId,
            "timestamp" to System.currentTimeMillis()
        )
        publishEvent(event, "ORDER_SHIPPED")
    }

    fun publishOrderDelivered(orderId: Long, userId: Long) {
        val event = mapOf(
            "event_type" to "ORDER_DELIVERED",
            "order_id" to orderId,
            "user_id" to userId,
            "timestamp" to System.currentTimeMillis()
        )
        publishEvent(event, "ORDER_DELIVERED")
    }

    fun publishOrderCancelled(orderId: Long, userId: Long) {
        val event = mapOf(
            "event_type" to "ORDER_CANCELLED",
            "order_id" to orderId,
            "user_id" to userId,
            "timestamp" to System.currentTimeMillis()
        )
        publishEvent(event, "ORDER_CANCELLED")
    }

    private fun publishEvent(event: Map<String, Any>, eventType: String) {
        try {
            val eventJson = objectMapper.writeValueAsString(event)
            kafkaTemplate.send(ORDER_EVENTS_TOPIC, eventJson)
            logger.info("Published $eventType event: $eventJson")
        } catch (e: Exception) {
            logger.error("Failed to publish $eventType event", e)
        }
    }
}
