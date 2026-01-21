// src/main/kotlin/ru/itmo/notificationservice/service/application/ProductNotificationUseCase.kt
package ru.itmo.notificationservice.service.application

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import ru.itmo.notificationservice.model.entity.NotificationEntity
import ru.itmo.notificationservice.repository.NotificationRepository
import java.time.LocalDateTime

@Service
class ProductNotificationUseCase(
    private val notificationRepository: NotificationRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun notifyProductCreated(productId: Long, sellerId: Long, productName: String): Mono<Void> {
        val notification = NotificationEntity(
            userId = sellerId,
            eventType = "PRODUCT_CREATED",
            title = "🆕 Товар создан",
            message = "Ваш товар \"$productName\" (ID: $productId) успешно создан и ожидает модерации.",
            isRead = false,
            channel = "IN_APP",
            createdAt = LocalDateTime.now()
        )
        
        return notificationRepository.save(notification)
            .then()
            .doOnError { error -> logger.error("Failed to save product creation notification for seller $sellerId", error) }
    }

    fun notifyProductDeleted(productId: Long, sellerId: Long): Mono<Void> {
        val notification = NotificationEntity(
            userId = sellerId,
            eventType = "PRODUCT_DELETED",
            title = "🗑️ Товар удален",
            message = "Ваш товар (ID: $productId) был удален.",
            isRead = false,
            channel = "IN_APP",
            createdAt = LocalDateTime.now()
        )
        
        return notificationRepository.save(notification)
            .then()
            .doOnError { error -> logger.error("Failed to save product deletion notification for seller $sellerId", error) }
    }

    fun notifyShopCreated(shopId: Long, sellerId: Long): Mono<Void> {
        val notification = NotificationEntity(
            userId = sellerId,
            eventType = "SHOP_CREATED",
            title = "🏪 Магазин создан",
            message = "Ваш магазин (ID: $shopId) успешно создан. Теперь вы можете добавлять товары!",
            isRead = false,
            channel = "IN_APP",
            createdAt = LocalDateTime.now()
        )
        
        return notificationRepository.save(notification)
            .then()
            .doOnError { error -> logger.error("Failed to save shop creation notification for seller $sellerId", error) }
    }

    fun notifyShopUpdated(shopId: Long, sellerId: Long): Mono<Void> {
        val notification = NotificationEntity(
            userId = sellerId,
            eventType = "SHOP_UPDATED",
            title = "🔄 Магазин обновлен",
            message = "Ваш магазин (ID: $shopId) успешно обновлен.",
            isRead = false,
            channel = "IN_APP",
            createdAt = LocalDateTime.now()
        )
        
        return notificationRepository.save(notification)
            .then()
            .doOnError { error -> logger.error("Failed to save shop update notification for seller $sellerId", error) }
    }
}
