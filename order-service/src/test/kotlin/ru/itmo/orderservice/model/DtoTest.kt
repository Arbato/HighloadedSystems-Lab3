package ru.itmo.orderservice.model

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import ru.itmo.orderservice.domain.model.dto.request.AddToCartRequest
import ru.itmo.orderservice.domain.model.dto.request.CreateOrderRequest
import ru.itmo.orderservice.domain.model.dto.request.UpdateQuantityRequest
import ru.itmo.orderservice.domain.model.dto.response.OrderItemResponse
import ru.itmo.orderservice.domain.model.dto.response.OrderResponse
import ru.itmo.orderservice.domain.model.dto.response.ProductResponse
import ru.itmo.orderservice.domain.model.dto.response.PaginatedResponse
import java.math.BigDecimal
import java.time.LocalDateTime

@DisplayName("DTO Tests")
class DtoTest {

    private val now = LocalDateTime.now()

    // ==================== Request DTO Tests ====================

    @Test
    @DisplayName("AddToCartRequest has correct fields")
    fun testAddToCartRequest() {
        val request = AddToCartRequest(productId = 1L, quantity = 2)

        assert(request.productId == 1L)
        assert(request.quantity == 2)
    }

    @Test
    @DisplayName("AddToCartRequest copy works")
    fun testAddToCartRequestCopy() {
        val request = AddToCartRequest(productId = 1L, quantity = 2)
        val copied = request.copy(quantity = 5)

        assert(copied.productId == 1L)
        assert(copied.quantity == 5)
    }

    @Test
    @DisplayName("CreateOrderRequest has correct fields")
    fun testCreateOrderRequest() {
        val request = CreateOrderRequest(deliveryAddress = "123 Main St")

        assert(request.deliveryAddress == "123 Main St")
    }

    @Test
    @DisplayName("UpdateQuantityRequest has correct fields")
    fun testUpdateQuantityRequest() {
        val request = UpdateQuantityRequest(quantity = 3)

        assert(request.quantity == 3)
    }

    @Test
    @DisplayName("UpdateQuantityRequest allows zero quantity")
    fun testUpdateQuantityRequestZero() {
        val request = UpdateQuantityRequest(quantity = 0)

        assert(request.quantity == 0)
    }

    // ==================== Response DTO Tests ====================

    @Test
    @DisplayName("OrderItemResponse has correct fields")
    fun testOrderItemResponse() {
        val response = OrderItemResponse(
            id = 1L,
            productId = 1L,
            productName = "Test Product",
            productPrice = BigDecimal("100.00"),
            quantity = 2,
            price = BigDecimal("100.00"),
            subtotal = BigDecimal("200.00"),
            createdAt = now
        )

        assert(response.id == 1L)
        assert(response.productId == 1L)
        assert(response.productName == "Test Product")
        assert(response.productPrice == BigDecimal("100.00"))
        assert(response.quantity == 2)
        assert(response.price == BigDecimal("100.00"))
        assert(response.subtotal == BigDecimal("200.00"))
    }

    @Test
    @DisplayName("OrderItemResponse subtotal calculation is correct")
    fun testOrderItemResponseSubtotal() {
        val response = OrderItemResponse(
            id = 1L,
            productId = 1L,
            productName = "Test",
            productPrice = BigDecimal("50.00"),
            quantity = 3,
            price = BigDecimal("50.00"),
            subtotal = BigDecimal("150.00"),
            createdAt = now
        )

        assert(response.subtotal == response.price * BigDecimal(response.quantity))
    }

    @Test
    @DisplayName("OrderResponse has correct fields")
    fun testOrderResponse() {
        val itemResponse = OrderItemResponse(
            id = 1L,
            productId = 1L,
            productName = "Test",
            productPrice = BigDecimal("100.00"),
            quantity = 1,
            price = BigDecimal("100.00"),
            subtotal = BigDecimal("100.00"),
            createdAt = now
        )

        val response = OrderResponse(
            id = 1L,
            userId = 1L,
            items = listOf(itemResponse),
            totalPrice = BigDecimal("100.00"),
            status = "PENDING",
            deliveryAddress = "123 Main St",
            createdAt = now,
            updatedAt = now
        )

        assert(response.id == 1L)
        assert(response.userId == 1L)
        assert(response.items.size == 1)
        assert(response.totalPrice == BigDecimal("100.00"))
        assert(response.status == "PENDING")
        assert(response.deliveryAddress == "123 Main St")
    }

