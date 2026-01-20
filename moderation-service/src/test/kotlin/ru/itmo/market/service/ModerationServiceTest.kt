package ru.itmo.market.application.service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import ru.itmo.market.adapter.out.messaging.kafka.client.KafkaRpcClient
import ru.itmo.market.adapter.exception.ForbiddenException
import ru.itmo.market.domain.model.dto.request.BulkModerationRequest
import ru.itmo.market.domain.model.dto.response.PaginatedResponse
import ru.itmo.market.domain.model.dto.response.ProductResponse
import ru.itmo.market.domain.model.dto.response.UserResponse
import ru.itmo.market.domain.model.entity.ModerationAction
import ru.itmo.market.domain.model.entity.ModerationAudit
import ru.itmo.market.domain.repository.ModerationActionRepository
import ru.itmo.market.domain.repository.ModerationAuditRepository
import java.math.BigDecimal
import java.time.Duration
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
@DisplayName("Moderation Service Tests")
class ModerationServiceTest {

    @Mock
    private lateinit var kafkaRpcClient: KafkaRpcClient

    @Mock
    private lateinit var moderationActionRepository: ModerationActionRepository

    @Mock
    private lateinit var moderationAuditRepository: ModerationAuditRepository

    private lateinit var moderationService: ModerationService

    private val moderatorUser = UserResponse(
        id = 1L,
        username = "moderator",
        email = "mod@test.com",
        firstName = "Mod",
        lastName = "User",
        roles = setOf("MODERATOR"),
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )

    private val adminUser = UserResponse(
        id = 2L,
        username = "admin",
        email = "admin@test.com",
        firstName = "Admin",
        lastName = "User",
        roles = setOf("ADMIN"),
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )

    private val regularUser = UserResponse(
        id = 3L,
        username = "regular",
        email = "user@test.com",
        firstName = "Regular",
        lastName = "User",
        roles = setOf("USER"),
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )

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

    @BeforeEach
    fun setUp() {
        moderationService = ModerationService(
            kafkaRpcClient,
            moderationActionRepository,
            moderationAuditRepository
        )
    }

    // ==================== getPendingProducts Tests ====================

    @Test
    @DisplayName("Should get pending products for moderator")
    fun testGetPendingProductsSuccess() {
        val paginatedResponse = PaginatedResponse(
            data = listOf(testProduct),
            totalElements = 1,
            totalPages = 1,
            page = 1,
            pageSize = 20
        )

        whenever(kafkaRpcClient.getUserById(1L)).thenReturn(moderatorUser)
        whenever(kafkaRpcClient.getPendingProducts(1, 20)).thenReturn(paginatedResponse)

        StepVerifier.create(moderationService.getPendingProducts(1L, 1, 20))
            .assertNext { response ->
                assert(response.data.size == 1)
                assert(response.data[0].id == 100L)
            }
            .expectComplete()
            .verify(Duration.ofSeconds(10))
    }

    @Test
    @DisplayName("Should get pending products for admin")
    fun testGetPendingProductsForAdmin() {
        val paginatedResponse = PaginatedResponse(
            data = listOf(testProduct),
            totalElements = 1,
            totalPages = 1,
            page = 1,
            pageSize = 20
        )

        whenever(kafkaRpcClient.getUserById(2L)).thenReturn(adminUser)
        whenever(kafkaRpcClient.getPendingProducts(1, 20)).thenReturn(paginatedResponse)

        StepVerifier.create(moderationService.getPendingProducts(2L, 1, 20))
            .assertNext { response ->
                assert(response.data.size == 1)
            }
            .expectComplete()
            .verify(Duration.ofSeconds(10))
    }

    @Test
    @DisplayName("Should fail getting pending products for regular user")
    fun testGetPendingProductsForbidden() {
        whenever(kafkaRpcClient.getUserById(3L)).thenReturn(regularUser)

        StepVerifier.create(moderationService.getPendingProducts(3L, 1, 20))
            .expectError(ForbiddenException::class.java)
            .verify(Duration.ofSeconds(10))
    }

