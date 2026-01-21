package ru.itmo.market.controller

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import ru.itmo.market.adapters.exception.ForbiddenException
import ru.itmo.market.adapters.exception.ResourceNotFoundException
import ru.itmo.market.application.dto.request.BulkModerationRequest
import ru.itmo.market.application.dto.request.RejectProductRequest
import ru.itmo.market.application.dto.response.ModerationResultResponse
import ru.itmo.market.application.dto.response.PaginatedResponse
import ru.itmo.market.application.dto.response.ProductResponse
import ru.itmo.market.domain.entity.ModerationAction
import ru.itmo.market.domain.entity.ModerationAudit
import ru.itmo.market.application.service.ModerationService
import ru.itmo.market.presentation.controller.ModerationController
import java.math.BigDecimal
import java.time.Duration
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
@DisplayName("Moderation Controller Tests")
class ModerationControllerTest {

    @Mock
    private lateinit var moderationService: ModerationService

    private lateinit var controller: ModerationController

    private val testProduct = ProductResponse(
        id = 100L,
        name = "Test Product",
        description = "Test Description",
        price = BigDecimal("99.99"),
        imageUrl = null,
        shopId = 1L,
        sellerId = 10L,
        status = "PENDING",
        rejectionReason = null,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )

    private val approveResult = ModerationResultResponse(
        productId = 100L,
        productName = "Test Product",
        action = "APPROVE",
        reason = null,
        moderatorId = 1L,
        newStatus = "APPROVED",
        timestamp = LocalDateTime.now()
    )

    private val rejectResult = ModerationResultResponse(
        productId = 100L,
        productName = "Test Product",
        action = "REJECT",
        reason = "Low quality",
        moderatorId = 1L,
        newStatus = "REJECTED",
        timestamp = LocalDateTime.now()
    )

    @BeforeEach
    fun setUp() {
        controller = ModerationController(moderationService)
    }

    // ==================== getPendingProducts Tests ====================

    @Test
    @DisplayName("GET /products - Should return pending products")
    fun testGetPendingProducts() {
        val paginatedResponse = PaginatedResponse(
            data = listOf(testProduct),
            totalElements = 1,
            totalPages = 1,
            page = 1,
            pageSize = 20
        )

        whenever(moderationService.getPendingProducts(1L, 1, 20))
            .thenReturn(Mono.just(paginatedResponse))

        StepVerifier.create(controller.getPendingProducts(1L, 1, 20))
            .assertNext { response ->
                assert(response.statusCode == HttpStatus.OK)
                assert(response.body?.data?.size == 1)
            }
            .expectComplete()
            .verify(Duration.ofSeconds(5))
    }

    @Test
    @DisplayName("GET /products - Should return 403 for non-moderator")
    fun testGetPendingProductsForbidden() {
        whenever(moderationService.getPendingProducts(3L, 1, 20))
            .thenReturn(Mono.error(ForbiddenException("User is not a moderator")))

        StepVerifier.create(controller.getPendingProducts(3L, 1, 20))
            .expectError(ForbiddenException::class.java)
            .verify(Duration.ofSeconds(5))
    }

    // ==================== getPendingProductById Tests ====================

    @Test
    @DisplayName("GET /products/{id} - Should return product")
    fun testGetPendingProductById() {
        whenever(moderationService.getPendingProductById(1L, 100L))
            .thenReturn(Mono.just(testProduct))

        StepVerifier.create(controller.getPendingProductById(1L, 100L))
            .assertNext { response ->
                assert(response.statusCode == HttpStatus.OK)
                assert(response.body?.id == 100L)
            }
            .expectComplete()
            .verify(Duration.ofSeconds(5))
    }

    @Test
    @DisplayName("GET /products/{id} - Should return 403 for non-moderator")
    fun testGetPendingProductByIdForbidden() {
        whenever(moderationService.getPendingProductById(3L, 100L))
            .thenReturn(Mono.error(ForbiddenException("User is not a moderator")))

        StepVerifier.create(controller.getPendingProductById(3L, 100L))
            .expectError(ForbiddenException::class.java)
            .verify(Duration.ofSeconds(5))
    }

