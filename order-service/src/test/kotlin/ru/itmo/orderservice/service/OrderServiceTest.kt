package ru.itmo.orderservice.application.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import ru.itmo.orderservice.adapter.out.messaging.kafka.client.KafkaRpcClient
import ru.itmo.orderservice.adapter.exception.BadRequestException
import ru.itmo.orderservice.adapter.exception.ResourceNotFoundException
import ru.itmo.orderservice.domain.model.dto.request.CreateOrderRequest
import ru.itmo.orderservice.domain.model.dto.response.ProductResponse
import ru.itmo.orderservice.domain.model.entity.Order
import ru.itmo.orderservice.domain.model.entity.OrderItem
import ru.itmo.orderservice.domain.model.enums.OrderStatus
import ru.itmo.orderservice.domain.repository.OrderRepository
import ru.itmo.orderservice.domain.repository.OrderItemRepository
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
@DisplayName("OrderService Unit Tests")
class OrderServiceTest {

    @Mock
    private lateinit var orderRepository: OrderRepository

    @Mock
    private lateinit var orderItemRepository: OrderItemRepository

    @Mock
    private lateinit var kafkaRpcClient: KafkaRpcClient

    private lateinit var orderService: OrderService

    private val now = LocalDateTime.now()

    private val testProduct = ProductResponse(
        id = 1L,
        name = "Test Product",
        description = "Test Description",
        price = BigDecimal("100.00"),
        imageUrl = "http://image.url",
        shopId = 1L,
        sellerId = 1L,
        status = "APPROVED",
        rejectionReason = null,
        averageRating = 4.5,
        commentsCount = 10L,
        createdAt = now,
        updatedAt = now
    )

    private val testOrder = Order(
        id = 1L,
        userId = 1L,
        totalPrice = BigDecimal("200.00"),
        status = OrderStatus.CART,
        deliveryAddress = null,
        createdAt = now,
        updatedAt = now
    )

    private val testOrderItem = OrderItem(
        id = 1L,
        orderId = 1L,
        productId = 1L,
        quantity = 2,
        price = BigDecimal("100.00"),
        createdAt = now
    )

    @BeforeEach
    fun setUp() {
        orderService = OrderService(orderRepository, orderItemRepository, kafkaRpcClient)
    }

    // ==================== getCart Tests ====================

    @Test
    @DisplayName("getCart returns existing cart")
    fun testGetCartReturnsExistingCart() {
        whenever(orderRepository.findByUserIdAndStatus(1L, OrderStatus.CART))
            .thenReturn(Optional.of(testOrder))
        whenever(orderItemRepository.findAllByOrderId(1L))
            .thenReturn(listOf(testOrderItem))
        whenever(kafkaRpcClient.getProductById(1L))
            .thenReturn(testProduct)

        val result = orderService.getCart(1L)

        assert(result.id == 1L)
        assert(result.userId == 1L)
        assert(result.status == "CART")
    }

    @Test
    @DisplayName("getCart creates new cart when none exists")
    fun testGetCartCreatesNewCart() {
        val newCart = Order(id = 0L, userId = 1L, status = OrderStatus.CART)
        val savedCart = newCart.copy(id = 2L)

        whenever(orderRepository.findByUserIdAndStatus(1L, OrderStatus.CART))
            .thenReturn(Optional.empty())
        whenever(orderRepository.save(any<Order>())).thenReturn(savedCart)
        whenever(orderItemRepository.findAllByOrderId(any())).thenReturn(emptyList())

        val result = orderService.getCart(1L)

        verify(orderRepository).save(any())
        assert(result.status == "CART")
    }

    @Test
    @DisplayName("getCart throws on invalid user ID")
    fun testGetCartInvalidUserId() {
        assertThrows<BadRequestException> {
            orderService.getCart(0L)
        }
    }

    @Test
    @DisplayName("getCart throws on negative user ID")
    fun testGetCartNegativeUserId() {
        assertThrows<BadRequestException> {
            orderService.getCart(-1L)
        }
    }

    // ==================== addToCart Tests ====================