    // ==================== getPendingProductById Tests ====================

    @Test
    @DisplayName("Should get pending product by ID for moderator")
    fun testGetPendingProductByIdSuccess() {
        whenever(kafkaRpcClient.getUserById(1L)).thenReturn(moderatorUser)
        whenever(kafkaRpcClient.getPendingProductById(100L)).thenReturn(testProduct)

        StepVerifier.create(moderationService.getPendingProductById(1L, 100L))
            .assertNext { product ->
                assert(product.id == 100L)
                assert(product.name == "Test Product")
            }
            .expectComplete()
            .verify(Duration.ofSeconds(10))
    }

    @Test
    @DisplayName("Should fail getting pending product for regular user")
    fun testGetPendingProductByIdForbidden() {
        whenever(kafkaRpcClient.getUserById(3L)).thenReturn(regularUser)

        StepVerifier.create(moderationService.getPendingProductById(3L, 100L))
            .expectError(ForbiddenException::class.java)
            .verify(Duration.ofSeconds(10))
    }

    // ==================== approveProduct Tests ====================

    @Test
    @DisplayName("Should approve product successfully")
    fun testApproveProductSuccess() {
        val approvedProduct = testProduct.copy(status = "APPROVED")
        val savedAction = ModerationAction(
            id = 1L,
            productId = 100L,
            moderatorId = 1L,
            actionType = "APPROVE",
            reason = null
        )
        val savedAudit = ModerationAudit(
            id = 1L,
            actionId = 1L,
            productId = 100L,
            moderatorId = 1L,
            oldStatus = "PENDING",
            newStatus = "APPROVED"
        )

        whenever(kafkaRpcClient.getUserById(1L)).thenReturn(moderatorUser)
        whenever(kafkaRpcClient.approveProduct(100L, 1L)).thenReturn(approvedProduct)
        whenever(moderationActionRepository.save(any<ModerationAction>())).thenReturn(Mono.just(savedAction))
        whenever(moderationAuditRepository.save(any<ModerationAudit>())).thenReturn(Mono.just(savedAudit))

        StepVerifier.create(moderationService.approveProduct(1L, 100L))
            .assertNext { result ->
                assert(result.productId == 100L)
                assert(result.action == "APPROVE")
                assert(result.newStatus == "APPROVED")
            }
            .expectComplete()
            .verify(Duration.ofSeconds(10))
    }

    @Test
    @DisplayName("Should fail approving product for regular user")
    fun testApproveProductForbidden() {
        whenever(kafkaRpcClient.getUserById(3L)).thenReturn(regularUser)

        StepVerifier.create(moderationService.approveProduct(3L, 100L))
            .expectError(ForbiddenException::class.java)
            .verify(Duration.ofSeconds(10))
    }

    // ==================== rejectProduct Tests ====================

    @Test
    @DisplayName("Should reject product successfully")
    fun testRejectProductSuccess() {
        val rejectedProduct = testProduct.copy(status = "REJECTED", rejectionReason = "Low quality")
        val savedAction = ModerationAction(
            id = 1L,
            productId = 100L,
            moderatorId = 1L,
            actionType = "REJECT",
            reason = "Low quality"
        )
        val savedAudit = ModerationAudit(
            id = 1L,
            actionId = 1L,
            productId = 100L,
            moderatorId = 1L,
            oldStatus = "PENDING",
            newStatus = "REJECTED"
        )

        whenever(kafkaRpcClient.getUserById(1L)).thenReturn(moderatorUser)
        whenever(kafkaRpcClient.rejectProduct(100L, 1L, "Low quality")).thenReturn(rejectedProduct)
        whenever(moderationActionRepository.save(any<ModerationAction>())).thenReturn(Mono.just(savedAction))
        whenever(moderationAuditRepository.save(any<ModerationAudit>())).thenReturn(Mono.just(savedAudit))

        StepVerifier.create(moderationService.rejectProduct(1L, 100L, "Low quality"))
            .assertNext { result ->
                assert(result.productId == 100L)
                assert(result.action == "REJECT")
                assert(result.reason == "Low quality")
                assert(result.newStatus == "REJECTED")
            }
            .expectComplete()
            .verify(Duration.ofSeconds(10))
    }