    @Test
    @DisplayName("GET /products/{id} - Should return 404 for non-existent product")
    fun testGetPendingProductByIdNotFound() {
        whenever(moderationService.getPendingProductById(1L, 999L))
            .thenReturn(Mono.error(ResourceNotFoundException("Product not found")))

        StepVerifier.create(controller.getPendingProductById(1L, 999L))
            .expectError(ResourceNotFoundException::class.java)
            .verify(Duration.ofSeconds(5))
    }

    // ==================== approveProduct Tests ====================

    @Test
    @DisplayName("POST /products/{id}/approve - Should approve product")
    fun testApproveProduct() {
        whenever(moderationService.approveProduct(1L, 100L))
            .thenReturn(Mono.just(approveResult))

        StepVerifier.create(controller.approveProduct(1L, 100L))
            .assertNext { response ->
                assert(response.statusCode == HttpStatus.OK)
                assert(response.body?.action == "APPROVE")
                assert(response.body?.newStatus == "APPROVED")
            }
            .expectComplete()
            .verify(Duration.ofSeconds(5))
    }

    @Test
    @DisplayName("POST /products/{id}/approve - Should return 403 for non-moderator")
    fun testApproveProductForbidden() {
        whenever(moderationService.approveProduct(3L, 100L))
            .thenReturn(Mono.error(ForbiddenException("User is not a moderator")))

        StepVerifier.create(controller.approveProduct(3L, 100L))
            .expectError(ForbiddenException::class.java)
            .verify(Duration.ofSeconds(5))
    }

    // ==================== rejectProduct Tests ====================

    @Test
    @DisplayName("POST /products/{id}/reject - Should reject product")
    fun testRejectProduct() {
        val request = RejectProductRequest(reason = "Low quality")

        whenever(moderationService.rejectProduct(1L, 100L, "Low quality"))
            .thenReturn(Mono.just(rejectResult))

        StepVerifier.create(controller.rejectProduct(1L, 100L, request))
            .assertNext { response ->
                assert(response.statusCode == HttpStatus.OK)
                assert(response.body?.action == "REJECT")
                assert(response.body?.reason == "Low quality")
            }
            .expectComplete()
            .verify(Duration.ofSeconds(5))
    }

    @Test
    @DisplayName("POST /products/{id}/reject - Should return 403 for non-moderator")
    fun testRejectProductForbidden() {
        val request = RejectProductRequest(reason = "Bad")

        whenever(moderationService.rejectProduct(3L, 100L, "Bad"))
            .thenReturn(Mono.error(ForbiddenException("User is not a moderator")))

        StepVerifier.create(controller.rejectProduct(3L, 100L, request))
            .expectError(ForbiddenException::class.java)
            .verify(Duration.ofSeconds(5))
    }

    // ==================== bulkModerate Tests ====================

    @Test
    @DisplayName("POST /bulk - Should bulk approve products")
    fun testBulkModerate() {
        val request = BulkModerationRequest(
            productIds = listOf(100L, 101L),
            action = "APPROVE",
            reason = null
        )

        val result1 = approveResult.copy(productId = 100L)
        val result2 = approveResult.copy(productId = 101L)

        whenever(moderationService.bulkModerate(1L, request))
            .thenReturn(Flux.just(result1, result2))

        StepVerifier.create(controller.bulkModerate(1L, request))
            .assertNext { result ->
                assert(result.productId == 100L)
            }
            .assertNext { result ->
                assert(result.productId == 101L)
            }
            .expectComplete()
            .verify(Duration.ofSeconds(5))
    }

    // ==================== getModerationHistory Tests ====================

