package ru.itmo.productservice.application.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import ru.itmo.productservice.adapter.out.client.UserServiceClient
import ru.itmo.productservice.adapter.exception.BadRequestException
import ru.itmo.productservice.adapter.exception.ForbiddenException
import ru.itmo.productservice.adapter.exception.ResourceNotFoundException
import ru.itmo.productservice.domain.model.dto.request.CreateProductRequest
import ru.itmo.productservice.domain.model.dto.request.UpdateProductRequest
import ru.itmo.productservice.domain.model.dto.response.UserResponse
import ru.itmo.productservice.domain.model.entity.Product
import ru.itmo.productservice.domain.model.entity.Shop
import ru.itmo.productservice.domain.model.enums.ProductStatus
import ru.itmo.productservice.domain.repository.ProductRepository
import ru.itmo.productservice.domain.repository.ShopRepository
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
@DisplayName("ProductService Unit Tests")
class ProductServiceTest {

    @Mock
    private lateinit var productRepository: ProductRepository

    @Mock
    private lateinit var shopRepository: ShopRepository

    @Mock
    private lateinit var userServiceClient: UserServiceClient

    private lateinit var productService: ProductService

    private val testProduct = Product(
        id = 1L,
        name = "Test Product",
        description = "Test Description",
        price = BigDecimal("100.00"),
        imageUrl = "http://image.url",
        shopId = 1L,
        sellerId = 1L,
        status = ProductStatus.PENDING,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )

    private val testShop = Shop(
        id = 1L,
        name = "Test Shop",
        description = "Test Description",
        sellerId = 1L,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )

    private val now = LocalDateTime.now()

    private val moderatorUser = UserResponse(
        id = 2L,
        username = "moderator",
        email = "moderator@test.com",
        firstName = "Mod",
        lastName = "User",
        roles = setOf("MODERATOR"),
        createdAt = now,
        updatedAt = now
    )

    private val regularUser = UserResponse(
        id = 1L,
        username = "user",
        email = "user@test.com",
        firstName = "Regular",
        lastName = "User",
        roles = setOf("USER"),
        createdAt = now,
        updatedAt = now
    )

    @BeforeEach
    fun setUp() {
        productService = ProductService(productRepository, shopRepository, userServiceClient)
    }

    // ==================== getApprovedProducts ====================

    @Test
    @DisplayName("getApprovedProducts returns paginated products")
    fun testGetApprovedProductsSuccess() {
        val products = listOf(testProduct.copy(status = ProductStatus.APPROVED))
        val page = PageImpl(products, PageRequest.of(0, 10), 1)

        whenever(productRepository.findAllByStatus(ProductStatus.APPROVED, PageRequest.of(0, 10)))
            .thenReturn(page)

        val result = productService.getApprovedProducts(1, 10)

        assert(result.data.size == 1)
        assert(result.page == 1)
        assert(result.pageSize == 10)
        assert(result.totalElements == 1L)
    }

    @Test
    @DisplayName("getApprovedProducts throws on invalid page")
    fun testGetApprovedProductsInvalidPage() {
        assertThrows<BadRequestException> {
            productService.getApprovedProducts(0, 10)
        }
    }

    @Test
    @DisplayName("getApprovedProducts throws on invalid pageSize")
    fun testGetApprovedProductsInvalidPageSize() {
        assertThrows<BadRequestException> {
            productService.getApprovedProducts(1, 0)
        }
    }

    @Test
    @DisplayName("getApprovedProducts throws on negative page")
    fun testGetApprovedProductsNegativePage() {
        assertThrows<BadRequestException> {
            productService.getApprovedProducts(-1, 10)
        }
    }