    @Test
    @DisplayName("Should fail rejecting product for regular user")
    fun testRejectProductForbidden() {
        whenever(kafkaRpcClient.getUserById(3L)).thenReturn(regularUser)

        StepVerifier.create(moderationService.rejectProduct(3L, 100L, "Bad"))
            .expectError(ForbiddenException::class.java)
            .verify(Duration.ofSeconds(10))
    }

    // ==================== getModerationHistory Tests ====================

    @Test
    @DisplayName("Should get moderation history")
    fun testGetModerationHistory() {
        val actions = listOf(
            ModerationAction(id = 1L, productId = 100L, moderatorId = 1L, actionType = "APPROVE"),
            ModerationAction(id = 2L, productId = 101L, moderatorId = 1L, actionType = "REJECT", reason = "Bad")
        )

        whenever(moderationActionRepository.findByModeratorIdOrderByCreatedAtDesc(1L))
            .thenReturn(Flux.fromIterable(actions))

        StepVerifier.create(moderationService.getModerationHistory(1L))
            .assertNext { action ->
                assert(action.id == 1L)
                assert(action.actionType == "APPROVE")
            }
            .assertNext { action ->
                assert(action.id == 2L)
                assert(action.actionType == "REJECT")
            }
            .expectComplete()
            .verify(Duration.ofSeconds(10))
    }

    @Test
    @DisplayName("Should return empty history for new moderator")
    fun testGetModerationHistoryEmpty() {
        whenever(moderationActionRepository.findByModeratorIdOrderByCreatedAtDesc(999L))
            .thenReturn(Flux.empty())

        StepVerifier.create(moderationService.getModerationHistory(999L))
            .expectComplete()
            .verify(Duration.ofSeconds(10))
    }

    // ==================== getProductModerationHistory Tests ====================

    @Test
    @DisplayName("Should get product moderation history")
    fun testGetProductModerationHistory() {
        val audits = listOf(
            ModerationAudit(id = 1L, actionId = 1L, productId = 100L, moderatorId = 1L, oldStatus = "PENDING", newStatus = "APPROVED")
        )

        whenever(moderationAuditRepository.findByProductIdOrderByCreatedAtDesc(100L))
            .thenReturn(Flux.fromIterable(audits))

        StepVerifier.create(moderationService.getProductModerationHistory(100L))
            .assertNext { audit ->
                assert(audit.productId == 100L)
                assert(audit.newStatus == "APPROVED")
            }
            .expectComplete()
            .verify(Duration.ofSeconds(10))
    }

    @Test
    @DisplayName("Should return empty product history for new product")
    fun testGetProductModerationHistoryEmpty() {
        whenever(moderationAuditRepository.findByProductIdOrderByCreatedAtDesc(999L))
            .thenReturn(Flux.empty())

        StepVerifier.create(moderationService.getProductModerationHistory(999L))
            .expectComplete()
            .verify(Duration.ofSeconds(10))
    }

    // ==================== bulkModerate Tests ====================

