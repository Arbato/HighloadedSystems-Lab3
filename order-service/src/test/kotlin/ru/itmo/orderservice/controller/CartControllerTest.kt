package ru.itmo.orderservice.controller

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
import ru.itmo.orderservice.adapters.exception.BadRequestException
import ru.itmo.orderservice.adapters.exception.ResourceNotFoundException
import ru.itmo.orderservice.application.dto.request.AddToCartRequest
import ru.itmo.orderservice.application.dto.request.UpdateQuantityRequest
import ru.itmo.orderservice.application.dto.response.OrderItemResponse
import ru.itmo.orderservice.application.dto.response.OrderResponse
import ru.itmo.orderservice.application.service.OrderService
import ru.itmo.orderservice.presentation.adapter.exception.GlobalExceptionHandler
import ru.itmo.orderservice.presentation.controller.CartController
import java.math.BigDecimal
import java.time.LocalDateTime

@WebMvcTest(controllers = [CartController::class])
@ActiveProfiles("test")
@TestPropertySource(properties = [
    "spring.cloud.config.enabled=false",
    "eureka.client.enabled=false"
])
@org.springframework.context.annotation.Import(GlobalExceptionHandler::class)
@DisplayName("CartController Tests")
class CartControllerTest {

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
        status = "CART",
        deliveryAddress = null,
        createdAt = now,
        updatedAt = now
    )

    // ==================== GET /api/cart ====================

    @Test
    @DisplayName("GET /api/cart returns cart")
    fun testGetCart() {
        whenever(orderService.getCart(1L)).thenReturn(testOrderResponse)

        mockMvc.perform(get("/api/cart")
            .header("X-User-Id", "1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.userId").value(1))
            .andExpect(jsonPath("$.status").value("CART"))
            .andExpect(jsonPath("$.items").isArray)
            .andExpect(jsonPath("$.items[0].productName").value("Test Product"))
    }

    @Test
    @DisplayName("GET /api/cart returns 400 on invalid user ID")
    fun testGetCartInvalidUserId() {
        whenever(orderService.getCart(0L))
            .thenThrow(BadRequestException("Invalid user ID"))

        mockMvc.perform(get("/api/cart")
            .header("X-User-Id", "0"))
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("GET /api/cart returns empty cart")
    fun testGetCartEmpty() {
        val emptyCart = testOrderResponse.copy(items = emptyList(), totalPrice = BigDecimal.ZERO)
        whenever(orderService.getCart(1L)).thenReturn(emptyCart)

        mockMvc.perform(get("/api/cart")
            .header("X-User-Id", "1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items").isEmpty)
            .andExpect(jsonPath("$.totalPrice").value(0))
    }

    // ==================== POST /api/cart/items ====================

    @Test
    @DisplayName("POST /api/cart/items adds item to cart")
    fun testAddToCart() {
        val request = AddToCartRequest(productId = 1L, quantity = 2)

        whenever(orderService.addToCart(1L, 1L, 2)).thenReturn(testOrderResponse)

        mockMvc.perform(post("/api/cart/items")
            .header("X-User-Id", "1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items[0].quantity").value(2))
    }

    @Test
    @DisplayName("POST /api/cart/items returns 404 when product not found")
    fun testAddToCartProductNotFound() {
        val request = AddToCartRequest(productId = 999L, quantity = 1)

        whenever(orderService.addToCart(eq(1L), eq(999L), eq(1)))
            .thenThrow(ResourceNotFoundException("Product not found"))

        mockMvc.perform(post("/api/cart/items")
            .header("X-User-Id", "1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound)
    }

    @Test
    @DisplayName("POST /api/cart/items returns 400 on invalid quantity")
    fun testAddToCartInvalidQuantity() {
        val request = AddToCartRequest(productId = 1L, quantity = 0)

        whenever(orderService.addToCart(eq(1L), eq(1L), eq(0)))
            .thenThrow(BadRequestException("Quantity must be at least 1"))

        mockMvc.perform(post("/api/cart/items")
            .header("X-User-Id", "1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest)
    }

    // ==================== PUT /api/cart/items/{itemId} ====================

    @Test
    @DisplayName("PUT /api/cart/items/{itemId} updates quantity")
    fun testUpdateCartItem() {
        val request = UpdateQuantityRequest(quantity = 5)
        val updatedResponse = testOrderResponse.copy(
            items = listOf(testOrderResponse.items[0].copy(quantity = 5))
        )

        whenever(orderService.updateCartItemQuantity(1L, 1L, 5)).thenReturn(updatedResponse)

        mockMvc.perform(put("/api/cart/items/1")
            .header("X-User-Id", "1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items[0].quantity").value(5))
    }

    @Test
    @DisplayName("PUT /api/cart/items/{itemId} returns 404 when item not found")
    fun testUpdateCartItemNotFound() {
        val request = UpdateQuantityRequest(quantity = 5)

        whenever(orderService.updateCartItemQuantity(eq(1L), eq(999L), eq(5)))
            .thenThrow(ResourceNotFoundException("Item not found"))

        mockMvc.perform(put("/api/cart/items/999")
            .header("X-User-Id", "1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound)
    }

    @Test
    @DisplayName("PUT /api/cart/items/{itemId} returns 400 when item not in cart")
    fun testUpdateCartItemNotInCart() {
        val request = UpdateQuantityRequest(quantity = 5)

        whenever(orderService.updateCartItemQuantity(eq(1L), eq(1L), eq(5)))
            .thenThrow(BadRequestException("Item does not belong to user's cart"))

        mockMvc.perform(put("/api/cart/items/1")
            .header("X-User-Id", "1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest)
    }

    // ==================== DELETE /api/cart/items/{itemId} ====================

    @Test
    @DisplayName("DELETE /api/cart/items/{itemId} removes item")
    fun testRemoveFromCart() {
        val emptyCart = testOrderResponse.copy(items = emptyList(), totalPrice = BigDecimal.ZERO)

        whenever(orderService.removeFromCart(1L, 1L)).thenReturn(emptyCart)

        mockMvc.perform(delete("/api/cart/items/1")
            .header("X-User-Id", "1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items").isEmpty)
    }

    @Test
    @DisplayName("DELETE /api/cart/items/{itemId} returns 404 when item not found")
    fun testRemoveFromCartNotFound() {
        whenever(orderService.removeFromCart(eq(1L), eq(999L)))
            .thenThrow(ResourceNotFoundException("Item not found"))

        mockMvc.perform(delete("/api/cart/items/999")
            .header("X-User-Id", "1"))
            .andExpect(status().isNotFound)
    }

    @Test
    @DisplayName("DELETE /api/cart/items/{itemId} returns 404 when cart not found")
    fun testRemoveFromCartCartNotFound() {
        whenever(orderService.removeFromCart(eq(1L), eq(1L)))
            .thenThrow(ResourceNotFoundException("Cart not found"))

        mockMvc.perform(delete("/api/cart/items/1")
            .header("X-User-Id", "1"))
            .andExpect(status().isNotFound)
    }

    // ==================== DELETE /api/cart ====================

    @Test
    @DisplayName("DELETE /api/cart clears cart")
    fun testClearCart() {
        doNothing().whenever(orderService).clearCart(1L)

        mockMvc.perform(delete("/api/cart")
            .header("X-User-Id", "1"))
            .andExpect(status().isNoContent)

        verify(orderService).clearCart(1L)
    }

    @Test
    @DisplayName("DELETE /api/cart returns 404 when cart not found")
    fun testClearCartNotFound() {
        doThrow(ResourceNotFoundException("Cart not found"))
            .whenever(orderService).clearCart(1L)

        mockMvc.perform(delete("/api/cart")
            .header("X-User-Id", "1"))
            .andExpect(status().isNotFound)
    }

    @Test
    @DisplayName("DELETE /api/cart returns 400 on invalid user ID")
    fun testClearCartInvalidUserId() {
        doThrow(BadRequestException("Invalid user ID"))
            .whenever(orderService).clearCart(0L)

        mockMvc.perform(delete("/api/cart")
            .header("X-User-Id", "0"))
            .andExpect(status().isBadRequest)
    }
}