    @Test
    @DisplayName("getApprovedProducts returns empty when no products")
    fun testGetApprovedProductsEmpty() {
        val emptyPage = PageImpl<Product>(emptyList(), PageRequest.of(0, 10), 0)

        whenever(productRepository.findAllByStatus(ProductStatus.APPROVED, PageRequest.of(0, 10)))
            .thenReturn(emptyPage)

        val result = productService.getApprovedProducts(1, 10)

        assert(result.data.isEmpty())
        assert(result.totalElements == 0L)
    }

    // ==================== getProductsByShopId ====================

    @Test
    @DisplayName("getProductsByShopId returns products for valid shop")
    fun testGetProductsByShopIdSuccess() {
        val products = listOf(testProduct)
        val page = PageImpl(products, PageRequest.of(0, 10), 1)

        whenever(shopRepository.existsById(1L)).thenReturn(true)
        whenever(productRepository.findAllByShopId(1L, PageRequest.of(0, 10))).thenReturn(page)

        val result = productService.getProductsByShopId(1L, 1, 10)

        assert(result.data.size == 1)
        assert(result.data[0].shopId == 1L)
    }

    @Test
    @DisplayName("getProductsByShopId throws on non-existent shop")
    fun testGetProductsByShopIdShopNotFound() {
        whenever(shopRepository.existsById(999L)).thenReturn(false)

        assertThrows<ResourceNotFoundException> {
            productService.getProductsByShopId(999L, 1, 10)
        }
    }

    @Test
    @DisplayName("getProductsByShopId throws on invalid page")
    fun testGetProductsByShopIdInvalidPage() {
        assertThrows<BadRequestException> {
            productService.getProductsByShopId(1L, 0, 10)
        }
    }

    @Test
    @DisplayName("getProductsByShopId throws on invalid pageSize")
    fun testGetProductsByShopIdInvalidPageSize() {
        assertThrows<BadRequestException> {
            productService.getProductsByShopId(1L, 1, -1)
        }
    }

    // ==================== searchProducts ====================

    @Test
    @DisplayName("searchProducts returns results for valid keyword")
    fun testSearchProductsSuccess() {
        val products = listOf(testProduct.copy(status = ProductStatus.APPROVED))
        val page = PageImpl(products, PageRequest.of(0, 10), 1)

        whenever(productRepository.searchApprovedProducts("test", PageRequest.of(0, 10)))
            .thenReturn(page)

        val result = productService.searchProducts("test", 1, 10)

        assert(result.data.size == 1)
        assert(result.data[0].name.contains("Test"))
    }

    @Test
    @DisplayName("searchProducts throws on blank keyword")
    fun testSearchProductsBlankKeyword() {
        assertThrows<BadRequestException> {
            productService.searchProducts("   ", 1, 10)
        }
    }

    @Test
    @DisplayName("searchProducts throws on empty keyword")
    fun testSearchProductsEmptyKeyword() {
        assertThrows<BadRequestException> {
            productService.searchProducts("", 1, 10)
        }
    }

    @Test
    @DisplayName("searchProducts throws on invalid page")
    fun testSearchProductsInvalidPage() {
        assertThrows<BadRequestException> {
            productService.searchProducts("test", 0, 10)
        }
    }

    @Test
    @DisplayName("searchProducts returns empty for no matches")
    fun testSearchProductsNoMatches() {
        val emptyPage = PageImpl<Product>(emptyList(), PageRequest.of(0, 10), 0)

        whenever(productRepository.searchApprovedProducts("nonexistent", PageRequest.of(0, 10)))
            .thenReturn(emptyPage)

        val result = productService.searchProducts("nonexistent", 1, 10)

        assert(result.data.isEmpty())
    }

    // ==================== getProductById ====================

    @Test
    @DisplayName("getProductById returns product for valid ID")
    fun testGetProductByIdSuccess() {
        whenever(productRepository.findById(1L)).thenReturn(Optional.of(testProduct))

        val result = productService.getProductById(1L)

        assert(result.id == 1L)
        assert(result.name == "Test Product")
    }

