package ru.itmo.orderservice.service

import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.slf4j.LoggerFactory
import ru.itmo.orderservice.client.KafkaRpcClient
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
import ru.itmo.orderservice.kafka.publisher.OrderEventPublisher
import java.math.BigDecimal
import java.time.LocalDateTime

@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository,
    private val kafkaRpcClient: KafkaRpcClient,
    private val orderEventPublisher: OrderEventPublisher
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private fun validateUser(userId: Long) {
        if (userId <= 0) {
            throw BadRequestException("Invalid user ID: $userId")
        }
        
        try {
            logger.debug("Validating user: $userId")
            val user = kafkaRpcClient.getUserById(userId)
            logger.debug("User $userId validated successfully")
        } catch (e: ResourceNotFoundException) {
            throw e
        } catch (e: Exception) {
            logger.error("Error validating user $userId", e)
            throw e
        }
    }

    
    fun getCart(userId: Long): OrderResponse {
        validateUser(userId)
        
        val cart = orderRepository.findByUserIdAndStatus(userId, OrderStatus.CART)
            .orElseGet {
                Order(userId = userId, status = OrderStatus.CART)
            }
        
        if (cart.id == 0L) {
            orderRepository.save(cart)
        }
        
        return cart.toResponse(orderItemRepository, kafkaRpcClient)
    }
    
    @Transactional
    fun addToCart(userId: Long, productId: Long, quantity: Int): OrderResponse {
        
        validateUser(userId)
        
        if (quantity < 1) {
            throw BadRequestException("Quantity must be at least 1")
        }
        
        logger.info("Adding product $productId to cart for user $userId (quantity: $quantity)")
        
        val product = kafkaRpcClient.getProductById(productId)
        
        var cart = orderRepository.findByUserIdAndStatus(userId, OrderStatus.CART)
            .orElseGet {
                Order(userId = userId, status = OrderStatus.CART)
            }
        
        if (cart.id == 0L) {
            cart = orderRepository.save(cart)
        }
        
        val existingItem = orderItemRepository.findByOrderIdAndProductId(cart.id, productId)
        
        if (existingItem.isPresent) {
            val item = existingItem.get()
            val updatedItem = item.copy(quantity = item.quantity + quantity)
            orderItemRepository.save(updatedItem)
            orderEventPublisher.publishCartItemUpdated(cart.id, userId, existingItem.get().id, updatedItem.quantity)
            logger.debug("Updated existing cart item: $productId (new quantity: ${updatedItem.quantity})")
        } else {
            val newItem = OrderItem(
                orderId = cart.id,
                productId = productId,
                quantity = quantity,
                price = product.price
            )
            orderItemRepository.save(newItem)
            logger.debug("Added new cart item: $productId (quantity: $quantity)")
        }
        
        val items = orderItemRepository.findAllByOrderId(cart.id)
        val newTotal = items.fold(BigDecimal.ZERO) { acc, item ->
            acc + (item.price * BigDecimal(item.quantity))
        }
        
        val updatedCart = cart.copy(totalPrice = newTotal)
        orderRepository.save(updatedCart)
        
        logger.info("Cart updated for user $userId. New total: $newTotal")
        return updatedCart.toResponse(orderItemRepository, kafkaRpcClient)
    }
    
    @Transactional
    fun updateCartItemQuantity(userId: Long, itemId: Long, quantity: Int): OrderResponse {
        
        validateUser(userId)
        
        if (quantity < 0) {
            throw BadRequestException("Quantity must be non-negative")
        }
        
        logger.info("Updating cart item $itemId for user $userId to quantity $quantity")
        
        val cart = orderRepository.findByUserIdAndStatus(userId, OrderStatus.CART)
            .orElseThrow { ResourceNotFoundException("Cart not found for user $userId") }
        
        val item = orderItemRepository.findById(itemId)
            .orElseThrow { ResourceNotFoundException("Item not found: $itemId") }
        
        if (item.orderId != cart.id) {
            throw BadRequestException("Item does not belong to user's cart")
        }
        
        if (quantity == 0) {
            orderItemRepository.deleteById(itemId)
            logger.debug("Removed cart item: $itemId")
        } else {
            val updatedItem = item.copy(quantity = quantity)
            orderItemRepository.save(updatedItem)
            logger.debug("Updated cart item: $itemId (new quantity: $quantity)")
        }
        
        val items = orderItemRepository.findAllByOrderId(cart.id)
        val newTotal = items.fold(BigDecimal.ZERO) { acc, orderItem ->
            acc + (orderItem.price * BigDecimal(orderItem.quantity))
        }
        
        val updatedCart = cart.copy(totalPrice = newTotal)
        orderRepository.save(updatedCart)
        
        logger.info("Cart item updated for user $userId")
        orderEventPublisher.publishCartItemUpdated(cart.id, userId, itemId, quantity)
        return updatedCart.toResponse(orderItemRepository, kafkaRpcClient)
    }
    
    @Transactional
    fun removeFromCart(userId: Long, itemId: Long): OrderResponse {
       
        validateUser(userId)
        
        logger.info("Removing cart item $itemId for user $userId")
        
        val cart = orderRepository.findByUserIdAndStatus(userId, OrderStatus.CART)
            .orElseThrow { ResourceNotFoundException("Cart not found for user $userId") }
        
        val item = orderItemRepository.findById(itemId)
            .orElseThrow { ResourceNotFoundException("Item not found: $itemId") }
        
        if (item.orderId != cart.id) {
            throw BadRequestException("Item does not belong to user's cart")
        }
        
        orderItemRepository.deleteById(itemId)
        logger.debug("Deleted cart item: $itemId")
        
        val items = orderItemRepository.findAllByOrderId(cart.id)
        val newTotal = items.fold(BigDecimal.ZERO) { acc, orderItem ->
            acc + (orderItem.price * BigDecimal(orderItem.quantity))
        }
        
        val updatedCart = cart.copy(totalPrice = newTotal)
        orderRepository.save(updatedCart)
        
        logger.info("Cart item removed for user $userId")
        orderEventPublisher.publishItemRemovedFromCart(cart.id!!, userId, itemId)
        return updatedCart.toResponse(orderItemRepository, kafkaRpcClient)
    }
    
    @Transactional
    fun clearCart(userId: Long) {

        validateUser(userId)
        
        logger.info("Clearing cart for user $userId")
        
        val cart = orderRepository.findByUserIdAndStatus(userId, OrderStatus.CART)
            .orElseThrow { ResourceNotFoundException("Cart not found for user $userId") }
        
        orderItemRepository.deleteByOrderId(cart.id)
        
        val clearedCart = cart.copy(totalPrice = BigDecimal.ZERO)
        orderRepository.save(clearedCart)
        
        logger.info("Cart cleared for user $userId")
        orderEventPublisher.publishCartCleared(userId)
    }
    
    @Transactional
    fun createOrder(userId: Long, request: CreateOrderRequest): OrderResponse {
        
        validateUser(userId)
        
        if (request.deliveryAddress.isBlank()) {
            throw BadRequestException("Delivery address is required")
        }
        
        logger.info("Creating order for user $userId with delivery address: ${request.deliveryAddress}")
        
        val cart = orderRepository.findByUserIdAndStatus(userId, OrderStatus.CART)
            .orElseThrow { ResourceNotFoundException("Cart not found for user $userId") }
        
        val items = orderItemRepository.findAllByOrderId(cart.id)
        
        if (items.isEmpty()) {
            throw BadRequestException("Cannot create order from empty cart")
        }
        
        val order = cart.copy(
            status = OrderStatus.PENDING,
            deliveryAddress = request.deliveryAddress
        )
        
        val savedOrder = orderRepository.save(order)
        logger.info("Order created: ${savedOrder.id} for user $userId")
        
        // Создай новую корзину для пользователя
        val newCart = Order(userId = userId, status = OrderStatus.CART)
        orderRepository.save(newCart)
        logger.debug("New cart created for user $userId")
        
        orderEventPublisher.publishOrderCreated(savedOrder.toResponse(orderItemRepository, kafkaRpcClient))
        return savedOrder.toResponse(orderItemRepository, kafkaRpcClient)
    }
    
    fun getOrderById(orderId: Long, userId: Long): OrderResponse {
        
        validateUser(userId)
        
        logger.info("Fetching order $orderId for user $userId")
        
        val order = orderRepository.findByIdAndUserId(orderId, userId)
            .orElseThrow { ResourceNotFoundException("Order not found: $orderId") }
        
        return order.toResponse(orderItemRepository, kafkaRpcClient)
    }
    
    fun getUserOrders(userId: Long, page: Int, pageSize: Int): PaginatedResponse<OrderResponse> {
        
        validateUser(userId)
        
        if (page < 1 || pageSize < 1) {
            throw BadRequestException("Page and pageSize must be greater than 0")
        }
        
        logger.info("Fetching orders for user $userId (page: $page, pageSize: $pageSize)")
        
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
                    logger.warn("Could not fetch product ${item.productId} for item ${item.id}", e)
                    // Fallback если сервис недоступен
                    OrderItemResponse(
                        id = item.id,
                        productId = item.productId,
                        productName = "Product unavailable",
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