    @Test
    @DisplayName("OrderResponse can have empty items")
    fun testOrderResponseEmptyItems() {
        val response = OrderResponse(
            id = 1L,
            userId = 1L,
            items = emptyList(),
            totalPrice = BigDecimal.ZERO,
            status = "CART",
            deliveryAddress = null,
            createdAt = now,
            updatedAt = now
        )

        assert(response.items.isEmpty())
        assert(response.deliveryAddress == null)
    }

    @Test
    @DisplayName("ProductResponse has correct fields")
    fun testProductResponse() {
        val response = ProductResponse(
            id = 1L,
            name = "Test Product",
            description = "Description",
            price = BigDecimal("99.99"),
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

        assert(response.id == 1L)
        assert(response.name == "Test Product")
        assert(response.price == BigDecimal("99.99"))
        assert(response.status == "APPROVED")
        assert(response.averageRating == 4.5)
    }

    @Test
    @DisplayName("ProductResponse nullable fields")
    fun testProductResponseNullableFields() {
        val response = ProductResponse(
            id = 1L,
            name = "Test",
            description = null,
            price = BigDecimal("10.00"),
            imageUrl = null,
            shopId = 1L,
            sellerId = 1L,
            status = "PENDING",
            rejectionReason = null,
            averageRating = null,
            commentsCount = null,
            createdAt = now,
            updatedAt = now
        )

        assert(response.description == null)
        assert(response.imageUrl == null)
        assert(response.averageRating == null)
        assert(response.commentsCount == null)
    }

    // ==================== PaginatedResponse Tests ====================

    @Test
    @DisplayName("PaginatedResponse has correct fields")
    fun testPaginatedResponse() {
        val data = listOf("item1", "item2")
        val response = PaginatedResponse(
            data = data,
            page = 1,
            pageSize = 20,
            totalElements = 2L,
            totalPages = 1
        )

        assert(response.data == data)
        assert(response.page == 1)
        assert(response.pageSize == 20)
        assert(response.totalElements == 2L)
        assert(response.totalPages == 1)
    }

    @Test
    @DisplayName("PaginatedResponse can be empty")
    fun testPaginatedResponseEmpty() {
        val response = PaginatedResponse(
            data = emptyList<String>(),
            page = 1,
            pageSize = 20,
            totalElements = 0L,
            totalPages = 0
        )

        assert(response.data.isEmpty())
        assert(response.totalElements == 0L)
        assert(response.totalPages == 0)
    }

    @Test
    @DisplayName("PaginatedResponse with multiple pages")
    fun testPaginatedResponseMultiplePages() {
        val response = PaginatedResponse(
            data = listOf("item1", "item2"),
            page = 2,
            pageSize = 10,
            totalElements = 25L,
            totalPages = 3
        )

        assert(response.page == 2)
        assert(response.totalPages == 3)
    }

    // ==================== DTO Equality Tests ====================

    @Test
    @DisplayName("Request DTOs equality works")
    fun testRequestDtoEquality() {
        val request1 = AddToCartRequest(productId = 1L, quantity = 2)
        val request2 = AddToCartRequest(productId = 1L, quantity = 2)
        val request3 = AddToCartRequest(productId = 2L, quantity = 2)

        assert(request1 == request2)
        assert(request1 != request3)
    }

    @Test
    @DisplayName("Response DTOs equality works")
    fun testResponseDtoEquality() {
        val response1 = OrderItemResponse(
            id = 1L, productId = 1L, productName = "Test",
            productPrice = BigDecimal("10.00"), quantity = 1,
            price = BigDecimal("10.00"), subtotal = BigDecimal("10.00"),
            createdAt = now
        )
        val response2 = OrderItemResponse(
            id = 1L, productId = 1L, productName = "Test",
            productPrice = BigDecimal("10.00"), quantity = 1,
            price = BigDecimal("10.00"), subtotal = BigDecimal("10.00"),
            createdAt = now
        )

        assert(response1 == response2)
    }
}