    @Test
    @DisplayName("GET /history - Should return moderation history")
    fun testGetModerationHistory() {
        val actions = listOf(
            ModerationAction(id = 1L, productId = 100L, moderatorId = 1L, actionType = "APPROVE"),
            ModerationAction(id = 2L, productId = 101L, moderatorId = 1L, actionType = "REJECT", reason = "Bad")
        )

        whenever(moderationService.getModerationHistory(1L))
            .thenReturn(Flux.fromIterable(actions))

        StepVerifier.create(controller.getModerationHistory(1L))
            .assertNext { action ->
                assert(action.id == 1L)
            }
            .assertNext { action ->
                assert(action.id == 2L)
            }
            .expectComplete()
            .verify(Duration.ofSeconds(5))
    }

    // ==================== getProductModerationHistory Tests ====================

    @Test
    @DisplayName("GET /products/{id}/history - Should return product history")
    fun testGetProductModerationHistory() {
        val audits = listOf(
            ModerationAudit(id = 1L, actionId = 1L, productId = 100L, moderatorId = 1L, oldStatus = "PENDING", newStatus = "APPROVED")
        )

        whenever(moderationService.getProductModerationHistory(100L))
            .thenReturn(Flux.fromIterable(audits))

        StepVerifier.create(controller.getProductModerationHistory(100L))
            .assertNext { audit ->
                assert(audit.productId == 100L)
            }
            .expectComplete()
            .verify(Duration.ofSeconds(5))
    }

    // ==================== Additional Edge Case Tests ====================

    @Test
    @DisplayName("GET /products - Should return empty list when no pending products")
    fun testGetPendingProductsEmpty() {
        val emptyResponse = PaginatedResponse(
            data = emptyList<ProductResponse>(),
            totalElements = 0,
            totalPages = 0,
            page = 1,
            pageSize = 20
        )

        whenever(moderationService.getPendingProducts(1L, 1, 20))
            .thenReturn(Mono.just(emptyResponse))

        StepVerifier.create(controller.getPendingProducts(1L, 1, 20))
            .assertNext { response ->
                assert(response.statusCode == HttpStatus.OK)
                assert(response.body?.data?.isEmpty() == true)
            }
            .expectComplete()
            .verify(Duration.ofSeconds(5))
    }

    @Test
    @DisplayName("GET /products - Should handle pagination parameters")
    fun testGetPendingProductsPagination() {
        val paginatedResponse = PaginatedResponse(
            data = listOf(testProduct),
            totalElements = 50,
            totalPages = 5,
            page = 3,
            pageSize = 10
        )

        whenever(moderationService.getPendingProducts(1L, 3, 10))
            .thenReturn(Mono.just(paginatedResponse))

        StepVerifier.create(controller.getPendingProducts(1L, 3, 10))
            .assertNext { response ->
                assert(response.statusCode == HttpStatus.OK)
                assert(response.body?.page == 3)
                assert(response.body?.pageSize == 10)
                assert(response.body?.totalPages == 5)
            }
            .expectComplete()
            .verify(Duration.ofSeconds(5))
    }

    @Test
    @DisplayName("POST /products/{id}/approve - Should return 404 for non-existent product")
    fun testApproveProductNotFound() {
        whenever(moderationService.approveProduct(1L, 999L))
            .thenReturn(Mono.error(ResourceNotFoundException("Product not found")))

        StepVerifier.create(controller.approveProduct(1L, 999L))
            .expectError(ResourceNotFoundException::class.java)
            .verify(Duration.ofSeconds(5))
    }

    @Test
    @DisplayName("POST /products/{id}/reject - Should return 404 for non-existent product")
    fun testRejectProductNotFound() {
        val request = RejectProductRequest(reason = "Bad product")

        whenever(moderationService.rejectProduct(1L, 999L, "Bad product"))
            .thenReturn(Mono.error(ResourceNotFoundException("Product not found")))

        StepVerifier.create(controller.rejectProduct(1L, 999L, request))
            .expectError(ResourceNotFoundException::class.java)
            .verify(Duration.ofSeconds(5))
    }

