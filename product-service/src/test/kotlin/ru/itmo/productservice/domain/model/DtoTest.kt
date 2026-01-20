package ru.itmo.productservice.domain.model

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import ru.itmo.productservice.domain.model.dto.request.CreateProductRequest
import ru.itmo.productservice.domain.model.dto.request.CreateShopRequest
import ru.itmo.productservice.domain.model.dto.request.UpdateProductRequest
import ru.itmo.productservice.domain.model.dto.request.UpdateShopRequest
import ru.itmo.productservice.domain.model.dto.response.PaginatedResponse
import ru.itmo.productservice.domain.model.dto.response.ProductResponse
import ru.itmo.productservice.domain.model.dto.response.ShopResponse
import ru.itmo.productservice.domain.model.dto.response.UserResponse
import java.math.BigDecimal
import java.time.LocalDateTime

@DisplayName("DTO Tests")
class DtoTest {

    // ==================== CreateProductRequest Tests ====================

    @Test
    @DisplayName("CreateProductRequest should store all fields")
    fun testCreateProductRequest() {
        val request = CreateProductRequest(
            name = "Test Product",
            description = "Description",
            price = BigDecimal("100.00"),
            imageUrl = "http://image.url",
            shopId = 1L
        )

        assert(request.name == "Test Product")
        assert(request.description == "Description")
        assert(request.price == BigDecimal("100.00"))
        assert(request.imageUrl == "http://image.url")
        assert(request.shopId == 1L)
    }

    @Test
    @DisplayName("CreateProductRequest should allow null optional fields")
    fun testCreateProductRequestNullOptionals() {
        val request = CreateProductRequest(
            name = "Product",
            price = BigDecimal("50.00"),
            shopId = 1L
        )

        assert(request.description == null)
        assert(request.imageUrl == null)
    }

    @Test
    @DisplayName("CreateProductRequest data class equality")
    fun testCreateProductRequestEquality() {
        val request1 = CreateProductRequest("Product", "Desc", BigDecimal("10.00"), null, 1L)
        val request2 = CreateProductRequest("Product", "Desc", BigDecimal("10.00"), null, 1L)

        assert(request1 == request2)
        assert(request1.hashCode() == request2.hashCode())
    }

    @Test
    @DisplayName("CreateProductRequest copy function")
    fun testCreateProductRequestCopy() {
        val original = CreateProductRequest("Product", "Desc", BigDecimal("10.00"), null, 1L)
        val copy = original.copy(name = "Updated")

        assert(copy.name == "Updated")
        assert(copy.description == original.description)
    }

    // ==================== CreateShopRequest Tests ====================

    @Test
    @DisplayName("CreateShopRequest should store all fields")
    fun testCreateShopRequest() {
        val request = CreateShopRequest(
            name = "Test Shop",
            description = "Description",
            avatarUrl = "http://avatar.url"
        )

        assert(request.name == "Test Shop")
        assert(request.description == "Description")
        assert(request.avatarUrl == "http://avatar.url")
    }

    @Test
    @DisplayName("CreateShopRequest should allow null optional fields")
    fun testCreateShopRequestNullOptionals() {
        val request = CreateShopRequest(name = "Shop")

        assert(request.description == null)
        assert(request.avatarUrl == null)
    }

    // ==================== UpdateProductRequest Tests ====================

    @Test
    @DisplayName("UpdateProductRequest should allow all fields null")
    fun testUpdateProductRequestAllNull() {
        val request = UpdateProductRequest()

        assert(request.name == null)
        assert(request.description == null)
        assert(request.price == null)
        assert(request.imageUrl == null)
    }

    @Test
    @DisplayName("UpdateProductRequest should store specified fields")
    fun testUpdateProductRequestPartial() {
        val request = UpdateProductRequest(
            name = "Updated Name",
            price = BigDecimal("200.00")
        )

        assert(request.name == "Updated Name")
        assert(request.price == BigDecimal("200.00"))
        assert(request.description == null)
    }

    // ==================== UpdateShopRequest Tests ====================

    @Test
    @DisplayName("UpdateShopRequest should allow all fields null")
    fun testUpdateShopRequestAllNull() {
        val request = UpdateShopRequest()

        assert(request.name == null)
        assert(request.description == null)
        assert(request.avatarUrl == null)
    }

    @Test
    @DisplayName("UpdateShopRequest should store specified fields")
    fun testUpdateShopRequestPartial() {
        val request = UpdateShopRequest(name = "Updated Name")

        assert(request.name == "Updated Name")
        assert(request.description == null)
    }

    // ==================== PaginatedResponse Tests ====================

    @Test
    @DisplayName("PaginatedResponse should store pagination info")
    fun testPaginatedResponse() {
        val response = PaginatedResponse(
            data = listOf("item1", "item2"),
            page = 1,
            pageSize = 10,
            totalElements = 2L,
            totalPages = 1
        )

        assert(response.data.size == 2)
        assert(response.page == 1)
        assert(response.pageSize == 10)
        assert(response.totalElements == 2L)
        assert(response.totalPages == 1)
    }

