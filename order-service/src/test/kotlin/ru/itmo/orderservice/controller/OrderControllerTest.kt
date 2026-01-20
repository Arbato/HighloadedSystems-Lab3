package ru.itmo.orderservice.adapter.`in`.web

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import ru.itmo.orderservice.adapter.exception.BadRequestException
import ru.itmo.orderservice.adapter.exception.ResourceNotFoundException
import ru.itmo.orderservice.domain.model.dto.request.CreateOrderRequest
import ru.itmo.orderservice.domain.model.dto.response.OrderItemResponse
import ru.itmo.orderservice.domain.model.dto.response.OrderResponse
import ru.itmo.orderservice.application.service.OrderService
import ru.itmo.orderservice.domain.model.dto.response.PaginatedResponse
import java.math.BigDecimal
import java.time.LocalDateTime

@WebMvcTest(controllers = [OrderController::class])
@ActiveProfiles("test")
@TestPropertySource(properties = [
    "spring.cloud.config.enabled=false",
    "eureka.client.enabled=false"
])
@org.springframework.context.annotation.Import(ru.itmo.orderservice.adapter.exception.GlobalExceptionHandler::class)
@DisplayName("OrderController Tests")
class OrderControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockBean
    private lateinit var orderService: OrderService

    private val now = LocalDateTime.now()

    private val testOrderResponse = OrderResponse(
        id = 1L,
        userId = 1L,
        items = listOf(
            OrderItemResponse(
                id = 1L,
                productId = 1L,
                productName = "Test Product",
                productPrice = BigDecimal("100.00"),
                quantity = 2,
                price = BigDecimal("100.00"),
                subtotal = BigDecimal("200.00"),
                createdAt = now
            )
        ),
        totalPrice = BigDecimal("200.00"),
        status = "PENDING",
        deliveryAddress = "123 Main St",
        createdAt = now,
        updatedAt = now
    )

    // ==================== POST /api/orders ====================

    @Test
    @DisplayName("POST /api/orders creates order")
    fun testCreateOrder() {
        val request = CreateOrderRequest(deliveryAddress = "123 Main St")

        whenever(orderService.createOrder(eq(1L), any())).thenReturn(testOrderResponse)

        mockMvc.perform(post("/api/orders")
            .header("X-User-Id", "1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.deliveryAddress").value("123 Main St"))
    }

    @Test
    @DisplayName("POST /api/orders returns 400 on empty cart")
    fun testCreateOrderEmptyCart() {
        val request = CreateOrderRequest(deliveryAddress = "123 Main St")

        whenever(orderService.createOrder(eq(1L), any()))
            .thenThrow(BadRequestException("Cannot create order from empty cart"))

        mockMvc.perform(post("/api/orders")
            .header("X-User-Id", "1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("POST /api/orders returns 404 when cart not found")
    fun testCreateOrderCartNotFound() {
        val request = CreateOrderRequest(deliveryAddress = "123 Main St")

        whenever(orderService.createOrder(eq(1L), any()))
            .thenThrow(ResourceNotFoundException("Cart not found"))

        mockMvc.perform(post("/api/orders")
            .header("X-User-Id", "1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound)
    }

    @Test
    @DisplayName("POST /api/orders returns 400 on blank address")
    fun testCreateOrderBlankAddress() {
        val request = CreateOrderRequest(deliveryAddress = "   ")

        whenever(orderService.createOrder(eq(1L), any()))
            .thenThrow(BadRequestException("Delivery address is required"))

        mockMvc.perform(post("/api/orders")
            .header("X-User-Id", "1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest)
    }

    // ==================== GET /api/orders/{orderId} ====================

    @Test
    @DisplayName("GET /api/orders/{orderId} returns order")
    fun testGetOrder() {
        whenever(orderService.getOrderById(1L, 1L)).thenReturn(testOrderResponse)

        mockMvc.perform(get("/api/orders/1")
            .header("X-User-Id", "1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.userId").value(1))
            .andExpect(jsonPath("$.status").value("PENDING"))
    }

    @Test
    @DisplayName("GET /api/orders/{orderId} returns 404 when not found")
    fun testGetOrderNotFound() {
        whenever(orderService.getOrderById(999L, 1L))
            .thenThrow(ResourceNotFoundException("Order not found"))

        mockMvc.perform(get("/api/orders/999")
            .header("X-User-Id", "1"))
            .andExpect(status().isNotFound)
    }

    @Test
    @DisplayName("GET /api/orders/{orderId} returns 400 on invalid order ID")
    fun testGetOrderInvalidOrderId() {
        whenever(orderService.getOrderById(0L, 1L))
            .thenThrow(BadRequestException("Invalid order ID"))

        mockMvc.perform(get("/api/orders/0")
            .header("X-User-Id", "1"))
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("GET /api/orders/{orderId} returns order with items")
    fun testGetOrderWithItems() {
        whenever(orderService.getOrderById(1L, 1L)).thenReturn(testOrderResponse)

        mockMvc.perform(get("/api/orders/1")
            .header("X-User-Id", "1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items").isArray)
            .andExpect(jsonPath("$.items[0].productName").value("Test Product"))
            .andExpect(jsonPath("$.items[0].quantity").value(2))
    }

    // ==================== GET /api/orders ====================

    @Test
    @DisplayName("GET /api/orders returns paginated orders")
    fun testGetUserOrders() {
        val response = PaginatedResponse(
            data = listOf(testOrderResponse),
            page = 1,
            pageSize = 20,
            totalElements = 1L,
            totalPages = 1
        )

        whenever(orderService.getUserOrders(1L, 1, 20)).thenReturn(response)

        mockMvc.perform(get("/api/orders")
            .header("X-User-Id", "1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data[0].id").value(1))
            .andExpect(jsonPath("$.page").value(1))
            .andExpect(jsonPath("$.pageSize").value(20))
    }

    @Test
    @DisplayName("GET /api/orders with custom pagination")
    fun testGetUserOrdersWithPagination() {
        val response = PaginatedResponse(
            data = listOf(testOrderResponse),
            page = 2,
            pageSize = 10,
            totalElements = 15L,
            totalPages = 2
        )

        whenever(orderService.getUserOrders(1L, 2, 10)).thenReturn(response)

        mockMvc.perform(get("/api/orders")
            .header("X-User-Id", "1")
            .param("page", "2")
            .param("pageSize", "10"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.page").value(2))
            .andExpect(jsonPath("$.pageSize").value(10))
    }

    @Test
    @DisplayName("GET /api/orders returns empty list")
    fun testGetUserOrdersEmpty() {
        val response = PaginatedResponse(
            data = emptyList<OrderResponse>(),
            page = 1,
            pageSize = 20,
            totalElements = 0L,
            totalPages = 0
        )

        whenever(orderService.getUserOrders(1L, 1, 20)).thenReturn(response)

        mockMvc.perform(get("/api/orders")
            .header("X-User-Id", "1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").isEmpty)
            .andExpect(jsonPath("$.totalElements").value(0))
    }

    @Test
    @DisplayName("GET /api/orders returns 400 on invalid user ID")
    fun testGetUserOrdersInvalidUserId() {
        whenever(orderService.getUserOrders(0L, 1, 20))
            .thenThrow(BadRequestException("Invalid user ID"))

        mockMvc.perform(get("/api/orders")
            .header("X-User-Id", "0"))
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("GET /api/orders returns 400 on invalid page")
    fun testGetUserOrdersInvalidPage() {
        whenever(orderService.getUserOrders(1L, 0, 20))
            .thenThrow(BadRequestException("Page must be greater than 0"))

        mockMvc.perform(get("/api/orders")
            .header("X-User-Id", "1")
            .param("page", "0"))
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("GET /api/orders returns multiple orders")
    fun testGetUserOrdersMultiple() {
        val order2 = testOrderResponse.copy(id = 2L, status = "DELIVERED")
        val response = PaginatedResponse(
            data = listOf(testOrderResponse, order2),
            page = 1,
            pageSize = 20,
            totalElements = 2L,
            totalPages = 1
        )

        whenever(orderService.getUserOrders(1L, 1, 20)).thenReturn(response)

        mockMvc.perform(get("/api/orders")
            .header("X-User-Id", "1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[0].status").value("PENDING"))
            .andExpect(jsonPath("$.data[1].status").value("DELIVERED"))
    }
}
