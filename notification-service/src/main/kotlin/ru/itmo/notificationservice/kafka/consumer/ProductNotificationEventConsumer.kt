// src/main/kotlin/ru/itmo/notificationservice/kafka/consumer/ProductNotificationEventConsumer.kt
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
import ru.itmo.notificationservice.service.application.ProductNotificationUseCase
import java.time.Duration

@Component
class ProductNotificationEventConsumer(
    private val productNotificationUseCase: ProductNotificationUseCase,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = ["product-events"],
        groupId = "notification-service-consumer",
        properties = [
            "spring.json.trusted.packages=*",
            "spring.json.use.type.info.headers=false"
        ]
    )
    fun handleProductEvent(
        @Payload eventJson: String,
        @Header(KafkaHeaders.RECEIVED_PARTITION) partition: Int,
        @Header(KafkaHeaders.OFFSET) offset: Long,
        ack: Acknowledgment?
    ) {
        try {
            logger.info("📦 Received product event from partition=$partition, offset=$offset")
            
            val event = objectMapper.readValue(eventJson, Map::class.java) as Map<String, Any?>
            val eventType = event["event_type"] as String
            
            when (eventType) {
                "PRODUCT_CREATED" -> handleProductCreated(event)
                "PRODUCT_DELETED" -> handleProductDeleted(event)
                "SHOP_CREATED" -> handleShopCreated(event)
                "SHOP_UPDATED" -> handleShopUpdated(event)
            }
            
            ack?.acknowledge()
            logger.info("✅ Successfully processed product event: $eventType")
            
        } catch (e: Exception) {
            logger.error("❌ Error processing product notification event", e)
            ack?.nack(Duration.ofSeconds(1000))
        }
    }

    private fun handleProductCreated(event: Map<String, Any?>) {
        val productId = (event["product_id"] as Number).toLong()
        val sellerId = (event["seller_id"] as Number).toLong()
        
        productNotificationUseCase.notifyProductCreated(productId, sellerId, event["product_name"] as String)
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe(
                { logger.info("🆕 Product creation notification sent: productId=$productId") }
            )
    }

    private fun handleProductDeleted(event: Map<String, Any?>) {
        val productId = (event["product_id"] as Number).toLong()
        val sellerId = (event["seller_id"] as Number).toLong()
        
        productNotificationUseCase.notifyProductDeleted(productId, sellerId)
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe(
                { logger.info("🗑️ Product deletion notification sent: productId=$productId") }
            )
    }

    private fun handleShopCreated(event: Map<String, Any?>) {
        val shopId = (event["shop_id"] as Number).toLong()
        val sellerId = (event["seller_id"] as Number).toLong()
        
        productNotificationUseCase.notifyShopCreated(shopId, sellerId)
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe(
                { logger.info("🏪 Shop creation notification sent: shopId=$shopId") }
            )
    }

    private fun handleShopUpdated(event: Map<String, Any?>) {
        val shopId = (event["shop_id"] as Number).toLong()
        val sellerId = (event["seller_id"] as Number).toLong()
        
        productNotificationUseCase.notifyShopUpdated(shopId, sellerId)
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe(
                { logger.info("🔄 Shop update notification sent: shopId=$shopId") }
            )
    }
}
