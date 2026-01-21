package ru.itmo.market.model

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import ru.itmo.market.application.dto.request.BulkModerationRequest
import ru.itmo.market.application.dto.request.RejectProductRequest
import ru.itmo.market.application.dto.response.ErrorResponse
import ru.itmo.market.application.dto.response.ModerationResultResponse
import ru.itmo.market.application.dto.response.PaginatedResponse
import ru.itmo.market.application.dto.response.ProductResponse
import ru.itmo.market.application.dto.response.UserResponse
import java.math.BigDecimal
import java.time.LocalDateTime

@DisplayName("DTO Tests")
class DtoTest {

    // ==================== Request DTOs ====================

    @Test
    @DisplayName("BulkModerationRequest should create with all fields")
    fun testBulkModerationRequestCreation() {
        val request = BulkModerationRequest(
            productIds = listOf(1L, 2L, 3L),
            action = "APPROVE",
            reason = "Good quality"
        )

        assert(request.productIds.size == 3)
        assert(request.action == "APPROVE")
        assert(request.reason == "Good quality")
    }

    @Test
    @DisplayName("BulkModerationRequest should allow null reason")
    fun testBulkModerationRequestNullReason() {
        val request = BulkModerationRequest(
            productIds = listOf(1L),
            action = "APPROVE",
            reason = null
        )

        assert(request.reason == null)
    }

    @Test
    @DisplayName("RejectProductRequest should create with reason")
    fun testRejectProductRequestCreation() {
        val request = RejectProductRequest(reason = "Low quality product")

        assert(request.reason == "Low quality product")
    }

    // ==================== Response DTOs ====================

    @Test
    @DisplayName("ProductResponse should create with all fields")
    fun testProductResponseCreation() {
        val now = LocalDateTime.now()
        val response = ProductResponse(
            id = 100L,
            name = "Test Product",
            description = "Test Description",
            price = BigDecimal("99.99"),
            imageUrl = "http://example.com/image.jpg",
            shopId = 1L,
            sellerId = 10L,
            status = "PENDING",
            rejectionReason = null,
            createdAt = now,
            updatedAt = now
        )

        assert(response.id == 100L)
        assert(response.name == "Test Product")
        assert(response.price == BigDecimal("99.99"))
        assert(response.imageUrl == "http://example.com/image.jpg")
        assert(response.status == "PENDING")
    }

    @Test
    @DisplayName("ProductResponse should allow null imageUrl")
    fun testProductResponseNullImageUrl() {
        val response = ProductResponse(
            id = 100L,
            name = "Test",
            description = "Test",
            price = BigDecimal("10.00"),
            imageUrl = null,
            shopId = 1L,
            sellerId = 10L,
            status = "PENDING",
            rejectionReason = null,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        assert(response.imageUrl == null)
    }

    @Test
    @DisplayName("ProductResponse copy should work correctly")
    fun testProductResponseCopy() {
        val response = ProductResponse(
            id = 100L,
            name = "Test",
            description = "Test",
            price = BigDecimal("10.00"),
            imageUrl = null,
            shopId = 1L,
            sellerId = 10L,
            status = "PENDING",
            rejectionReason = null,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        val copied = response.copy(status = "APPROVED")

        assert(copied.id == 100L)
        assert(copied.status == "APPROVED")
    }

    @Test
    @DisplayName("UserResponse should create with all fields")
    fun testUserResponseCreation() {
        val now = LocalDateTime.now()
        val response = UserResponse(
            id = 1L,
            username = "testuser",
            email = "test@example.com",
            firstName = "Test",
            lastName = "User",
            roles = setOf("USER", "MODERATOR"),
            createdAt = now,
            updatedAt = now
        )

        assert(response.id == 1L)
        assert(response.username == "testuser")
        assert(response.roles.contains("MODERATOR"))
        assert(response.roles.size == 2)
    }

    @Test
    @DisplayName("ModerationResultResponse should create with all fields")
    fun testModerationResultResponseCreation() {
        val now = LocalDateTime.now()
        val response = ModerationResultResponse(
            productId = 100L,
            productName = "Test Product",
            action = "APPROVE",
            reason = null,
            moderatorId = 1L,
            newStatus = "APPROVED",
            timestamp = now
        )

        assert(response.productId == 100L)
        assert(response.action == "APPROVE")
        assert(response.newStatus == "APPROVED")
        assert(response.reason == null)
    }

    @Test
    @DisplayName("ModerationResultResponse should create with rejection reason")
    fun testModerationResultResponseWithReason() {
        val response = ModerationResultResponse(
            productId = 100L,
            productName = "Test Product",
            action = "REJECT",
            reason = "Policy violation",
            moderatorId = 1L,
            newStatus = "REJECTED",
            timestamp = LocalDateTime.now()
        )

        assert(response.action == "REJECT")
        assert(response.reason == "Policy violation")
        assert(response.newStatus == "REJECTED")
    }

    @Test
    @DisplayName("PaginatedResponse should create with all fields")
    fun testPaginatedResponseCreation() {
        val items = listOf("item1", "item2", "item3")
        val response = PaginatedResponse(
            data = items,
            totalElements = 100,
            totalPages = 10,
            page = 1,
            pageSize = 10
        )

        assert(response.data.size == 3)
        assert(response.totalElements == 100L)
        assert(response.totalPages == 10)
        assert(response.page == 1)
        assert(response.pageSize == 10)
    }

    @Test
    @DisplayName("PaginatedResponse should handle empty data")
    fun testPaginatedResponseEmpty() {
        val response = PaginatedResponse(
            data = emptyList<String>(),
            totalElements = 0,
            totalPages = 0,
            page = 1,
            pageSize = 20
        )

        assert(response.data.isEmpty())
        assert(response.totalElements == 0L)
    }

    @Test
    @DisplayName("ErrorResponse should create with all fields")
    fun testErrorResponseCreation() {
        val now = LocalDateTime.now()
        val response = ErrorResponse(
            message = "Something went wrong",
            status = 500,
            timestamp = now,
            path = "/api/test"
        )

        assert(response.message == "Something went wrong")
        assert(response.status == 500)
        assert(response.path == "/api/test")
    }

    @Test
    @DisplayName("ErrorResponse should handle empty path")
    fun testErrorResponseEmptyPath() {
        val response = ErrorResponse(
            message = "Error",
            status = 400,
            timestamp = LocalDateTime.now(),
            path = ""
        )

        assert(response.path == "")
    }
}
