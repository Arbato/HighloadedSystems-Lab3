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
import ru.itmo.productservice.adapter.out.client.KafkaRpcClient
import ru.itmo.productservice.adapter.exception.BadRequestException
import ru.itmo.productservice.adapter.exception.ForbiddenException
import ru.itmo.productservice.adapter.exception.ResourceNotFoundException
import ru.itmo.productservice.domain.model.dto.request.CreateShopRequest
import ru.itmo.productservice.domain.model.dto.request.UpdateShopRequest
import ru.itmo.productservice.domain.model.dto.kafka.UserServiceResponse
import ru.itmo.productservice.domain.model.dto.kafka.UserData
import ru.itmo.productservice.domain.model.entity.Product
import ru.itmo.productservice.domain.model.entity.Shop
import ru.itmo.productservice.domain.model.enums.ProductStatus
import ru.itmo.productservice.domain.repository.ProductRepository
import ru.itmo.productservice.domain.repository.ShopRepository
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
@DisplayName("ShopService Unit Tests")
class ShopServiceTest {

    @Mock
    private lateinit var shopRepository: ShopRepository

    @Mock
    private lateinit var productRepository: ProductRepository

    @Mock
    private lateinit var kafkaRpcClient: KafkaRpcClient

    private lateinit var shopService: ShopService

    private val testShop = Shop(
        id = 1L,
        name = "Test Shop",
        description = "Test Description",
        avatarUrl = "http://avatar.url",
        sellerId = 1L,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )

    private val testProduct = Product(
        id = 1L,
        name = "Test Product",
        description = "Test Description",
        price = BigDecimal("100.00"),
        imageUrl = "http://image.url",
        shopId = 1L,
        sellerId = 1L,
        status = ProductStatus.APPROVED,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )

    private val now = LocalDateTime.now()

    private val testUserData = UserData(
        id = 1L,
        username = "seller",
        email = "seller@test.com",
        firstName = "Seller",
        lastName = "User",
        roles = setOf("SELLER"),
        createdAt = now,
        updatedAt = now
    )

    private val testUserResponse = UserServiceResponse(
        requestId = "test-request-id",
        success = true,
        user = testUserData
    )

    @BeforeEach
    fun setUp() {
        shopService = ShopService(shopRepository, productRepository, kafkaRpcClient)
    }

    // ==================== getAllShops ====================

    @Test
    @DisplayName("getAllShops returns paginated shops")
    fun testGetAllShopsSuccess() {
        val shops = listOf(testShop)
        val page = PageImpl(shops, PageRequest.of(0, 10), 1)

        whenever(shopRepository.findAll(PageRequest.of(0, 10))).thenReturn(page)
        whenever(kafkaRpcClient.getUserById(1L)).thenReturn(testUserResponse)
        whenever(productRepository.countByShopId(1L)).thenReturn(5L)

        val result = shopService.getAllShops(1, 10)

        assert(result.data.size == 1)
        assert(result.page == 1)
        assert(result.pageSize == 10)
        assert(result.totalElements == 1L)
    }

    @Test
    @DisplayName("getAllShops throws on invalid page")
    fun testGetAllShopsInvalidPage() {
        assertThrows<BadRequestException> {
            shopService.getAllShops(0, 10)
        }
    }

    @Test
    @DisplayName("getAllShops throws on invalid pageSize")
    fun testGetAllShopsInvalidPageSize() {
        assertThrows<BadRequestException> {
            shopService.getAllShops(1, 0)
        }
    }

    @Test
    @DisplayName("getAllShops throws on negative page")
    fun testGetAllShopsNegativePage() {
        assertThrows<BadRequestException> {
            shopService.getAllShops(-1, 10)
        }
    }