    @Test
    @DisplayName("PaginatedResponse should work with empty list")
    fun testPaginatedResponseEmpty() {
        val response = PaginatedResponse(
            data = emptyList<String>(),
            page = 1,
            pageSize = 10,
            totalElements = 0L,
            totalPages = 0
        )

        assert(response.data.isEmpty())
        assert(response.totalElements == 0L)
    }

    @Test
    @DisplayName("PaginatedResponse generic type works")
    fun testPaginatedResponseGeneric() {
        val intResponse = PaginatedResponse(
            data = listOf(1, 2, 3),
            page = 1,
            pageSize = 10,
            totalElements = 3L,
            totalPages = 1
        )

        assert(intResponse.data[0] == 1)
    }

    // ==================== ProductResponse Tests ====================

    @Test
    @DisplayName("ProductResponse should store all fields")
    fun testProductResponse() {
        val now = LocalDateTime.now()
        val response = ProductResponse(
            id = 1L,
            name = "Product",
            description = "Description",
            price = BigDecimal("100.00"),
            imageUrl = "http://image.url",
            shopId = 1L,
            sellerId = 1L,
            status = "APPROVED",
            rejectionReason = null,
            createdAt = now,
            updatedAt = now
        )

        assert(response.id == 1L)
        assert(response.name == "Product")
        assert(response.status == "APPROVED")
        assert(response.rejectionReason == null)
    }

    @Test
    @DisplayName("ProductResponse should store rejection reason")
    fun testProductResponseWithRejection() {
        val now = LocalDateTime.now()
        val response = ProductResponse(
            id = 1L,
            name = "Product",
            description = null,
            price = BigDecimal("50.00"),
            imageUrl = null,
            shopId = 1L,
            sellerId = 1L,
            status = "REJECTED",
            rejectionReason = "Inappropriate content",
            createdAt = now,
            updatedAt = now
        )

        assert(response.status == "REJECTED")
        assert(response.rejectionReason == "Inappropriate content")
    }

    @Test
    @DisplayName("ProductResponse copy function")
    fun testProductResponseCopy() {
        val now = LocalDateTime.now()
        val original = ProductResponse(
            id = 1L,
            name = "Product",
            description = null,
            price = BigDecimal("50.00"),
            imageUrl = null,
            shopId = 1L,
            sellerId = 1L,
            status = "PENDING",
            rejectionReason = null,
            createdAt = now,
            updatedAt = now
        )

        val approved = original.copy(status = "APPROVED")

        assert(approved.status == "APPROVED")
        assert(approved.id == original.id)
    }

    // ==================== ShopResponse Tests ====================

    @Test
    @DisplayName("ShopResponse should store all fields")
    fun testShopResponse() {
        val now = LocalDateTime.now()
        val response = ShopResponse(
            id = 1L,
            name = "Shop",
            description = "Description",
            avatarUrl = "http://avatar.url",
            sellerId = 1L,
            sellerName = "John Doe",
            productsCount = 10L,
            createdAt = now,
            updatedAt = now
        )

        assert(response.id == 1L)
        assert(response.name == "Shop")
        assert(response.sellerName == "John Doe")
        assert(response.productsCount == 10L)
    }

    @Test
    @DisplayName("ShopResponse should allow null sellerName")
    fun testShopResponseNullSellerName() {
        val now = LocalDateTime.now()
        val response = ShopResponse(
            id = 1L,
            name = "Shop",
            description = null,
            avatarUrl = null,
            sellerId = 1L,
            sellerName = null,
            productsCount = 0L,
            createdAt = now,
            updatedAt = now
        )

        assert(response.sellerName == null)
    }

    // ==================== UserResponse Tests ====================

    @Test
    @DisplayName("UserResponse should store all fields")
    fun testUserResponse() {
        val now = LocalDateTime.now()
        val response = UserResponse(
            id = 1L,
            username = "john_doe",
            email = "user@test.com",
            firstName = "John",
            lastName = "Doe",
            roles = setOf("USER", "SELLER"),
            createdAt = now,
            updatedAt = now
        )

        assert(response.id == 1L)
        assert(response.username == "john_doe")
        assert(response.email == "user@test.com")
        assert(response.firstName == "John")
        assert(response.lastName == "Doe")
        assert(response.roles.size == 2)
    }

    @Test
    @DisplayName("UserResponse should have empty roles set")
    fun testUserResponseEmptyRoles() {
        val now = LocalDateTime.now()
        val response = UserResponse(
            id = 1L,
            username = "user",
            email = "user@test.com",
            firstName = "John",
            lastName = "Doe",
            roles = emptySet(),
            createdAt = now,
            updatedAt = now
        )

        assert(response.roles.isEmpty())
    }

    @Test
    @DisplayName("UserResponse roles contains check")
    fun testUserResponseRolesContains() {
        val now = LocalDateTime.now()
        val response = UserResponse(
            id = 1L,
            username = "moderator",
            email = "mod@test.com",
            firstName = "Mod",
            lastName = "User",
            roles = setOf("MODERATOR"),
            createdAt = now,
            updatedAt = now
        )

        assert(response.roles.contains("MODERATOR"))
        assert(!response.roles.contains("ADMIN"))
    }
}