    @Test
    @DisplayName("getProductById throws on non-existent product")
    fun testGetProductByIdNotFound() {
        whenever(productRepository.findById(999L)).thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> {
            productService.getProductById(999L)
        }
    }

    @Test
    @DisplayName("getProductById throws on invalid ID zero")
    fun testGetProductByIdInvalidIdZero() {
        assertThrows<BadRequestException> {
            productService.getProductById(0L)
        }
    }

    @Test
    @DisplayName("getProductById throws on invalid ID negative")
    fun testGetProductByIdInvalidIdNegative() {
        assertThrows<BadRequestException> {
            productService.getProductById(-1L)
        }
    }

    // ==================== createProduct ====================

    @Test
    @DisplayName("createProduct creates product successfully")
    fun testCreateProductSuccess() {
        val request = CreateProductRequest(
            name = "New Product",
            description = "Description",
            price = BigDecimal("50.00"),
            imageUrl = null,
            shopId = 1L
        )

        whenever(shopRepository.findById(1L)).thenReturn(Optional.of(testShop))
        whenever(productRepository.save(any<Product>())).thenAnswer { it.arguments[0] as Product }

        val result = productService.createProduct(request, 1L)

        assert(result.name == "New Product")
        assert(result.status == ProductStatus.PENDING.name)
        verify(productRepository).save(any())
    }

    @Test
    @DisplayName("createProduct throws on invalid seller ID")
    fun testCreateProductInvalidSellerId() {
        val request = CreateProductRequest(
            name = "Product",
            description = null,
            price = BigDecimal("10.00"),
            imageUrl = null,
            shopId = 1L
        )

        assertThrows<BadRequestException> {
            productService.createProduct(request, 0L)
        }
    }

    @Test
    @DisplayName("createProduct throws on blank name")
    fun testCreateProductBlankName() {
        val request = CreateProductRequest(
            name = "   ",
            description = null,
            price = BigDecimal("10.00"),
            imageUrl = null,
            shopId = 1L
        )

        assertThrows<BadRequestException> {
            productService.createProduct(request, 1L)
        }
    }

    @Test
    @DisplayName("createProduct throws when shop not found")
    fun testCreateProductShopNotFound() {
        val request = CreateProductRequest(
            name = "Product",
            description = null,
            price = BigDecimal("10.00"),
            imageUrl = null,
            shopId = 999L
        )

        whenever(shopRepository.findById(999L)).thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> {
            productService.createProduct(request, 1L)
        }
    }

    @Test
    @DisplayName("createProduct throws when seller doesn't own shop")
    fun testCreateProductNotShopOwner() {
        val request = CreateProductRequest(
            name = "Product",
            description = null,
            price = BigDecimal("10.00"),
            imageUrl = null,
            shopId = 1L
        )

        whenever(shopRepository.findById(1L)).thenReturn(Optional.of(testShop))

        assertThrows<ForbiddenException> {
            productService.createProduct(request, 999L) // Different seller
        }
    }

    // ==================== updateProduct ====================

    @Test
    @DisplayName("updateProduct updates product by moderator")
    fun testUpdateProductSuccess() {
        val request = UpdateProductRequest(
            name = "Updated Name",
            description = "Updated Description",
            price = BigDecimal("200.00"),
            imageUrl = "http://new-image.url"
        )

        whenever(productRepository.findById(1L)).thenReturn(Optional.of(testProduct))
        whenever(userServiceClient.getUserById(2L)).thenReturn(moderatorUser)
        whenever(productRepository.save(any<Product>())).thenAnswer { it.arguments[0] as Product }

        val result = productService.updateProduct(1L, 2L, request)

        assert(result.name == "Updated Name")
        assert(result.price == BigDecimal("200.00"))
    }

    @Test
    @DisplayName("updateProduct throws when user is not moderator")
    fun testUpdateProductNotModerator() {
        val request = UpdateProductRequest(name = "Updated")

        whenever(productRepository.findById(1L)).thenReturn(Optional.of(testProduct))
        whenever(userServiceClient.getUserById(1L)).thenReturn(regularUser)

        assertThrows<ForbiddenException> {
            productService.updateProduct(1L, 1L, request)
        }
    }