    @Test
    @DisplayName("addToCart adds new product to cart")
    fun testAddToCartNewProduct() {
        val cart = testOrder.copy(totalPrice = BigDecimal.ZERO)
        val savedCart = cart.copy(id = 1L)

        whenever(kafkaRpcClient.getProductById(1L)).thenReturn(testProduct)
        whenever(orderRepository.findByUserIdAndStatus(1L, OrderStatus.CART))
            .thenReturn(Optional.of(savedCart))
        whenever(orderItemRepository.findByOrderIdAndProductId(1L, 1L))
            .thenReturn(Optional.empty())
        whenever(orderItemRepository.save(any<OrderItem>())).thenAnswer { it.arguments[0] }
        whenever(orderItemRepository.findAllByOrderId(1L)).thenReturn(listOf(testOrderItem))
        whenever(orderRepository.save(any<Order>())).thenAnswer { it.arguments[0] }

        val result = orderService.addToCart(1L, 1L, 2)

        verify(orderItemRepository).save(any())
        assert(result.userId == 1L)
    }

    @Test
    @DisplayName("addToCart increases quantity for existing product")
    fun testAddToCartExistingProduct() {
        whenever(kafkaRpcClient.getProductById(1L)).thenReturn(testProduct)
        whenever(orderRepository.findByUserIdAndStatus(1L, OrderStatus.CART))
            .thenReturn(Optional.of(testOrder))
        whenever(orderItemRepository.findByOrderIdAndProductId(1L, 1L))
            .thenReturn(Optional.of(testOrderItem))
        whenever(orderItemRepository.save(any<OrderItem>())).thenAnswer { it.arguments[0] }
        whenever(orderItemRepository.findAllByOrderId(1L)).thenReturn(listOf(testOrderItem))
        whenever(orderRepository.save(any<Order>())).thenAnswer { it.arguments[0] }

        orderService.addToCart(1L, 1L, 1)

        verify(orderItemRepository).save(argThat<OrderItem> { quantity == 3 })
    }

    @Test
    @DisplayName("addToCart throws on invalid user ID")
    fun testAddToCartInvalidUserId() {
        assertThrows<BadRequestException> {
            orderService.addToCart(0L, 1L, 1)
        }
    }

    @Test
    @DisplayName("addToCart throws on invalid product ID")
    fun testAddToCartInvalidProductId() {
        assertThrows<BadRequestException> {
            orderService.addToCart(1L, 0L, 1)
        }
    }

    @Test
    @DisplayName("addToCart throws on quantity less than 1")
    fun testAddToCartInvalidQuantity() {
        assertThrows<BadRequestException> {
            orderService.addToCart(1L, 1L, 0)
        }
    }

    @Test
    @DisplayName("addToCart throws on negative quantity")
    fun testAddToCartNegativeQuantity() {
        assertThrows<BadRequestException> {
            orderService.addToCart(1L, 1L, -1)
        }
    }

    @Test
    @DisplayName("addToCart creates new cart if none exists")
    fun testAddToCartCreatesNewCart() {
        val newCart = Order(id = 0L, userId = 1L, status = OrderStatus.CART)
        val savedCart = newCart.copy(id = 2L)

        whenever(kafkaRpcClient.getProductById(1L)).thenReturn(testProduct)
        whenever(orderRepository.findByUserIdAndStatus(1L, OrderStatus.CART))
            .thenReturn(Optional.empty())
        whenever(orderRepository.save(any<Order>())).thenReturn(savedCart)
        whenever(orderItemRepository.findByOrderIdAndProductId(2L, 1L))
            .thenReturn(Optional.empty())
        whenever(orderItemRepository.save(any<OrderItem>())).thenAnswer { it.arguments[0] }
        whenever(orderItemRepository.findAllByOrderId(2L)).thenReturn(listOf(testOrderItem))

        orderService.addToCart(1L, 1L, 1)

        verify(orderRepository, atLeast(1)).save(any())
    }

    // ==================== updateCartItemQuantity Tests ====================

    @Test
    @DisplayName("updateCartItemQuantity updates quantity")
    fun testUpdateCartItemQuantity() {
        whenever(orderRepository.findByUserIdAndStatus(1L, OrderStatus.CART))
            .thenReturn(Optional.of(testOrder))
        whenever(orderItemRepository.findById(1L))
            .thenReturn(Optional.of(testOrderItem))
        whenever(orderItemRepository.save(any<OrderItem>())).thenAnswer { it.arguments[0] }
        whenever(orderItemRepository.findAllByOrderId(1L)).thenReturn(listOf(testOrderItem))
        whenever(orderRepository.save(any<Order>())).thenAnswer { it.arguments[0] }
        whenever(kafkaRpcClient.getProductById(1L)).thenReturn(testProduct)

        val result = orderService.updateCartItemQuantity(1L, 1L, 5)

        verify(orderItemRepository).save(argThat<OrderItem> { quantity == 5 })
    }