    @Test
    @DisplayName("POST /bulk - Should bulk reject products with reason")
    fun testBulkModerateReject() {
        val request = BulkModerationRequest(
            productIds = listOf(100L, 101L),
            action = "REJECT",
            reason = "Policy violation"
        )

        val result1 = rejectResult.copy(productId = 100L, reason = "Policy violation")
        val result2 = rejectResult.copy(productId = 101L, reason = "Policy violation")

        whenever(moderationService.bulkModerate(1L, request))
            .thenReturn(Flux.just(result1, result2))

        StepVerifier.create(controller.bulkModerate(1L, request))
            .assertNext { result ->
                assert(result.productId == 100L)
                assert(result.action == "REJECT")
            }
            .assertNext { result ->
                assert(result.productId == 101L)
                assert(result.action == "REJECT")
            }
            .expectComplete()
            .verify(Duration.ofSeconds(5))
    }

    @Test
    @DisplayName("POST /bulk - Should return 403 for non-moderator")
    fun testBulkModerateForbidden() {
        val request = BulkModerationRequest(
            productIds = listOf(100L),
            action = "APPROVE",
            reason = null
        )

        whenever(moderationService.bulkModerate(3L, request))
            .thenReturn(Flux.error(ForbiddenException("User is not a moderator")))

        StepVerifier.create(controller.bulkModerate(3L, request))
            .expectError(ForbiddenException::class.java)
            .verify(Duration.ofSeconds(5))
    }

    @Test
    @DisplayName("GET /history - Should return empty history")
    fun testGetModerationHistoryEmpty() {
        whenever(moderationService.getModerationHistory(999L))
            .thenReturn(Flux.empty())

        StepVerifier.create(controller.getModerationHistory(999L))
            .expectComplete()
            .verify(Duration.ofSeconds(5))
    }

    @Test
    @DisplayName("GET /products/{id}/history - Should return empty product history")
    fun testGetProductModerationHistoryEmpty() {
        whenever(moderationService.getProductModerationHistory(999L))
            .thenReturn(Flux.empty())

        StepVerifier.create(controller.getProductModerationHistory(999L))
            .expectComplete()
            .verify(Duration.ofSeconds(5))
    }

    @Test
    @DisplayName("GET /products/{id}/history - Should return multiple audits")
    fun testGetProductModerationHistoryMultiple() {
        val audits = listOf(
            ModerationAudit(id = 1L, actionId = 1L, productId = 100L, moderatorId = 1L, oldStatus = "PENDING", newStatus = "APPROVED"),
            ModerationAudit(id = 2L, actionId = 2L, productId = 100L, moderatorId = 2L, oldStatus = "APPROVED", newStatus = "REJECTED"),
            ModerationAudit(id = 3L, actionId = 3L, productId = 100L, moderatorId = 1L, oldStatus = "REJECTED", newStatus = "APPROVED")
        )

        whenever(moderationService.getProductModerationHistory(100L))
            .thenReturn(Flux.fromIterable(audits))

        StepVerifier.create(controller.getProductModerationHistory(100L))
            .expectNextCount(3)
            .expectComplete()
            .verify(Duration.ofSeconds(5))
    }

    @Test
    @DisplayName("GET /history - Should return multiple actions")
    fun testGetModerationHistoryMultiple() {
        val actions = (1..5).map { i ->
            ModerationAction(
                id = i.toLong(),
                productId = (100 + i).toLong(),
                moderatorId = 1L,
                actionType = if (i % 2 == 0) "APPROVE" else "REJECT",
                reason = if (i % 2 == 1) "Reason $i" else null
            )
        }

        whenever(moderationService.getModerationHistory(1L))
            .thenReturn(Flux.fromIterable(actions))

        StepVerifier.create(controller.getModerationHistory(1L))
            .expectNextCount(5)
            .expectComplete()
            .verify(Duration.ofSeconds(5))
    }
}
