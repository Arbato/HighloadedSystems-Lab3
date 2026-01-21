// src/main/kotlin/ru/itmo/notificationservice/service/application/OrderNotificationUseCase.kt
package ru.itmo.notificationservice.service.application

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import ru.itmo.notificationservice.model.entity.NotificationEntity
import ru.itmo.notificationservice.repository.NotificationRepository
import java.time.LocalDateTime

@Service
class OrderNotificationUseCase(
    private val notificationRepository: NotificationRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun notifyItemAddedToCart(orderId: Long, userId: Long, productId: Long, quantity: Int): Mono<Void> {
        val notification = NotificationEntity(
            userId = userId,
            eventType = "ITEM_ADDED_TO_CART",
            title = "🛒 Товар добавлен в корзину",
            message = "Товар (ID: $productId) добавлен в корзину (количество: $quantity).",
            isRead = false,
            channel = "IN_APP",
            createdAt = LocalDateTime.now()
        )
        
        return notificationRepository.save(notification)
            .then()
            .doOnError { error -> logger.error("Failed to save item added to cart notification for user $userId", error) }
    }

    fun notifyCartItemUpdated(orderId: Long, userId: Long, itemId: Long, quantity: Int): Mono<Void> {
        val notification = NotificationEntity(
            userId = userId,
            eventType = "CART_ITEM_UPDATED",
            title = "🔄 Корзина обновлена",
            message = "Количество товара (ID: $itemId) в корзине изменено на $quantity.",
            isRead = false,
            channel = "IN_APP",
            createdAt = LocalDateTime.now()
        )
        
        return notificationRepository.save(notification)
            .then()
            .doOnError { error -> logger.error("Failed to save cart item update notification for user $userId", error) }
    }

    fun notifyItemRemovedFromCart(orderId: Long, userId: Long, itemId: Long): Mono<Void> {
        val notification = NotificationEntity(
            userId = userId,
            eventType = "ITEM_REMOVED_FROM_CART",
            title = "🗑️ Товар удален из корзины",
            message = "Товар (ID: $itemId) удален из корзины.",
            isRead = false,
            channel = "IN_APP",
            createdAt = LocalDateTime.now()
        )
        
        return notificationRepository.save(notification)
            .then()
            .doOnError { error -> logger.error("Failed to save item removed from cart notification for user $userId", error) }
    }

    fun notifyCartCleared(userId: Long): Mono<Void> {
        val notification = NotificationEntity(
            userId = userId,
            eventType = "CART_CLEARED",
            title = "🧹 Корзина очищена",
            message = "Ваша корзина была полностью очищена.",
            isRead = false,
            channel = "IN_APP",
            createdAt = LocalDateTime.now()
        )
        
        return notificationRepository.save(notification)
            .then()
            .doOnError { error -> logger.error("Failed to save cart cleared notification for user $userId", error) }
    }

    fun notifyOrderCreated(orderId: Long, userId: Long, totalPrice: String, itemsCount: Int): Mono<Void> {
        val notification = NotificationEntity(
            userId = userId,
            eventType = "ORDER_CREATED",
            title = "📦 Заказ создан",
            message = "Ваш заказ (ID: $orderId) на сумму $totalPrice (${itemsCount} товаров) успешно создан и ожидает обработки.",
            isRead = false,
            channel = "IN_APP",
            createdAt = LocalDateTime.now()
        )
        
        return notificationRepository.save(notification)
            .then()
            .doOnError { error -> logger.error("Failed to save order created notification for user $userId", error) }
    }
}