    @Test
    @DisplayName("updateProduct throws on invalid product ID")
    fun testUpdateProductInvalidProductId() {
        val request = UpdateProductRequest(name = "Updated")

        assertThrows<BadRequestException> {
            productService.updateProduct(0L, 1L, request)
        }
    }

    @Test
    @DisplayName("updateProduct throws on invalid user ID")
    fun testUpdateProductInvalidUserId() {
        val request = UpdateProductRequest(name = "Updated")

        assertThrows<BadRequestException> {
            productService.updateProduct(1L, -1L, request)
        }
    }

    @Test
    @DisplayName("updateProduct throws when product not found")
    fun testUpdateProductNotFound() {
        val request = UpdateProductRequest(name = "Updated")

        whenever(productRepository.findById(999L)).thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> {
            productService.updateProduct(999L, 2L, request)
        }
    }

    @Test
    @DisplayName("updateProduct preserves existing values when null in request")
    fun testUpdateProductPartialUpdate() {
        val request = UpdateProductRequest(name = "Only Name Updated")

        whenever(productRepository.findById(1L)).thenReturn(Optional.of(testProduct))
        whenever(userServiceClient.getUserById(2L)).thenReturn(moderatorUser)
        whenever(productRepository.save(any<Product>())).thenAnswer { it.arguments[0] as Product }

        val result = productService.updateProduct(1L, 2L, request)

        assert(result.name == "Only Name Updated")
        assert(result.description == testProduct.description)
        assert(result.price == testProduct.price)
    }

    // ==================== approveProduct ====================

    @Test
    @DisplayName("approveProduct approves pending product")
    fun testApproveProductSuccess() {
        val pendingProduct = testProduct.copy(status = ProductStatus.PENDING)

        whenever(productRepository.findById(1L)).thenReturn(Optional.of(pendingProduct))
        whenever(userServiceClient.getUserById(2L)).thenReturn(moderatorUser)
        whenever(productRepository.save(any<Product>())).thenAnswer {
            (it.arguments[0] as Product).copy(status = ProductStatus.APPROVED)
        }

        val result = productService.approveProduct(1L, 2L)

        assert(result.status == ProductStatus.APPROVED.name)
    }

    @Test
    @DisplayName("approveProduct throws when user is not moderator")
    fun testApproveProductNotModerator() {
        val pendingProduct = testProduct.copy(status = ProductStatus.PENDING)

        whenever(productRepository.findById(1L)).thenReturn(Optional.of(pendingProduct))
        whenever(userServiceClient.getUserById(1L)).thenReturn(regularUser)

        assertThrows<ForbiddenException> {
            productService.approveProduct(1L, 1L)
        }
    }

    @Test
    @DisplayName("approveProduct throws when product not pending")
    fun testApproveProductNotPending() {
        val approvedProduct = testProduct.copy(status = ProductStatus.APPROVED)

        whenever(productRepository.findById(1L)).thenReturn(Optional.of(approvedProduct))
        whenever(userServiceClient.getUserById(2L)).thenReturn(moderatorUser)

        assertThrows<IllegalStateException> {
            productService.approveProduct(1L, 2L)
        }
    }

    @Test
    @DisplayName("approveProduct throws on invalid product ID")
    fun testApproveProductInvalidProductId() {
        assertThrows<BadRequestException> {
            productService.approveProduct(0L, 2L)
        }
    }

    @Test
    @DisplayName("approveProduct throws on invalid moderator ID")
    fun testApproveProductInvalidModeratorId() {
        assertThrows<BadRequestException> {
            productService.approveProduct(1L, 0L)
        }
    }