    @Test
    @DisplayName("updateCartItemQuantity deletes item when quantity is 0")
    fun testUpdateCartItemQuantityDeletesOnZero() {
        whenever(orderRepository.findByUserIdAndStatus(1L, OrderStatus.CART))
            .thenReturn(Optional.of(testOrder))
        whenever(orderItemRepository.findById(1L))
            .thenReturn(Optional.of(testOrderItem))
        whenever(orderItemRepository.findAllByOrderId(1L)).thenReturn(emptyList())
        whenever(orderRepository.save(any<Order>())).thenAnswer { it.arguments[0] }

        orderService.updateCartItemQuantity(1L, 1L, 0)

        verify(orderItemRepository).deleteById(1L)
    }

    @Test
    @DisplayName("updateCartItemQuantity throws on invalid user ID")
    fun testUpdateCartItemQuantityInvalidUserId() {
        assertThrows<BadRequestException> {
            orderService.updateCartItemQuantity(0L, 1L, 1)
        }
    }

    @Test
    @DisplayName("updateCartItemQuantity throws on invalid item ID")
    fun testUpdateCartItemQuantityInvalidItemId() {
        assertThrows<BadRequestException> {
            orderService.updateCartItemQuantity(1L, 0L, 1)
        }
    }

    @Test
    @DisplayName("updateCartItemQuantity throws on negative quantity")
    fun testUpdateCartItemQuantityNegativeQuantity() {
        assertThrows<BadRequestException> {
            orderService.updateCartItemQuantity(1L, 1L, -1)
        }
    }

    @Test
    @DisplayName("updateCartItemQuantity throws when cart not found")
    fun testUpdateCartItemQuantityCartNotFound() {
        whenever(orderRepository.findByUserIdAndStatus(1L, OrderStatus.CART))
            .thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> {
            orderService.updateCartItemQuantity(1L, 1L, 1)
        }
    }

    @Test
    @DisplayName("updateCartItemQuantity throws when item not found")
    fun testUpdateCartItemQuantityItemNotFound() {
        whenever(orderRepository.findByUserIdAndStatus(1L, OrderStatus.CART))
            .thenReturn(Optional.of(testOrder))
        whenever(orderItemRepository.findById(999L))
            .thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> {
            orderService.updateCartItemQuantity(1L, 999L, 1)
        }
    }

    @Test
    @DisplayName("updateCartItemQuantity throws when item not in user's cart")
    fun testUpdateCartItemQuantityItemNotInCart() {
        val otherItem = testOrderItem.copy(orderId = 999L)

        whenever(orderRepository.findByUserIdAndStatus(1L, OrderStatus.CART))
            .thenReturn(Optional.of(testOrder))
        whenever(orderItemRepository.findById(1L))
            .thenReturn(Optional.of(otherItem))

        assertThrows<BadRequestException> {
            orderService.updateCartItemQuantity(1L, 1L, 1)
        }
    }

    // ==================== removeFromCart Tests ====================

    @Test
    @DisplayName("removeFromCart removes item successfully")
    fun testRemoveFromCart() {
        whenever(orderRepository.findByUserIdAndStatus(1L, OrderStatus.CART))
            .thenReturn(Optional.of(testOrder))
        whenever(orderItemRepository.findById(1L))
            .thenReturn(Optional.of(testOrderItem))
        whenever(orderItemRepository.findAllByOrderId(1L)).thenReturn(emptyList())
        whenever(orderRepository.save(any<Order>())).thenAnswer { it.arguments[0] }

        orderService.removeFromCart(1L, 1L)

        verify(orderItemRepository).deleteById(1L)
    }

    @Test
    @DisplayName("removeFromCart throws on invalid user ID")
    fun testRemoveFromCartInvalidUserId() {
        assertThrows<BadRequestException> {
            orderService.removeFromCart(0L, 1L)
        }
    }

    @Test
    @DisplayName("removeFromCart throws on invalid item ID")
    fun testRemoveFromCartInvalidItemId() {
        assertThrows<BadRequestException> {
            orderService.removeFromCart(1L, 0L)
        }
    }

