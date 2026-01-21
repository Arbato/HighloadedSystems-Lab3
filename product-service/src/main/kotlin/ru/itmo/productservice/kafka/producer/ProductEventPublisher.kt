// src/main/kotlin/ru/itmo/productservice/kafka/publisher/ProductEventPublisher.kt
package ru.itmo.productservice.kafka.publisher

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import ru.itmo.productservice.model.dto.response.ProductResponse
import ru.itmo.productservice.model.dto.response.ShopResponse
import ru.itmo.productservice.model.enums.ProductStatus
import ru.itmo.productservice.model.enums.UserRole
import java.math.BigDecimal
import java.time.LocalDateTime

@Component
class ProductEventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, Any>,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(ProductEventPublisher::class.java)
    private val topic = "product-events"

    private fun publishEvent(
        event: Map<String, Any?>,
        eventType: String
    ): Mono<Unit> {
        val eventJson = objectMapper.writeValueAsString(event)
        val messageKey = "${eventType}-${event["product_id"] ?: event["shop_id"] ?: "no-id"}"
        
        return Mono.fromFuture(kafkaTemplate.send(topic, messageKey, eventJson))
            .doOnSuccess { 
                logger.info("✅ $eventType published: ${event["product_id"] ?: event["shop_id"]}")
            }
            .doOnError { ex ->
                logger.error("❌ Failed to publish $eventType: ${event["product_id"] ?: event["shop_id"]}", ex)
            }
            .then(Mono.just(Unit))
    }

    // 📦 Product Events
    fun publishProductCreated(product: ProductResponse): Mono<Unit> = publishEvent(
        mapOf(
            "event_type" to "PRODUCT_CREATED",
            "product_id" to product.id,
            "product_name" to product.name,
            "price" to product.price.toString(),
            "shop_id" to product.shopId,
            "seller_id" to product.sellerId,
            "status" to product.status,
            "timestamp" to LocalDateTime.now().toString()
        ),
        "PRODUCT_CREATED"
    )

    fun publishProductUpdated(productId: Long, oldStatus: String, newStatus: String): Mono<Unit> = 
        publishEvent(
            mapOf(
                "event_type" to "PRODUCT_UPDATED",
                "product_id" to productId,
                "old_status" to oldStatus,
                "new_status" to newStatus,
                "timestamp" to LocalDateTime.now().toString()
            ),
            "PRODUCT_UPDATED"
        )

    fun publishProductApproved(productId: Long, productName: String, shopId: Long, sellerId: Long, moderatorId: Long): Mono<Unit> = 
        publishEvent(
            mapOf(
                "event_type" to "PRODUCT_APPROVED",
                "product_id" to productId,
                "product_name" to productName,
                "shop_id" to shopId,
                "seller_id" to sellerId,
                "moderator_id" to moderatorId,
                "timestamp" to LocalDateTime.now().toString()
            ),
            "PRODUCT_APPROVED"
        )

    fun publishProductRejected(
        productId: Long, 
        productName: String, 
        shopId: Long, 
        sellerId: Long, 
        reason: String, 
        moderatorId: Long
    ): Mono<Unit> = publishEvent(
        mapOf(
            "event_type" to "PRODUCT_REJECTED",
            "product_id" to productId,
            "product_name" to productName,
            "shop_id" to shopId,
            "seller_id" to sellerId,
            "reason" to reason,
            "moderator_id" to moderatorId,
            "timestamp" to LocalDateTime.now().toString()
        ),
        "PRODUCT_REJECTED"
    )

    fun publishProductDeleted(productId: Long, sellerId: Long, moderatorId: Long? = null): Mono<Unit> = 
        publishEvent(
            mapOf(
                "event_type" to "PRODUCT_DELETED",
                "product_id" to productId,
                "seller_id" to sellerId,
                "moderator_id" to moderatorId,
                "timestamp" to LocalDateTime.now().toString()
            ),
            "PRODUCT_DELETED"
        )

    // 🏪 Shop Events
    fun publishShopCreated(shop: ShopResponse): Mono<Unit> = publishEvent(
        mapOf(
            "event_type" to "SHOP_CREATED",
            "shop_id" to shop.id,
            "shop_name" to shop.name,
            "seller_id" to shop.sellerId,
            "seller_name" to shop.sellerName,
            "products_count" to shop.productsCount,
            "timestamp" to LocalDateTime.now().toString()
        ),
        "SHOP_CREATED"
    )

    fun publishShopUpdated(shopId: Long, shopName: String, sellerId: Long): Mono<Unit> = 
        publishEvent(
            mapOf(
                "event_type" to "SHOP_UPDATED",
                "shop_id" to shopId,
                "shop_name" to shopName,
                "seller_id" to sellerId,
                "timestamp" to LocalDateTime.now().toString()
            ),
            "SHOP_UPDATED"
        )
}
