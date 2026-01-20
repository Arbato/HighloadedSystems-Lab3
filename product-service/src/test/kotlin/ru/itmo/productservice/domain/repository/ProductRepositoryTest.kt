package ru.itmo.productservice.domain.repository

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import java.math.BigDecimal

@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(properties = [
    "spring.cloud.config.enabled=false",
    "spring.cloud.config.import-check.enabled=false",
    "eureka.client.enabled=false"
])
@DisplayName("Product Repository Tests")
class ProductRepositoryTest {

    @Autowired
    private lateinit var productRepository: ProductRepository

    @Autowired
    private lateinit var shopRepository: ShopRepository

    @Autowired
    private lateinit var entityManager: TestEntityManager

    private lateinit var shop: Shop

    @BeforeEach
    fun setUp() {
        productRepository.deleteAll()
        shopRepository.deleteAll()

        shop = Shop(
            name = "Test Shop",
            sellerId = 1L
        )
        shop = shopRepository.save(shop)
        entityManager.flush()
    }

    @Test
    @DisplayName("Should find all products by shop ID")
    fun testFindByShopId() {
        val product1 = Product(
            name = "Product 1",
            price = BigDecimal("100"),
            shopId = shop.id,
            sellerId = 1L
        )
        val product2 = Product(
            name = "Product 2",
            price = BigDecimal("200"),
            shopId = shop.id,
            sellerId = 1L
        )

        productRepository.saveAll(listOf(product1, product2))
        entityManager.flush()

        val pageable = PageRequest.of(0, 20)
        val result = productRepository.findAllByShopId(shop.id, pageable)

        assert(result.content.size == 2)
    }

    @Test
    @DisplayName("Should search products by keyword")
    fun testSearchByKeyword() {
        val product = Product(
            name = "Amazing Laptop",
            description = "High performance laptop",
            price = BigDecimal("1000"),
            shopId = shop.id,
            sellerId = 1L,
            status = ProductStatus.APPROVED
        )

        productRepository.save(product)
        entityManager.flush()

        val pageable = PageRequest.of(0, 20)
        val result = productRepository.searchApprovedProducts("laptop", pageable)

        assert(result.content.size == 1)
    }

    @Test
    @DisplayName("Should find pending product by ID")
    fun testFindPendingById() {
        val product = Product(
            name = "Pending Product",
            price = BigDecimal("50"),
            shopId = shop.id,
            sellerId = 1L,
            status = ProductStatus.PENDING
        )

        val saved = productRepository.save(product)
        entityManager.flush()

        val result = productRepository.findPendingById(saved.id)

        assert(result != null)
        assert(result?.status == ProductStatus.PENDING)
    }

    @Test
    @DisplayName("Should count products by shop ID")
    fun testCountByShopId() {
        val product1 = Product(
            name = "Product 1",
            price = BigDecimal("100"),
            shopId = shop.id,
            sellerId = 1L
        )
        val product2 = Product(
            name = "Product 2",
            price = BigDecimal("200"),
            shopId = shop.id,
            sellerId = 1L
        )

        productRepository.saveAll(listOf(product1, product2))
        entityManager.flush()

        val count = productRepository.countByShopId(shop.id)

        assert(count == 2L)
    }

    @Test
    @DisplayName("Should find products by status")
    fun testFindAllByStatus() {
        val pending = Product(
            name = "Pending Product",
            price = BigDecimal("100"),
            shopId = shop.id,
            sellerId = 1L,
            status = ProductStatus.PENDING
        )
        val approved = Product(
            name = "Approved Product",
            price = BigDecimal("200"),
            shopId = shop.id,
            sellerId = 1L,
            status = ProductStatus.APPROVED
        )

        productRepository.saveAll(listOf(pending, approved))
        entityManager.flush()

        val pageable = PageRequest.of(0, 20)
        val pendingResults = productRepository.findAllByStatus(ProductStatus.PENDING, pageable)
        val approvedResults = productRepository.findAllByStatus(ProductStatus.APPROVED, pageable)

        assert(pendingResults.content.size == 1)
        assert(approvedResults.content.size == 1)
    }
}