    @Test
    @DisplayName("removeFromCart throws when cart not found")
    fun testRemoveFromCartCartNotFound() {
        whenever(orderRepository.findByUserIdAndStatus(1L, OrderStatus.CART))
            .thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> {
            orderService.removeFromCart(1L, 1L)
        }
    }

    @Test
    @DisplayName("removeFromCart throws when item not in user's cart")
    fun testRemoveFromCartItemNotInCart() {
        val otherItem = testOrderItem.copy(orderId = 999L)

        whenever(orderRepository.findByUserIdAndStatus(1L, OrderStatus.CART))
            .thenReturn(Optional.of(testOrder))
        whenever(orderItemRepository.findById(1L))
            .thenReturn(Optional.of(otherItem))

        assertThrows<BadRequestException> {
            orderService.removeFromCart(1L, 1L)
        }
    }

    // ==================== clearCart Tests ====================

    @Test
    @DisplayName("clearCart clears cart successfully")
    fun testClearCart() {
        whenever(orderRepository.findByUserIdAndStatus(1L, OrderStatus.CART))
            .thenReturn(Optional.of(testOrder))
        whenever(orderRepository.save(any<Order>())).thenAnswer { it.arguments[0] }

        orderService.clearCart(1L)

        verify(orderItemRepository).deleteByOrderId(1L)
        verify(orderRepository).save(argThat<Order> { totalPrice == BigDecimal.ZERO })
    }

    @Test
    @DisplayName("clearCart throws on invalid user ID")
    fun testClearCartInvalidUserId() {
        assertThrows<BadRequestException> {
            orderService.clearCart(0L)
        }
    }

    @Test
    @DisplayName("clearCart throws when cart not found")
    fun testClearCartCartNotFound() {
        whenever(orderRepository.findByUserIdAndStatus(1L, OrderStatus.CART))
            .thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> {
            orderService.clearCart(1L)
        }
    }

    // ==================== createOrder Tests ====================

    @Test
    @DisplayName("createOrder creates order from cart")
    fun testCreateOrder() {
        val request = CreateOrderRequest(deliveryAddress = "123 Main St")

        whenever(orderRepository.findByUserIdAndStatus(1L, OrderStatus.CART))
            .thenReturn(Optional.of(testOrder))
        whenever(orderItemRepository.findAllByOrderId(1L))
            .thenReturn(listOf(testOrderItem))
        whenever(orderRepository.save(any<Order>())).thenAnswer { it.arguments[0] }
        whenever(kafkaRpcClient.getProductById(1L)).thenReturn(testProduct)

        val result = orderService.createOrder(1L, request)

        assert(result.status == "PENDING")
        assert(result.deliveryAddress == "123 Main St")
        verify(orderRepository, atLeast(2)).save(any()) // order + new cart
    }

    @Test
    @DisplayName("createOrder throws on invalid user ID")
    fun testCreateOrderInvalidUserId() {
        val request = CreateOrderRequest(deliveryAddress = "123 Main St")

        assertThrows<BadRequestException> {
            orderService.createOrder(0L, request)
        }
    }

    @Test
    @DisplayName("createOrder throws on blank delivery address")
    fun testCreateOrderBlankAddress() {
        val request = CreateOrderRequest(deliveryAddress = "   ")

        assertThrows<BadRequestException> {
            orderService.createOrder(1L, request)
        }
    }

    @Test
    @DisplayName("createOrder throws when cart not found")
    fun testCreateOrderCartNotFound() {
        val request = CreateOrderRequest(deliveryAddress = "123 Main St")

        whenever(orderRepository.findByUserIdAndStatus(1L, OrderStatus.CART))
            .thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> {
            orderService.createOrder(1L, request)
        }
    }

    @Test
    @DisplayName("createOrder throws when cart is empty")
    fun testCreateOrderEmptyCart() {
        val request = CreateOrderRequest(deliveryAddress = "123 Main St")

        whenever(orderRepository.findByUserIdAndStatus(1L, OrderStatus.CART))
            .thenReturn(Optional.of(testOrder))
        whenever(orderItemRepository.findAllByOrderId(1L))
            .thenReturn(emptyList())

        assertThrows<BadRequestException> {
            orderService.createOrder(1L, request)
        }
    }

