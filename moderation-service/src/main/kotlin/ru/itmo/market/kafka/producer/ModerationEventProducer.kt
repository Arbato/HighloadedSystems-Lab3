package ru.itmo.market.kafka.producer

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["spring.kafka.enabled"], havingValue = "true", matchIfMissing = true)
class ModerationEventProducer(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val MODERATION_EVENTS_TOPIC = "moderation-events"
    }

    fun publishProductApproved(productId: Long, productName: String, sellerId: Long, moderatorId: Long) {
        val event = mapOf(
            "event_type" to "PRODUCT_APPROVED",
            "product_id" to productId,
            "product_name" to productName,
            "seller_id" to sellerId,
            "moderator_id" to moderatorId,
            "timestamp" to System.currentTimeMillis()
        )
        publishEvent(event, "PRODUCT_APPROVED")
    }

    fun publishProductRejected(productId: Long, productName: String, sellerId: Long, moderatorId: Long, reason: String) {
        val event = mapOf(
            "event_type" to "PRODUCT_REJECTED",
            "product_id" to productId,
            "product_name" to productName,
            "seller_id" to sellerId,
            "moderator_id" to moderatorId,
            "reason" to reason,
            "timestamp" to System.currentTimeMillis()
        )
        publishEvent(event, "PRODUCT_REJECTED")
    }

    private fun publishEvent(event: Map<String, Any>, eventType: String) {
        try {
            val eventJson = objectMapper.writeValueAsString(event)
            kafkaTemplate.send(MODERATION_EVENTS_TOPIC, eventJson)
            logger.info("Published $eventType event: $eventJson")
        } catch (e: Exception) {
            logger.error("Failed to publish $eventType event", e)
        }
    }
}