    @Test
    @DisplayName("Should bulk approve multiple products")
    fun testBulkModerateApprove() {
        val request = BulkModerationRequest(
            productIds = listOf(100L, 101L),
            action = "APPROVE",
            reason = null
        )

        val approvedProduct1 = testProduct.copy(id = 100L, status = "APPROVED")
        val approvedProduct2 = testProduct.copy(id = 101L, status = "APPROVED")

        val savedAction1 = ModerationAction(id = 1L, productId = 100L, moderatorId = 1L, actionType = "APPROVE")
        val savedAction2 = ModerationAction(id = 2L, productId = 101L, moderatorId = 1L, actionType = "APPROVE")
        val savedAudit1 = ModerationAudit(id = 1L, actionId = 1L, productId = 100L, moderatorId = 1L, oldStatus = "PENDING", newStatus = "APPROVED")
        val savedAudit2 = ModerationAudit(id = 2L, actionId = 2L, productId = 101L, moderatorId = 1L, oldStatus = "PENDING", newStatus = "APPROVED")

        whenever(kafkaRpcClient.getUserById(1L)).thenReturn(moderatorUser)
        whenever(kafkaRpcClient.approveProduct(100L, 1L)).thenReturn(approvedProduct1)
        whenever(kafkaRpcClient.approveProduct(101L, 1L)).thenReturn(approvedProduct2)
        whenever(moderationActionRepository.save(any<ModerationAction>()))
            .thenReturn(Mono.just(savedAction1))
            .thenReturn(Mono.just(savedAction2))
        whenever(moderationAuditRepository.save(any<ModerationAudit>()))
            .thenReturn(Mono.just(savedAudit1))
            .thenReturn(Mono.just(savedAudit2))

        StepVerifier.create(moderationService.bulkModerate(1L, request))
            .assertNext { result ->
                assert(result.action == "APPROVE")
                assert(result.newStatus == "APPROVED")
            }
            .assertNext { result ->
                assert(result.action == "APPROVE")
                assert(result.newStatus == "APPROVED")
            }
            .expectComplete()
            .verify(Duration.ofSeconds(10))
    }

    @Test
    @DisplayName("Should bulk reject multiple products")
    fun testBulkModerateReject() {
        val request = BulkModerationRequest(
            productIds = listOf(100L, 101L),
            action = "REJECT",
            reason = "Low quality"
        )

        val rejectedProduct1 = testProduct.copy(id = 100L, status = "REJECTED", rejectionReason = "Low quality")
        val rejectedProduct2 = testProduct.copy(id = 101L, status = "REJECTED", rejectionReason = "Low quality")

        val savedAction1 = ModerationAction(id = 1L, productId = 100L, moderatorId = 1L, actionType = "REJECT", reason = "Low quality")
        val savedAction2 = ModerationAction(id = 2L, productId = 101L, moderatorId = 1L, actionType = "REJECT", reason = "Low quality")
        val savedAudit1 = ModerationAudit(id = 1L, actionId = 1L, productId = 100L, moderatorId = 1L, oldStatus = "PENDING", newStatus = "REJECTED")
        val savedAudit2 = ModerationAudit(id = 2L, actionId = 2L, productId = 101L, moderatorId = 1L, oldStatus = "PENDING", newStatus = "REJECTED")

        whenever(kafkaRpcClient.getUserById(1L)).thenReturn(moderatorUser)
        whenever(kafkaRpcClient.rejectProduct(100L, 1L, "Low quality")).thenReturn(rejectedProduct1)
        whenever(kafkaRpcClient.rejectProduct(101L, 1L, "Low quality")).thenReturn(rejectedProduct2)
        whenever(moderationActionRepository.save(any<ModerationAction>()))
            .thenReturn(Mono.just(savedAction1))
            .thenReturn(Mono.just(savedAction2))
        whenever(moderationAuditRepository.save(any<ModerationAudit>()))
            .thenReturn(Mono.just(savedAudit1))
            .thenReturn(Mono.just(savedAudit2))

        StepVerifier.create(moderationService.bulkModerate(1L, request))
            .assertNext { result ->
                assert(result.action == "REJECT")
                assert(result.reason == "Low quality")
                assert(result.newStatus == "REJECTED")
            }
            .assertNext { result ->
                assert(result.action == "REJECT")
                assert(result.reason == "Low quality")
                assert(result.newStatus == "REJECTED")
            }
            .expectComplete()
            .verify(Duration.ofSeconds(10))
    }