    @Test
    @DisplayName("approveProduct throws when product not found")
    fun testApproveProductNotFound() {
        whenever(productRepository.findById(999L)).thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> {
            productService.approveProduct(999L, 2L)
        }
    }

    // ==================== rejectProduct ====================

    @Test
    @DisplayName("rejectProduct rejects pending product with reason")
    fun testRejectProductSuccess() {
        val pendingProduct = testProduct.copy(status = ProductStatus.PENDING)

        whenever(productRepository.findById(1L)).thenReturn(Optional.of(pendingProduct))
        whenever(userServiceClient.getUserById(2L)).thenReturn(moderatorUser)
        whenever(productRepository.save(any<Product>())).thenAnswer {
            (it.arguments[0] as Product)
        }

        val result = productService.rejectProduct(1L, 2L, "Inappropriate content")

        assert(result.status == ProductStatus.REJECTED.name)
        assert(result.rejectionReason == "Inappropriate content")
    }

    @Test
    @DisplayName("rejectProduct throws on blank reason")
    fun testRejectProductBlankReason() {
        assertThrows<BadRequestException> {
            productService.rejectProduct(1L, 2L, "   ")
        }
    }

    @Test
    @DisplayName("rejectProduct throws on empty reason")
    fun testRejectProductEmptyReason() {
        assertThrows<BadRequestException> {
            productService.rejectProduct(1L, 2L, "")
        }
    }

    @Test
    @DisplayName("rejectProduct throws when user is not moderator")
    fun testRejectProductNotModerator() {
        val pendingProduct = testProduct.copy(status = ProductStatus.PENDING)

        whenever(productRepository.findById(1L)).thenReturn(Optional.of(pendingProduct))
        whenever(userServiceClient.getUserById(1L)).thenReturn(regularUser)

        assertThrows<ForbiddenException> {
            productService.rejectProduct(1L, 1L, "Reason")
        }
    }

    @Test
    @DisplayName("rejectProduct throws when product not pending")
    fun testRejectProductNotPending() {
        val approvedProduct = testProduct.copy(status = ProductStatus.APPROVED)

        whenever(productRepository.findById(1L)).thenReturn(Optional.of(approvedProduct))
        whenever(userServiceClient.getUserById(2L)).thenReturn(moderatorUser)

        assertThrows<IllegalStateException> {
            productService.rejectProduct(1L, 2L, "Reason")
        }
    }

    @Test
    @DisplayName("rejectProduct throws on invalid IDs")
    fun testRejectProductInvalidIds() {
        assertThrows<BadRequestException> {
            productService.rejectProduct(-1L, 2L, "Reason")
        }

        assertThrows<BadRequestException> {
            productService.rejectProduct(1L, -1L, "Reason")
        }
    }

    // ==================== deleteProduct ====================

    @Test
    @DisplayName("deleteProduct deletes product by moderator")
    fun testDeleteProductByModerator() {
        whenever(productRepository.findById(1L)).thenReturn(Optional.of(testProduct))
        whenever(userServiceClient.getUserById(2L)).thenReturn(moderatorUser)
        doNothing().whenever(productRepository).deleteById(1L)

        productService.deleteProduct(1L, 2L)

        verify(productRepository).deleteById(1L)
    }

    @Test
    @DisplayName("deleteProduct deletes product by owner")
    fun testDeleteProductByOwner() {
        whenever(productRepository.findById(1L)).thenReturn(Optional.of(testProduct))
        whenever(userServiceClient.getUserById(1L)).thenReturn(regularUser)
        doNothing().whenever(productRepository).deleteById(1L)

        productService.deleteProduct(1L, 1L)

        verify(productRepository).deleteById(1L)
    }

    @Test
    @DisplayName("deleteProduct throws when not owner and not moderator")
    fun testDeleteProductNotAuthorized() {
        val otherUser = regularUser.copy(id = 999L)

        whenever(productRepository.findById(1L)).thenReturn(Optional.of(testProduct))
        whenever(userServiceClient.getUserById(999L)).thenReturn(otherUser)

        assertThrows<ForbiddenException> {
            productService.deleteProduct(1L, 999L)
        }
    }