    // ==================== getOrderById Tests ====================

    @Test
    @DisplayName("getOrderById returns order")
    fun testGetOrderById() {
        val order = testOrder.copy(status = OrderStatus.PENDING)

        whenever(orderRepository.findByIdAndUserId(1L, 1L))
            .thenReturn(Optional.of(order))
        whenever(orderItemRepository.findAllByOrderId(1L))
            .thenReturn(listOf(testOrderItem))
        whenever(kafkaRpcClient.getProductById(1L)).thenReturn(testProduct)

        val result = orderService.getOrderById(1L, 1L)

        assert(result.id == 1L)
        assert(result.status == "PENDING")
    }

    @Test
    @DisplayName("getOrderById throws on invalid order ID")
    fun testGetOrderByIdInvalidOrderId() {
        assertThrows<BadRequestException> {
            orderService.getOrderById(0L, 1L)
        }
    }

    @Test
    @DisplayName("getOrderById throws on invalid user ID")
    fun testGetOrderByIdInvalidUserId() {
        assertThrows<BadRequestException> {
            orderService.getOrderById(1L, 0L)
        }
    }

    @Test
    @DisplayName("getOrderById throws when order not found")
    fun testGetOrderByIdNotFound() {
        whenever(orderRepository.findByIdAndUserId(999L, 1L))
            .thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> {
            orderService.getOrderById(999L, 1L)
        }
    }

    // ==================== getUserOrders Tests ====================

    @Test
    @DisplayName("getUserOrders returns paginated orders")
    fun testGetUserOrders() {
        val orders = listOf(testOrder.copy(status = OrderStatus.PENDING))
        val page = PageImpl(orders, PageRequest.of(0, 10), 1)

        whenever(orderRepository.findAllByUserIdAndStatusNot(1L, OrderStatus.CART, PageRequest.of(0, 10)))
            .thenReturn(page)
        whenever(orderItemRepository.findAllByOrderId(1L))
            .thenReturn(listOf(testOrderItem))
        whenever(kafkaRpcClient.getProductById(1L)).thenReturn(testProduct)

        val result = orderService.getUserOrders(1L, 1, 10)

        assert(result.data.size == 1)
        assert(result.page == 1)
        assert(result.pageSize == 10)
        assert(result.totalElements == 1L)
    }

    @Test
    @DisplayName("getUserOrders throws on invalid user ID")
    fun testGetUserOrdersInvalidUserId() {
        assertThrows<BadRequestException> {
            orderService.getUserOrders(0L, 1, 10)
        }
    }

    @Test
    @DisplayName("getUserOrders throws on invalid page")
    fun testGetUserOrdersInvalidPage() {
        assertThrows<BadRequestException> {
            orderService.getUserOrders(1L, 0, 10)
        }
    }

    @Test
    @DisplayName("getUserOrders throws on invalid pageSize")
    fun testGetUserOrdersInvalidPageSize() {
        assertThrows<BadRequestException> {
            orderService.getUserOrders(1L, 1, 0)
        }
    }

    @Test
    @DisplayName("getUserOrders returns empty list when no orders")
    fun testGetUserOrdersEmpty() {
        val emptyPage = PageImpl<Order>(emptyList(), PageRequest.of(0, 10), 0)

        whenever(orderRepository.findAllByUserIdAndStatusNot(1L, OrderStatus.CART, PageRequest.of(0, 10)))
            .thenReturn(emptyPage)

        val result = orderService.getUserOrders(1L, 1, 10)

        assert(result.data.isEmpty())
        assert(result.totalElements == 0L)
    }

    // ==================== Product Service Fallback Tests ====================

    @Test
    @DisplayName("getCart handles product service unavailable gracefully")
    fun testGetCartProductServiceUnavailable() {
        whenever(orderRepository.findByUserIdAndStatus(1L, OrderStatus.CART))
            .thenReturn(Optional.of(testOrder))
        whenever(orderItemRepository.findAllByOrderId(1L))
            .thenReturn(listOf(testOrderItem))
        whenever(kafkaRpcClient.getProductById(1L))
            .thenThrow(RuntimeException("Service unavailable"))

        val result = orderService.getCart(1L)

        assert(result.items.isNotEmpty())
        assert(result.items[0].productName == "Product (unavailable)")
    }
}