    @Test
    @DisplayName("getAllShops returns empty when no shops")
    fun testGetAllShopsEmpty() {
        val emptyPage = PageImpl<Shop>(emptyList(), PageRequest.of(0, 10), 0)

        whenever(shopRepository.findAll(PageRequest.of(0, 10))).thenReturn(emptyPage)

        val result = shopService.getAllShops(1, 10)

        assert(result.data.isEmpty())
        assert(result.totalElements == 0L)
    }

    @Test
    @DisplayName("getAllShops handles user service exception gracefully")
    fun testGetAllShopsUserServiceException() {
        val shops = listOf(testShop)
        val page = PageImpl(shops, PageRequest.of(0, 10), 1)

        whenever(shopRepository.findAll(PageRequest.of(0, 10))).thenReturn(page)
        whenever(kafkaRpcClient.getUserById(1L)).thenThrow(RuntimeException("Service unavailable"))
        whenever(productRepository.countByShopId(1L)).thenReturn(5L)

        val result = shopService.getAllShops(1, 10)

        assert(result.data.size == 1)
        assert(result.data[0].sellerName == null)
    }

    // ==================== getShopById ====================

    @Test
    @DisplayName("getShopById returns shop for valid ID")
    fun testGetShopByIdSuccess() {
        whenever(shopRepository.findById(1L)).thenReturn(Optional.of(testShop))
        whenever(kafkaRpcClient.getUserById(1L)).thenReturn(testUserResponse)
        whenever(productRepository.countByShopId(1L)).thenReturn(3L)

        val result = shopService.getShopById(1L)

        assert(result.id == 1L)
        assert(result.name == "Test Shop")
        assert(result.sellerName == "Seller User")
        assert(result.productsCount == 3L)
    }

    @Test
    @DisplayName("getShopById throws on invalid ID zero")
    fun testGetShopByIdInvalidIdZero() {
        assertThrows<BadRequestException> {
            shopService.getShopById(0L)
        }
    }

    @Test
    @DisplayName("getShopById throws on invalid ID negative")
    fun testGetShopByIdInvalidIdNegative() {
        assertThrows<BadRequestException> {
            shopService.getShopById(-1L)
        }
    }

    @Test
    @DisplayName("getShopById throws when shop not found")
    fun testGetShopByIdNotFound() {
        whenever(shopRepository.findById(999L)).thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> {
            shopService.getShopById(999L)
        }
    }

    @Test
    @DisplayName("getShopById handles user service exception gracefully")
    fun testGetShopByIdUserServiceException() {
        whenever(shopRepository.findById(1L)).thenReturn(Optional.of(testShop))
        whenever(kafkaRpcClient.getUserById(1L)).thenThrow(RuntimeException("Service unavailable"))
        whenever(productRepository.countByShopId(1L)).thenReturn(0L)

        val result = shopService.getShopById(1L)

        assert(result.id == 1L)
        assert(result.sellerName == null)
    }

    // ==================== createShop ====================

    @Test
    @DisplayName("createShop creates shop successfully")
    fun testCreateShopSuccess() {
        val request = CreateShopRequest(
            name = "New Shop",
            description = "Description",
            avatarUrl = "http://avatar.url"
        )

        whenever(shopRepository.existsBySellerId(1L)).thenReturn(false)
        whenever(kafkaRpcClient.getUserById(1L)).thenReturn(testUserResponse)
        whenever(shopRepository.save(any<Shop>())).thenAnswer { invocation ->
            (invocation.arguments[0] as Shop).copy(id = 1L)
        }
        whenever(productRepository.countByShopId(any())).thenReturn(0L)

        val result = shopService.createShop(1L, request)

        assert(result.name == "New Shop")
        assert(result.description == "Description")
        verify(shopRepository).save(any())
    }

    @Test
    @DisplayName("createShop throws on invalid seller ID")
    fun testCreateShopInvalidSellerId() {
        val request = CreateShopRequest(name = "Shop", description = null, avatarUrl = null)

        assertThrows<BadRequestException> {
            shopService.createShop(0L, request)
        }
    }