    @Test
    @DisplayName("Should bulk reject with default reason when not provided")
    fun testBulkModerateRejectWithDefaultReason() {
        val request = BulkModerationRequest(
            productIds = listOf(100L),
            action = "REJECT",
            reason = null
        )

        val rejectedProduct = testProduct.copy(id = 100L, status = "REJECTED", rejectionReason = "No reason provided")
        val savedAction = ModerationAction(id = 1L, productId = 100L, moderatorId = 1L, actionType = "REJECT", reason = "No reason provided")
        val savedAudit = ModerationAudit(id = 1L, actionId = 1L, productId = 100L, moderatorId = 1L, oldStatus = "PENDING", newStatus = "REJECTED")

        whenever(kafkaRpcClient.getUserById(1L)).thenReturn(moderatorUser)
        whenever(kafkaRpcClient.rejectProduct(100L, 1L, "No reason provided")).thenReturn(rejectedProduct)
        whenever(moderationActionRepository.save(any<ModerationAction>())).thenReturn(Mono.just(savedAction))
        whenever(moderationAuditRepository.save(any<ModerationAudit>())).thenReturn(Mono.just(savedAudit))

        StepVerifier.create(moderationService.bulkModerate(1L, request))
            .assertNext { result ->
                assert(result.action == "REJECT")
                assert(result.reason == "No reason provided")
            }
            .expectComplete()
            .verify(Duration.ofSeconds(10))
    }

    @Test
    @DisplayName("Should fail bulk moderation for regular user")
    fun testBulkModerateForbidden() {
        val request = BulkModerationRequest(
            productIds = listOf(100L),
            action = "APPROVE",
            reason = null
        )

        whenever(kafkaRpcClient.getUserById(3L)).thenReturn(regularUser)

        StepVerifier.create(moderationService.bulkModerate(3L, request))
            .expectError(ForbiddenException::class.java)
            .verify(Duration.ofSeconds(10))
    }

    // ==================== Edge Cases ====================

    @Test
    @DisplayName("Should get pending product by ID for admin")
    fun testGetPendingProductByIdForAdmin() {
        whenever(kafkaRpcClient.getUserById(2L)).thenReturn(adminUser)
        whenever(kafkaRpcClient.getPendingProductById(100L)).thenReturn(testProduct)

        StepVerifier.create(moderationService.getPendingProductById(2L, 100L))
            .assertNext { product ->
                assert(product.id == 100L)
            }
            .expectComplete()
            .verify(Duration.ofSeconds(10))
    }

    @Test
    @DisplayName("Should approve product as admin")
    fun testApproveProductAsAdmin() {
        val approvedProduct = testProduct.copy(status = "APPROVED")
        val savedAction = ModerationAction(id = 1L, productId = 100L, moderatorId = 2L, actionType = "APPROVE")
        val savedAudit = ModerationAudit(id = 1L, actionId = 1L, productId = 100L, moderatorId = 2L, oldStatus = "PENDING", newStatus = "APPROVED")

        whenever(kafkaRpcClient.getUserById(2L)).thenReturn(adminUser)
        whenever(kafkaRpcClient.approveProduct(100L, 2L)).thenReturn(approvedProduct)
        whenever(moderationActionRepository.save(any<ModerationAction>())).thenReturn(Mono.just(savedAction))
        whenever(moderationAuditRepository.save(any<ModerationAudit>())).thenReturn(Mono.just(savedAudit))

        StepVerifier.create(moderationService.approveProduct(2L, 100L))
            .assertNext { result ->
                assert(result.productId == 100L)
                assert(result.moderatorId == 2L)
            }
            .expectComplete()
            .verify(Duration.ofSeconds(10))
    }

