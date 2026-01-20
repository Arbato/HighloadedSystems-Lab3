package ru.itmo.orderservice.service

import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.itmo.orderservice.kafka.client.KafkaRpcClient
import ru.itmo.orderservice.exception.BadRequestException
import ru.itmo.orderservice.exception.ResourceNotFoundException
import ru.itmo.orderservice.model.dto.request.CreateOrderRequest
import ru.itmo.orderservice.model.dto.response.OrderResponse
import ru.itmo.orderservice.model.dto.response.OrderItemResponse
import ru.itmo.orderservice.model.dto.response.PaginatedResponse
import ru.itmo.orderservice.model.entity.Order
import ru.itmo.orderservice.model.entity.OrderItem
import ru.itmo.orderservice.model.enums.OrderStatus
import ru.itmo.orderservice.repository.OrderRepository
import ru.itmo.orderservice.repository.OrderItemRepository
import java.math.BigDecimal
import java.time.LocalDateTime

@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository,
    private val kafkaRpcClient: KafkaRpcClient
) {
    
    /**
     * Получить корзину пользователя
     */
    fun getCart(userId: Long): OrderResponse {
        if (userId <= 0) {
            throw BadRequestException("Invalid user ID: $userId")
        }
        
        val cart = orderRepository.findByUserIdAndStatus(userId, OrderStatus.CART)
            .orElseGet {
                Order(userId = userId, status = OrderStatus.CART)
            }
        
        // Если это новая корзина, сохраняем её
        if (cart.id == 0L) {
            orderRepository.save(cart)
        }
        
        return cart.toResponse(orderItemRepository, kafkaRpcClient)
    }
    
    /**
     * Добавить товар в корзину
     */
    @Transactional
    fun addToCart(userId: Long, productId: Long, quantity: Int): OrderResponse {
        if (userId <= 0 || productId <= 0) {
            throw BadRequestException("Invalid user ID or product ID")
        }
        
        if (quantity < 1) {
            throw BadRequestException("Quantity must be at least 1")
        }
        
        // Проверяем что товар существует
        val product = kafkaRpcClient.getProductById(productId)
        
        // Получаем или создаем корзину
        var cart = orderRepository.findByUserIdAndStatus(userId, OrderStatus.CART)
            .orElseGet { Order(userId = userId, status = OrderStatus.CART) }
        
        if (cart.id == 0L) {
            cart = orderRepository.save(cart)
        }
        
        // Ищем товар в корзине
        val existingItem = orderItemRepository.findByOrderIdAndProductId(cart.id, productId)
        
        if (existingItem.isPresent) {
            // Увеличиваем количество
            val item = existingItem.get()
            val updatedItem = item.copy(quantity = item.quantity + quantity)
            orderItemRepository.save(updatedItem)
        } else {
            // Добавляем новый товар
            val newItem = OrderItem(
                orderId = cart.id,
                productId = productId,
                quantity = quantity,
                price = product.price
            )
            orderItemRepository.save(newItem)
        }
        
        // Пересчитываем итоговую цену
        val items = orderItemRepository.findAllByOrderId(cart.id)
        val newTotal = items.fold(BigDecimal.ZERO) { acc, item ->
            acc + (item.price * BigDecimal(item.quantity))
        }
        
        val updatedCart = cart.copy(totalPrice = newTotal)
        orderRepository.save(updatedCart)
        
        return updatedCart.toResponse(orderItemRepository, kafkaRpcClient)
    }
    
    /**
     * Обновить количество товара в корзине
     */
    @Transactional
    fun updateCartItemQuantity(userId: Long, itemId: Long, quantity: Int): OrderResponse {
        if (userId <= 0 || itemId <= 0) {
            throw BadRequestException("Invalid user ID or item ID")
        }
        
        if (quantity < 0) {
            throw BadRequestException("Quantity must be non-negative")
        }
        
        // Получаем корзину
        val cart = orderRepository.findByUserIdAndStatus(userId, OrderStatus.CART)
            .orElseThrow { ResourceNotFoundException("Cart not found for user: $userId") }
        
        // Получаем товар
        val item = orderItemRepository.findById(itemId)
            .orElseThrow { ResourceNotFoundException("Item not found: $itemId") }
        
        // Проверяем что товар в нужной корзине
        if (item.orderId != cart.id) {
            throw BadRequestException("Item does not belong to user's cart")
        }
        
        // Если количество 0, удаляем товар
        if (quantity == 0) {
            orderItemRepository.deleteById(itemId)
        } else {
            val updatedItem = item.copy(quantity = quantity)
            orderItemRepository.save(updatedItem)
        }
        
        // Пересчитываем итоговую цену
        val items = orderItemRepository.findAllByOrderId(cart.id)
        val newTotal = items.fold(BigDecimal.ZERO) { acc, orderItem ->
            acc + (orderItem.price * BigDecimal(orderItem.quantity))
        }
        
        val updatedCart = cart.copy(totalPrice = newTotal)
        orderRepository.save(updatedCart)
        
        return updatedCart.toResponse(orderItemRepository, kafkaRpcClient)
    }
    
    /**
     * Удалить товар из корзины
     */
    @Transactional
    fun removeFromCart(userId: Long, itemId: Long): OrderResponse {
        if (userId <= 0 || itemId <= 0) {
            throw BadRequestException("Invalid user ID or item ID")
        }
        
        // Получаем корзину
        val cart = orderRepository.findByUserIdAndStatus(userId, OrderStatus.CART)
            .orElseThrow { ResourceNotFoundException("Cart not found for user: $userId") }
        
        // Получаем товар
        val item = orderItemRepository.findById(itemId)
            .orElseThrow { ResourceNotFoundException("Item not found: $itemId") }
        
        // Проверяем что товар в нужной корзине
        if (item.orderId != cart.id) {
            throw BadRequestException("Item does not belong to user's cart")
        }
        
        orderItemRepository.deleteById(itemId)
        
        // Пересчитываем итоговую цену
        val items = orderItemRepository.findAllByOrderId(cart.id)
        val newTotal = items.fold(BigDecimal.ZERO) { acc, orderItem ->
            acc + (orderItem.price * BigDecimal(orderItem.quantity))
        }
        
        val updatedCart = cart.copy(totalPrice = newTotal)
        orderRepository.save(updatedCart)
        
        return updatedCart.toResponse(orderItemRepository, kafkaRpcClient)
    }
    
    /**
     * Очистить корзину
     */
    @Transactional
    fun clearCart(userId: Long) {
        if (userId <= 0) {
            throw BadRequestException("Invalid user ID: $userId")
        }
        
        val cart = orderRepository.findByUserIdAndStatus(userId, OrderStatus.CART)
            .orElseThrow { ResourceNotFoundException("Cart not found for user: $userId") }
        
        orderItemRepository.deleteByOrderId(cart.id)
        
        val clearedCart = cart.copy(totalPrice = BigDecimal.ZERO)
        orderRepository.save(clearedCart)
    }
    
    /**
     * Оформить заказ (конвертировать корзину в заказ)
     */
    @Transactional
    fun createOrder(userId: Long, request: CreateOrderRequest): OrderResponse {
        if (userId <= 0) {
            throw BadRequestException("Invalid user ID: $userId")
        }
        
        if (request.deliveryAddress.isBlank()) {
            throw BadRequestException("Delivery address is required")
        }
        
        // Получаем корзину
        val cart = orderRepository.findByUserIdAndStatus(userId, OrderStatus.CART)
            .orElseThrow { ResourceNotFoundException("Cart not found for user: $userId") }
        
        // Проверяем что корзина не пуста
        val items = orderItemRepository.findAllByOrderId(cart.id)
        if (items.isEmpty()) {
            throw BadRequestException("Cannot create order from empty cart")
        }
        
        // Конвертируем корзину в заказ
        val order = cart.copy(
            status = OrderStatus.PENDING,
            deliveryAddress = request.deliveryAddress
        )
        
        val savedOrder = orderRepository.save(order)
        
        // Создаем новую пустую корзину
        val newCart = Order(userId = userId, status = OrderStatus.CART)
        orderRepository.save(newCart)
        
        return savedOrder.toResponse(orderItemRepository, kafkaRpcClient)
    }
    
    /**
     * Получить заказ по ID
     */
    fun getOrderById(orderId: Long, userId: Long): OrderResponse {
        if (orderId <= 0 || userId <= 0) {
            throw BadRequestException("Invalid order ID or user ID")
        }
        
        val order = orderRepository.findByIdAndUserId(orderId, userId)
            .orElseThrow { ResourceNotFoundException("Order not found: $orderId") }
        
        return order.toResponse(orderItemRepository, kafkaRpcClient)
    }
    
    /**
     * Получить все заказы пользователя (кроме корзины)
     */
    fun getUserOrders(userId: Long, page: Int, pageSize: Int): PaginatedResponse<OrderResponse> {
        if (userId <= 0) {
            throw BadRequestException("Invalid user ID: $userId")
        }
        
        if (page < 1 || pageSize < 1) {
            throw BadRequestException("Page and pageSize must be greater than 0")
        }
        
        val pageable = PageRequest.of(page - 1, pageSize)
        val orderPage = orderRepository.findAllByUserIdAndStatusNot(userId, OrderStatus.CART, pageable)
        
        return PaginatedResponse(
            data = orderPage.content.map { it.toResponse(orderItemRepository, kafkaRpcClient) },
            page = page,
            pageSize = pageSize,
            totalElements = orderPage.totalElements,
            totalPages = orderPage.totalPages
        )
    }
    
    /**
     * Вспомогательный метод для преобразования Order в OrderResponse
     */
    private fun Order.toResponse(
        orderItemRepository: OrderItemRepository,
        kafkaRpcClient: KafkaRpcClient
    ): OrderResponse {
        val items = orderItemRepository.findAllByOrderId(this.id)
            .map { item ->
                try {
                    val product = kafkaRpcClient.getProductById(item.productId)
                    OrderItemResponse(
                        id = item.id,
                        productId = item.productId,
                        productName = product.name,
                        productPrice = product.price,
                        quantity = item.quantity,
                        price = item.price,
                        subtotal = item.price * BigDecimal(item.quantity),
                        createdAt = item.createdAt
                    )
                } catch (e: Exception) {
                    // Fallback если товар не найден
                    OrderItemResponse(
                        id = item.id,
                        productId = item.productId,
                        productName = "Product (unavailable)",
                        productPrice = item.price,
                        quantity = item.quantity,
                        price = item.price,
                        subtotal = item.price * BigDecimal(item.quantity),
                        createdAt = item.createdAt
                    )
                }
            }
        
        return OrderResponse(
            id = this.id,
            userId = this.userId,
            items = items,
            totalPrice = this.totalPrice,
            status = this.status.name,
            deliveryAddress = this.deliveryAddress,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }
}
