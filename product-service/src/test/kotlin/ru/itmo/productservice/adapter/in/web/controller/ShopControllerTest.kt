package ru.itmo.productservice.adapter.`in`.web.controller

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
import ru.itmo.productservice.adapter.exception.BadRequestException
import ru.itmo.productservice.adapter.exception.ForbiddenException
import ru.itmo.productservice.adapter.exception.ResourceNotFoundException
import ru.itmo.productservice.adapter.`in`.web.ShopController
import ru.itmo.productservice.domain.model.dto.request.CreateShopRequest
import ru.itmo.productservice.domain.model.dto.request.UpdateShopRequest
import ru.itmo.productservice.domain.model.dto.response.PaginatedResponse
import ru.itmo.productservice.domain.model.dto.response.ProductResponse
import ru.itmo.productservice.domain.model.dto.response.ShopResponse
import ru.itmo.productservice.application.service.ShopService
import java.math.BigDecimal
import java.time.LocalDateTime

@WebMvcTest(controllers = [ShopController::class])
@ActiveProfiles("test")
@TestPropertySource(properties = [
    "spring.cloud.config.enabled=false",
    "eureka.client.enabled=false"
])
@org.springframework.context.annotation.Import(ru.itmo.productservice.adapter.exception.GlobalExceptionHandler::class)
@DisplayName("ShopController Tests")
class ShopControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockBean
    private lateinit var shopService: ShopService

    private val testShopResponse = ShopResponse(
        id = 1L,
        name = "Test Shop",
        description = "Test Description",
        avatarUrl = "http://avatar.url",
        sellerId = 1L,
        sellerName = "Seller User",
        productsCount = 5L,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )

    private val testProductResponse = ProductResponse(
        id = 1L,
        name = "Test Product",
        description = "Test Description",
        price = BigDecimal("100.00"),
        imageUrl = "http://image.url",
        shopId = 1L,
        sellerId = 1L,
        status = "APPROVED",
        rejectionReason = null,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )

    // ==================== GET /api/shops ====================

    @Test
    @DisplayName("GET /api/shops returns paginated shops")
    fun testGetAllShops() {
        val response = PaginatedResponse(
            data = listOf(testShopResponse),
            page = 1,
            pageSize = 20,
            totalElements = 1L,
            totalPages = 1
        )

        whenever(shopService.getAllShops(1, 20)).thenReturn(response)

        mockMvc.perform(get("/api/shops"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data[0].name").value("Test Shop"))
            .andExpect(jsonPath("$.page").value(1))
    }

    @Test
    @DisplayName("GET /api/shops with custom pagination")
    fun testGetAllShopsWithPagination() {
        val response = PaginatedResponse(
            data = listOf(testShopResponse),
            page = 2,
            pageSize = 10,
            totalElements = 15L,
            totalPages = 2
        )

        whenever(shopService.getAllShops(2, 10)).thenReturn(response)

        mockMvc.perform(get("/api/shops")
            .param("page", "2")
            .param("pageSize", "10"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.page").value(2))
            .andExpect(jsonPath("$.pageSize").value(10))
    }

    @Test
    @DisplayName("GET /api/shops returns empty list")
    fun testGetAllShopsEmpty() {
        val response = PaginatedResponse(
            data = emptyList<ShopResponse>(),
            page = 1,
            pageSize = 20,
            totalElements = 0L,
            totalPages = 0
        )

        whenever(shopService.getAllShops(1, 20)).thenReturn(response)

        mockMvc.perform(get("/api/shops"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").isEmpty)
            .andExpect(jsonPath("$.totalElements").value(0))
    }

    // ==================== GET /api/shops/{shopId} ====================

    @Test
    @DisplayName("GET /api/shops/{id} returns shop")
    fun testGetShopById() {
        whenever(shopService.getShopById(1L)).thenReturn(testShopResponse)

        mockMvc.perform(get("/api/shops/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Test Shop"))
            .andExpect(jsonPath("$.sellerName").value("Seller User"))
            .andExpect(jsonPath("$.productsCount").value(5))
    }

    @Test
    @DisplayName("GET /api/shops/{id} returns 404 when not found")
    fun testGetShopByIdNotFound() {
        whenever(shopService.getShopById(999L))
            .thenThrow(ResourceNotFoundException("Shop not found"))

        mockMvc.perform(get("/api/shops/999"))
            .andExpect(status().isNotFound)
    }

    @Test
    @DisplayName("GET /api/shops/{id} returns seller info")
    fun testGetShopByIdReturnsSellerInfo() {
        val shopWithSeller = testShopResponse.copy(
            sellerId = 5L,
            sellerName = "John Seller"
        )
        whenever(shopService.getShopById(1L)).thenReturn(shopWithSeller)

        mockMvc.perform(get("/api/shops/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.sellerId").value(5))
            .andExpect(jsonPath("$.sellerName").value("John Seller"))
    }

    // ==================== POST /api/shops ====================

    @Test
    @DisplayName("POST /api/shops creates shop")
    fun testCreateShop() {
        val request = CreateShopRequest(
            name = "New Shop",
            description = "Description",
            avatarUrl = "http://avatar.url"
        )

        val createdShop = testShopResponse.copy(
            id = 2L,
            name = "New Shop",
            productsCount = 0L
        )

        whenever(shopService.createShop(eq(1L), any())).thenReturn(createdShop)

        mockMvc.perform(post("/api/shops")
            .header("X-User-Id", "1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.name").value("New Shop"))
            .andExpect(jsonPath("$.productsCount").value(0))
    }

    @Test
    @DisplayName("POST /api/shops returns 400 when seller already has shop")
    fun testCreateShopSellerAlreadyHasShop() {
        val request = CreateShopRequest(
            name = "Shop",
            description = null,
            avatarUrl = null
        )

        whenever(shopService.createShop(eq(1L), any()))
            .thenThrow(BadRequestException("Seller already has a shop"))

        mockMvc.perform(post("/api/shops")
            .header("X-User-Id", "1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("POST /api/shops with minimal data creates shop")
    fun testCreateShopMinimal() {
        val request = CreateShopRequest(
            name = "Simple Shop",
            description = null,
            avatarUrl = null
        )

        val createdShop = testShopResponse.copy(
            id = 3L,
            name = "Simple Shop",
            description = null,
            avatarUrl = null,
            productsCount = 0L
        )

        whenever(shopService.createShop(eq(1L), any())).thenReturn(createdShop)

        mockMvc.perform(post("/api/shops")
            .header("X-User-Id", "1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.name").value("Simple Shop"))
    }

    // ==================== PUT /api/shops/{shopId} ====================

    @Test
    @DisplayName("PUT /api/shops/{id} updates shop")
    fun testUpdateShop() {
        val request = UpdateShopRequest(
            name = "Updated Name",
            description = "Updated Description",
            avatarUrl = "http://new-avatar.url"
        )

        val updatedShop = testShopResponse.copy(
            name = "Updated Name",
            description = "Updated Description"
        )

        whenever(shopService.updateShop(eq(1L), eq(1L), any())).thenReturn(updatedShop)

        mockMvc.perform(put("/api/shops/1")
            .header("X-User-Id", "1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Updated Name"))
    }

    @Test
    @DisplayName("PUT /api/shops/{id} returns 404 when not found")
    fun testUpdateShopNotFound() {
        val request = UpdateShopRequest(name = "Updated")

        whenever(shopService.updateShop(eq(999L), eq(1L), any()))
            .thenThrow(ResourceNotFoundException("Shop not found"))

        mockMvc.perform(put("/api/shops/999")
            .header("X-User-Id", "1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound)
    }

    @Test
    @DisplayName("PUT /api/shops/{id} returns 403 when not owner")
    fun testUpdateShopNotOwner() {
        val request = UpdateShopRequest(name = "Updated")

        whenever(shopService.updateShop(eq(1L), eq(999L), any()))
            .thenThrow(ForbiddenException("Only shop owner can update"))

        mockMvc.perform(put("/api/shops/1")
            .header("X-User-Id", "999")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden)
    }

    // ==================== GET /api/shops/{shopId}/products ====================

    @Test
    @DisplayName("GET /api/shops/{id}/products returns products")
    fun testGetShopProducts() {
        val response = PaginatedResponse(
            data = listOf(testProductResponse),
            page = 1,
            pageSize = 20,
            totalElements = 1L,
            totalPages = 1
        )

        whenever(shopService.getShopProducts(1L, 1, 20)).thenReturn(response)

        mockMvc.perform(get("/api/shops/1/products"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data[0].name").value("Test Product"))
    }

    @Test
    @DisplayName("GET /api/shops/{id}/products with pagination")
    fun testGetShopProductsWithPagination() {
        val response = PaginatedResponse(
            data = listOf(testProductResponse),
            page = 2,
            pageSize = 5,
            totalElements = 10L,
            totalPages = 2
        )

        whenever(shopService.getShopProducts(1L, 2, 5)).thenReturn(response)

        mockMvc.perform(get("/api/shops/1/products")
            .param("page", "2")
            .param("pageSize", "5"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.page").value(2))
            .andExpect(jsonPath("$.pageSize").value(5))
    }

    @Test
    @DisplayName("GET /api/shops/{id}/products returns 404 when shop not found")
    fun testGetShopProductsShopNotFound() {
        whenever(shopService.getShopProducts(999L, 1, 20))
            .thenThrow(ResourceNotFoundException("Shop not found"))

        mockMvc.perform(get("/api/shops/999/products"))
            .andExpect(status().isNotFound)
    }

    @Test
    @DisplayName("GET /api/shops/{id}/products returns empty list")
    fun testGetShopProductsEmpty() {
        val response = PaginatedResponse(
            data = emptyList<ProductResponse>(),
            page = 1,
            pageSize = 20,
            totalElements = 0L,
            totalPages = 0
        )

        whenever(shopService.getShopProducts(1L, 1, 20)).thenReturn(response)

        mockMvc.perform(get("/api/shops/1/products"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").isEmpty)
            .andExpect(jsonPath("$.totalElements").value(0))
    }
}