    @Test
    @DisplayName("createShop throws on negative seller ID")
    fun testCreateShopNegativeSellerId() {
        val request = CreateShopRequest(name = "Shop", description = null, avatarUrl = null)

        assertThrows<BadRequestException> {
            shopService.createShop(-1L, request)
        }
    }

    @Test
    @DisplayName("createShop throws on blank name")
    fun testCreateShopBlankName() {
        val request = CreateShopRequest(name = "   ", description = null, avatarUrl = null)

        assertThrows<BadRequestException> {
            shopService.createShop(1L, request)
        }
    }

    @Test
    @DisplayName("createShop throws on empty name")
    fun testCreateShopEmptyName() {
        val request = CreateShopRequest(name = "", description = null, avatarUrl = null)

        assertThrows<BadRequestException> {
            shopService.createShop(1L, request)
        }
    }

    @Test
    @DisplayName("createShop throws when seller already has a shop")
    fun testCreateShopSellerAlreadyHasShop() {
        val request = CreateShopRequest(name = "Shop", description = null, avatarUrl = null)

        whenever(shopRepository.existsBySellerId(1L)).thenReturn(true)

        assertThrows<BadRequestException> {
            shopService.createShop(1L, request)
        }
    }

    @Test
    @DisplayName("createShop calls user service to verify user exists")
    fun testCreateShopVerifiesUserExists() {
        val request = CreateShopRequest(name = "Shop", description = null, avatarUrl = null)

        whenever(shopRepository.existsBySellerId(1L)).thenReturn(false)
        whenever(kafkaRpcClient.getUserById(1L)).thenReturn(testUserResponse)
        whenever(shopRepository.save(any<Shop>())).thenAnswer { invocation ->
            (invocation.arguments[0] as Shop).copy(id = 1L)
        }
        whenever(productRepository.countByShopId(any())).thenReturn(0L)

        shopService.createShop(1L, request)

        verify(kafkaRpcClient, atLeast(1)).getUserById(1L)
    }

    // ==================== updateShop ====================

    @Test
    @DisplayName("updateShop updates shop successfully")
    fun testUpdateShopSuccess() {
        val request = UpdateShopRequest(
            name = "Updated Name",
            description = "Updated Description",
            avatarUrl = "http://new-avatar.url"
        )

        whenever(shopRepository.findById(1L)).thenReturn(Optional.of(testShop))
        whenever(shopRepository.save(any<Shop>())).thenAnswer { it.arguments[0] as Shop }
        whenever(kafkaRpcClient.getUserById(1L)).thenReturn(testUserResponse)
        whenever(productRepository.countByShopId(1L)).thenReturn(0L)

        val result = shopService.updateShop(1L, 1L, request)

        assert(result.name == "Updated Name")
        assert(result.description == "Updated Description")
    }

    @Test
    @DisplayName("updateShop throws on invalid shop ID")
    fun testUpdateShopInvalidShopId() {
        val request = UpdateShopRequest(name = "Updated")

        assertThrows<BadRequestException> {
            shopService.updateShop(0L, 1L, request)
        }
    }

    @Test
    @DisplayName("updateShop throws on invalid seller ID")
    fun testUpdateShopInvalidSellerId() {
        val request = UpdateShopRequest(name = "Updated")

        assertThrows<BadRequestException> {
            shopService.updateShop(1L, 0L, request)
        }
    }

    @Test
    @DisplayName("updateShop throws when shop not found")
    fun testUpdateShopNotFound() {
        val request = UpdateShopRequest(name = "Updated")

        whenever(shopRepository.findById(999L)).thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> {
            shopService.updateShop(999L, 1L, request)
        }
    }

    @Test
    @DisplayName("updateShop throws when not shop owner")
    fun testUpdateShopNotOwner() {
        val request = UpdateShopRequest(name = "Updated")

        whenever(shopRepository.findById(1L)).thenReturn(Optional.of(testShop))

        assertThrows<ForbiddenException> {
            shopService.updateShop(1L, 999L, request) // Different seller
        }
    }

