package ru.itmo.productservice.controller

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
import ru.itmo.productservice.exception.BadRequestException
import ru.itmo.productservice.exception.ForbiddenException
import ru.itmo.productservice.exception.ResourceNotFoundException
import ru.itmo.productservice.model.dto.request.CreateProductRequest
import ru.itmo.productservice.model.dto.request.UpdateProductRequest
import ru.itmo.productservice.model.dto.response.PaginatedResponse
import ru.itmo.productservice.model.dto.response.ProductResponse
import ru.itmo.productservice.service.ProductService
import java.math.BigDecimal
import java.time.LocalDateTime

@WebMvcTest(controllers = [ProductController::class])
@ActiveProfiles("test")
@TestPropertySource(properties = [
    "spring.cloud.config.enabled=false",
    "eureka.client.enabled=false"
])
@org.springframework.context.annotation.Import(ru.itmo.productservice.exception.GlobalExceptionHandler::class)
@DisplayName("ProductController Tests")
class ProductControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockBean
    private lateinit var productService: ProductService

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

    // ==================== GET /api/products ====================

    @Test
    @DisplayName("GET /api/products returns paginated products")
    fun testGetAllProducts() {
        val response = PaginatedResponse(
            data = listOf(testProductResponse),
            page = 1,
            pageSize = 20,
            totalElements = 1L,
            totalPages = 1
        )

        whenever(productService.getApprovedProducts(1, 20)).thenReturn(response)

        mockMvc.perform(get("/api/products"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data[0].name").value("Test Product"))
            .andExpect(jsonPath("$.page").value(1))
    }

    @Test
    @DisplayName("GET /api/products with custom pagination")
    fun testGetAllProductsWithPagination() {
        val response = PaginatedResponse(
            data = listOf(testProductResponse),
            page = 2,
            pageSize = 10,
            totalElements = 15L,
            totalPages = 2
        )

        whenever(productService.getApprovedProducts(2, 10)).thenReturn(response)

        mockMvc.perform(get("/api/products")
            .param("page", "2")
            .param("pageSize", "10"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.page").value(2))
            .andExpect(jsonPath("$.pageSize").value(10))
    }

    @Test
    @DisplayName("GET /api/products returns empty list")
    fun testGetAllProductsEmpty() {
        val response = PaginatedResponse(
            data = emptyList<ProductResponse>(),
            page = 1,
            pageSize = 20,
            totalElements = 0L,
            totalPages = 0
        )

        whenever(productService.getApprovedProducts(1, 20)).thenReturn(response)

        mockMvc.perform(get("/api/products"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").isEmpty)
            .andExpect(jsonPath("$.totalElements").value(0))
    }

    // ==================== GET /api/products/{productId} ====================

    @Test
    @DisplayName("GET /api/products/{id} returns product")
    fun testGetProductById() {
        whenever(productService.getProductById(1L)).thenReturn(testProductResponse)

        mockMvc.perform(get("/api/products/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Test Product"))
    }

    @Test
    @DisplayName("GET /api/products/{id} returns 404 when not found")
    fun testGetProductByIdNotFound() {
        whenever(productService.getProductById(999L))
            .thenThrow(ResourceNotFoundException("Product not found"))

        mockMvc.perform(get("/api/products/999"))
            .andExpect(status().isNotFound)
    }

    // ==================== GET /api/products/search ====================

    @Test
    @DisplayName("GET /api/products/search returns search results")
    fun testSearchProducts() {
        val response = PaginatedResponse(
            data = listOf(testProductResponse),
            page = 1,
            pageSize = 20,
            totalElements = 1L,
            totalPages = 1
        )

        whenever(productService.searchProducts("test", 1, 20)).thenReturn(response)

        mockMvc.perform(get("/api/products/search")
            .param("keywords", "test"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data[0].name").value("Test Product"))
    }

    @Test
    @DisplayName("GET /api/products/search with empty keyword returns 400")
    fun testSearchProductsEmptyKeyword() {
        whenever(productService.searchProducts("", 1, 20))
            .thenThrow(BadRequestException("Search keyword cannot be empty"))

        mockMvc.perform(get("/api/products/search")
            .param("keywords", ""))
            .andExpect(status().isBadRequest)
    }

    // ==================== POST /api/products ====================

    @Test
    @DisplayName("POST /api/products creates product")
    fun testCreateProduct() {
        val request = CreateProductRequest(
            name = "New Product",
            description = "Description",
            price = BigDecimal("50.00"),
            imageUrl = null,
            shopId = 1L
        )

        val createdProduct = testProductResponse.copy(
            id = 2L,
            name = "New Product",
            status = "PENDING"
        )

        whenever(productService.createProduct(any(), eq(1L))).thenReturn(createdProduct)

        mockMvc.perform(post("/api/products")
            .header("X-User-Id", "1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.name").value("New Product"))
            .andExpect(jsonPath("$.status").value("PENDING"))
    }

    @Test
    @DisplayName("POST /api/products returns 404 when shop not found")
    fun testCreateProductShopNotFound() {
        val request = CreateProductRequest(
            name = "Product",
            description = null,
            price = BigDecimal("10.00"),
            imageUrl = null,
            shopId = 999L
        )

        whenever(productService.createProduct(any(), eq(1L)))
            .thenThrow(ResourceNotFoundException("Shop not found"))

        mockMvc.perform(post("/api/products")
            .header("X-User-Id", "1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound)
    }

    @Test
    @DisplayName("POST /api/products returns 403 when not shop owner")
    fun testCreateProductNotShopOwner() {
        val request = CreateProductRequest(
            name = "Product",
            description = null,
            price = BigDecimal("10.00"),
            imageUrl = null,
            shopId = 1L
        )

        whenever(productService.createProduct(any(), eq(999L)))
            .thenThrow(ForbiddenException("Only shop owner can add products"))

        mockMvc.perform(post("/api/products")
            .header("X-User-Id", "999")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden)
    }

    // ==================== PUT /api/products/{productId} ====================

    @Test
    @DisplayName("PUT /api/products/{id} updates product")
    fun testUpdateProduct() {
        val request = UpdateProductRequest(
            name = "Updated Name",
            description = "Updated Description",
            price = BigDecimal("200.00"),
            imageUrl = null
        )

        val updatedProduct = testProductResponse.copy(
            name = "Updated Name",
            description = "Updated Description",
            price = BigDecimal("200.00")
        )

        whenever(productService.updateProduct(eq(1L), eq(2L), any())).thenReturn(updatedProduct)

        mockMvc.perform(put("/api/products/1")
            .header("X-User-Id", "2")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Updated Name"))
    }

    @Test
    @DisplayName("PUT /api/products/{id} returns 403 when not moderator")
    fun testUpdateProductNotModerator() {
        val request = UpdateProductRequest(name = "Updated")

        whenever(productService.updateProduct(eq(1L), eq(1L), any()))
            .thenThrow(ForbiddenException("Only moderators can update products"))

        mockMvc.perform(put("/api/products/1")
            .header("X-User-Id", "1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden)
    }

    @Test
    @DisplayName("PUT /api/products/{id} returns 404 when not found")
    fun testUpdateProductNotFound() {
        val request = UpdateProductRequest(name = "Updated")

        whenever(productService.updateProduct(eq(999L), eq(2L), any()))
            .thenThrow(ResourceNotFoundException("Product not found"))

        mockMvc.perform(put("/api/products/999")
            .header("X-User-Id", "2")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound)
    }

    // ==================== DELETE /api/products/{productId} ====================

    @Test
    @DisplayName("DELETE /api/products/{id} deletes product")
    fun testDeleteProduct() {
        doNothing().whenever(productService).deleteProduct(1L, 1L)

        mockMvc.perform(delete("/api/products/1")
            .header("X-User-Id", "1"))
            .andExpect(status().isNoContent)

        verify(productService).deleteProduct(1L, 1L)
    }

    @Test
    @DisplayName("DELETE /api/products/{id} returns 404 when not found")
    fun testDeleteProductNotFound() {
        doThrow(ResourceNotFoundException("Product not found"))
            .whenever(productService).deleteProduct(999L, 1L)

        mockMvc.perform(delete("/api/products/999")
            .header("X-User-Id", "1"))
            .andExpect(status().isNotFound)
    }

    @Test
    @DisplayName("DELETE /api/products/{id} returns 403 when not authorized")
    fun testDeleteProductNotAuthorized() {
        doThrow(ForbiddenException("Not authorized"))
            .whenever(productService).deleteProduct(1L, 999L)

        mockMvc.perform(delete("/api/products/1")
            .header("X-User-Id", "999"))
            .andExpect(status().isForbidden)
    }

    // ==================== GET /api/products/pending ====================

    @Test
    @DisplayName("GET /api/products/pending returns pending products")
    fun testGetPendingProducts() {
        val pendingProduct = testProductResponse.copy(status = "PENDING")
        val response = PaginatedResponse(
            data = listOf(pendingProduct),
            page = 1,
            pageSize = 20,
            totalElements = 1L,
            totalPages = 1
        )

        whenever(productService.getPendingProducts(1, 20)).thenReturn(response)

        mockMvc.perform(get("/api/products/pending"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].status").value("PENDING"))
    }

    // ==================== GET /api/products/pending/{id} ====================

    @Test
    @DisplayName("GET /api/products/pending/{id} returns pending product")
    fun testGetPendingProductById() {
        val pendingProduct = testProductResponse.copy(status = "PENDING")

        whenever(productService.getPendingProductById(1L)).thenReturn(pendingProduct)

        mockMvc.perform(get("/api/products/pending/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("PENDING"))
    }

    @Test
    @DisplayName("GET /api/products/pending/{id} returns 404 when not found")
    fun testGetPendingProductByIdNotFound() {
        whenever(productService.getPendingProductById(999L))
            .thenThrow(ResourceNotFoundException("Pending product not found"))

        mockMvc.perform(get("/api/products/pending/999"))
            .andExpect(status().isNotFound)
    }

    // ==================== POST /api/products/{id}/approve ====================

    @Test
    @DisplayName("POST /api/products/{id}/approve approves product")
    fun testApproveProduct() {
        val approvedProduct = testProductResponse.copy(status = "APPROVED")

        whenever(productService.approveProduct(1L, 2L)).thenReturn(approvedProduct)

        mockMvc.perform(post("/api/products/1/approve")
            .header("X-User-Id", "2"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("APPROVED"))
    }

    @Test
    @DisplayName("POST /api/products/{id}/approve returns 403 when not moderator")
    fun testApproveProductNotModerator() {
        whenever(productService.approveProduct(1L, 1L))
            .thenThrow(ForbiddenException("Only moderators can approve"))

        mockMvc.perform(post("/api/products/1/approve")
            .header("X-User-Id", "1"))
            .andExpect(status().isForbidden)
    }

    @Test
    @DisplayName("POST /api/products/{id}/approve returns 404 when not found")
    fun testApproveProductNotFound() {
        whenever(productService.approveProduct(999L, 2L))
            .thenThrow(ResourceNotFoundException("Product not found"))

        mockMvc.perform(post("/api/products/999/approve")
            .header("X-User-Id", "2"))
            .andExpect(status().isNotFound)
    }

    // ==================== POST /api/products/{id}/reject ====================

    @Test
    @DisplayName("POST /api/products/{id}/reject rejects product")
    fun testRejectProduct() {
        val rejectedProduct = testProductResponse.copy(
            status = "REJECTED",
            rejectionReason = "Inappropriate content"
        )

        whenever(productService.rejectProduct(1L, 2L, "Inappropriate content"))
            .thenReturn(rejectedProduct)

        mockMvc.perform(post("/api/products/1/reject")
            .header("X-User-Id", "2")
            .param("reason", "Inappropriate content"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("REJECTED"))
            .andExpect(jsonPath("$.rejectionReason").value("Inappropriate content"))
    }

    @Test
    @DisplayName("POST /api/products/{id}/reject returns 403 when not moderator")
    fun testRejectProductNotModerator() {
        whenever(productService.rejectProduct(1L, 1L, "Reason"))
            .thenThrow(ForbiddenException("Only moderators can reject"))

        mockMvc.perform(post("/api/products/1/reject")
            .header("X-User-Id", "1")
            .param("reason", "Reason"))
            .andExpect(status().isForbidden)
    }

    @Test
    @DisplayName("POST /api/products/{id}/reject returns 400 when blank reason")
    fun testRejectProductBlankReason() {
        whenever(productService.rejectProduct(1L, 2L, ""))
            .thenThrow(BadRequestException("Rejection reason cannot be empty"))

        mockMvc.perform(post("/api/products/1/reject")
            .header("X-User-Id", "2")
            .param("reason", ""))
            .andExpect(status().isBadRequest)
    }
}