    @Test
    @DisplayName("Should reject product as admin")
    fun testRejectProductAsAdmin() {
        val rejectedProduct = testProduct.copy(status = "REJECTED", rejectionReason = "Policy violation")
        val savedAction = ModerationAction(id = 1L, productId = 100L, moderatorId = 2L, actionType = "REJECT", reason = "Policy violation")
        val savedAudit = ModerationAudit(id = 1L, actionId = 1L, productId = 100L, moderatorId = 2L, oldStatus = "PENDING", newStatus = "REJECTED")

        whenever(kafkaRpcClient.getUserById(2L)).thenReturn(adminUser)
        whenever(kafkaRpcClient.rejectProduct(100L, 2L, "Policy violation")).thenReturn(rejectedProduct)
        whenever(moderationActionRepository.save(any<ModerationAction>())).thenReturn(Mono.just(savedAction))
        whenever(moderationAuditRepository.save(any<ModerationAudit>())).thenReturn(Mono.just(savedAudit))

        StepVerifier.create(moderationService.rejectProduct(2L, 100L, "Policy violation"))
            .assertNext { result ->
                assert(result.productId == 100L)
                assert(result.moderatorId == 2L)
                assert(result.reason == "Policy violation")
            }
            .expectComplete()
            .verify(Duration.ofSeconds(10))
    }

    @Test
    @DisplayName("Should handle multiple pages of pending products")
    fun testGetPendingProductsWithPagination() {
        val paginatedResponse = PaginatedResponse(
            data = listOf(testProduct),
            totalElements = 100,
            totalPages = 5,
            page = 2,
            pageSize = 20
        )

        whenever(kafkaRpcClient.getUserById(1L)).thenReturn(moderatorUser)
        whenever(kafkaRpcClient.getPendingProducts(2, 20)).thenReturn(paginatedResponse)

        StepVerifier.create(moderationService.getPendingProducts(1L, 2, 20))
            .assertNext { response ->
                assert(response.page == 2)
                assert(response.totalPages == 5)
                assert(response.totalElements == 100L)
            }
            .expectComplete()
            .verify(Duration.ofSeconds(10))
    }

    @Test
    @DisplayName("Should handle empty pending products list")
    fun testGetPendingProductsEmpty() {
        val paginatedResponse = PaginatedResponse(
            data = emptyList<ProductResponse>(),
            totalElements = 0,
            totalPages = 0,
            page = 1,
            pageSize = 20
        )

        whenever(kafkaRpcClient.getUserById(1L)).thenReturn(moderatorUser)
        whenever(kafkaRpcClient.getPendingProducts(1, 20)).thenReturn(paginatedResponse)

        StepVerifier.create(moderationService.getPendingProducts(1L, 1, 20))
            .assertNext { response ->
                assert(response.data.isEmpty())
                assert(response.totalElements == 0L)
            }
            .expectComplete()
            .verify(Duration.ofSeconds(10))
    }

    @Test
    @DisplayName("Should get moderation history with many actions")
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

        whenever(moderationActionRepository.findByModeratorIdOrderByCreatedAtDesc(1L))
            .thenReturn(Flux.fromIterable(actions))

        StepVerifier.create(moderationService.getModerationHistory(1L))
            .expectNextCount(5)
            .expectComplete()
            .verify(Duration.ofSeconds(10))
    }

    @Test
    @DisplayName("Should get product moderation history with multiple audits")
    fun testGetProductModerationHistoryMultiple() {
        val audits = listOf(
            ModerationAudit(id = 1L, actionId = 1L, productId = 100L, moderatorId = 1L, oldStatus = "PENDING", newStatus = "APPROVED"),
            ModerationAudit(id = 2L, actionId = 2L, productId = 100L, moderatorId = 2L, oldStatus = "APPROVED", newStatus = "REJECTED"),
            ModerationAudit(id = 3L, actionId = 3L, productId = 100L, moderatorId = 1L, oldStatus = "REJECTED", newStatus = "APPROVED")
        )

        whenever(moderationAuditRepository.findByProductIdOrderByCreatedAtDesc(100L))
            .thenReturn(Flux.fromIterable(audits))

        StepVerifier.create(moderationService.getProductModerationHistory(100L))
            .expectNextCount(3)
            .expectComplete()
            .verify(Duration.ofSeconds(10))
    }
}