    @Test
    @DisplayName("deleteProduct throws on invalid product ID")
    fun testDeleteProductInvalidProductId() {
        assertThrows<BadRequestException> {
            productService.deleteProduct(0L, 1L)
        }
    }

    @Test
    @DisplayName("deleteProduct throws on invalid user ID")
    fun testDeleteProductInvalidUserId() {
        assertThrows<BadRequestException> {
            productService.deleteProduct(1L, 0L)
        }
    }

    @Test
    @DisplayName("deleteProduct throws when product not found")
    fun testDeleteProductNotFound() {
        whenever(productRepository.findById(999L)).thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> {
            productService.deleteProduct(999L, 1L)
        }
    }

    // ==================== countProductsByShopId ====================

    @Test
    @DisplayName("countProductsByShopId returns count")
    fun testCountProductsByShopId() {
        whenever(productRepository.countByShopId(1L)).thenReturn(5L)

        val result = productService.countProductsByShopId(1L)

        assert(result == 5L)
    }

    @Test
    @DisplayName("countProductsByShopId returns zero for empty shop")
    fun testCountProductsByShopIdEmpty() {
        whenever(productRepository.countByShopId(999L)).thenReturn(0L)

        val result = productService.countProductsByShopId(999L)

        assert(result == 0L)
    }

    // ==================== existsById ====================

    @Test
    @DisplayName("existsById returns true for existing product")
    fun testExistsByIdTrue() {
        whenever(productRepository.existsById(1L)).thenReturn(true)

        assert(productService.existsById(1L))
    }

    @Test
    @DisplayName("existsById returns false for non-existing product")
    fun testExistsByIdFalse() {
        whenever(productRepository.existsById(999L)).thenReturn(false)

        assert(!productService.existsById(999L))
    }

    // ==================== getPendingProductById ====================

    @Test
    @DisplayName("getPendingProductById returns pending product")
    fun testGetPendingProductByIdSuccess() {
        val pendingProduct = testProduct.copy(status = ProductStatus.PENDING)

        whenever(productRepository.findPendingById(1L)).thenReturn(pendingProduct)

        val result = productService.getPendingProductById(1L)

        assert(result.id == 1L)
        assert(result.status == ProductStatus.PENDING.name)
    }

    @Test
    @DisplayName("getPendingProductById throws on invalid ID")
    fun testGetPendingProductByIdInvalidId() {
        assertThrows<BadRequestException> {
            productService.getPendingProductById(0L)
        }
    }

    @Test
    @DisplayName("getPendingProductById throws when not found")
    fun testGetPendingProductByIdNotFound() {
        whenever(productRepository.findPendingById(999L)).thenReturn(null)

        assertThrows<ResourceNotFoundException> {
            productService.getPendingProductById(999L)
        }
    }

    // ==================== getPendingProducts ====================

    @Test
    @DisplayName("getPendingProducts returns paginated pending products")
    fun testGetPendingProductsSuccess() {
        val pendingProducts = listOf(testProduct.copy(status = ProductStatus.PENDING))
        val page = PageImpl(pendingProducts, PageRequest.of(0, 10), 1)

        whenever(productRepository.findAllByStatus(ProductStatus.PENDING, PageRequest.of(0, 10)))
            .thenReturn(page)

        val result = productService.getPendingProducts(1, 10)

        assert(result.data.size == 1)
        assert(result.data[0].status == ProductStatus.PENDING.name)
    }

    @Test
    @DisplayName("getPendingProducts throws on invalid pagination")
    fun testGetPendingProductsInvalidPagination() {
        assertThrows<BadRequestException> {
            productService.getPendingProducts(0, 10)
        }

        assertThrows<BadRequestException> {
            productService.getPendingProducts(1, 0)
        }
    }
}