    @Test
    @DisplayName("updateShop preserves existing values when null in request")
    fun testUpdateShopPartialUpdate() {
        val request = UpdateShopRequest(name = "Only Name Updated")

        whenever(shopRepository.findById(1L)).thenReturn(Optional.of(testShop))
        whenever(shopRepository.save(any<Shop>())).thenAnswer { it.arguments[0] as Shop }
        whenever(kafkaRpcClient.getUserById(1L)).thenReturn(testUserResponse)
        whenever(productRepository.countByShopId(1L)).thenReturn(0L)

        val result = shopService.updateShop(1L, 1L, request)

        assert(result.name == "Only Name Updated")
        assert(result.description == testShop.description)
        assert(result.avatarUrl == testShop.avatarUrl)
    }

    // ==================== getShopProducts ====================

    @Test
    @DisplayName("getShopProducts returns products for valid shop")
    fun testGetShopProductsSuccess() {
        val products = listOf(testProduct)
        val page = PageImpl(products, PageRequest.of(0, 10), 1)

        whenever(shopRepository.existsById(1L)).thenReturn(true)
        whenever(productRepository.findAllByShopId(1L, PageRequest.of(0, 10))).thenReturn(page)

        val result = shopService.getShopProducts(1L, 1, 10)

        assert(result.data.size == 1)
        assert(result.data[0].shopId == 1L)
    }

    @Test
    @DisplayName("getShopProducts throws on invalid shop ID zero")
    fun testGetShopProductsInvalidShopIdZero() {
        assertThrows<BadRequestException> {
            shopService.getShopProducts(0L, 1, 10)
        }
    }

    @Test
    @DisplayName("getShopProducts throws on invalid shop ID negative")
    fun testGetShopProductsInvalidShopIdNegative() {
        assertThrows<BadRequestException> {
            shopService.getShopProducts(-1L, 1, 10)
        }
    }

    @Test
    @DisplayName("getShopProducts throws on invalid page")
    fun testGetShopProductsInvalidPage() {
        assertThrows<BadRequestException> {
            shopService.getShopProducts(1L, 0, 10)
        }
    }

    @Test
    @DisplayName("getShopProducts throws on invalid pageSize")
    fun testGetShopProductsInvalidPageSize() {
        assertThrows<BadRequestException> {
            shopService.getShopProducts(1L, 1, 0)
        }
    }

    @Test
    @DisplayName("getShopProducts throws when shop not found")
    fun testGetShopProductsShopNotFound() {
        whenever(shopRepository.existsById(999L)).thenReturn(false)

        assertThrows<ResourceNotFoundException> {
            shopService.getShopProducts(999L, 1, 10)
        }
    }

    @Test
    @DisplayName("getShopProducts returns empty when no products")
    fun testGetShopProductsEmpty() {
        val emptyPage = PageImpl<Product>(emptyList(), PageRequest.of(0, 10), 0)

        whenever(shopRepository.existsById(1L)).thenReturn(true)
        whenever(productRepository.findAllByShopId(1L, PageRequest.of(0, 10))).thenReturn(emptyPage)

        val result = shopService.getShopProducts(1L, 1, 10)

        assert(result.data.isEmpty())
        assert(result.totalElements == 0L)
    }

    @Test
    @DisplayName("getShopProducts returns correct pagination info")
    fun testGetShopProductsPagination() {
        val products = listOf(testProduct)
        val page = PageImpl(products, PageRequest.of(1, 5), 10) // Page 2, 5 per page, 10 total

        whenever(shopRepository.existsById(1L)).thenReturn(true)
        whenever(productRepository.findAllByShopId(1L, PageRequest.of(1, 5))).thenReturn(page)

        val result = shopService.getShopProducts(1L, 2, 5)

        assert(result.page == 2)
        assert(result.pageSize == 5)
        assert(result.totalElements == 10L)
        assert(result.totalPages == 2)
    }
}
