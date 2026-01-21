package ru.itmo.productservice.model

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import ru.itmo.productservice.domain.entity.Product
import ru.itmo.productservice.domain.entity.Shop
import ru.itmo.productservice.domain.enums.ProductStatus
import java.math.BigDecimal
import java.time.LocalDateTime

@DisplayName("Entity Tests")
class EntityTest {

    // ==================== Product Entity Tests ====================

    @Test
    @DisplayName("Product should store all fields")
    fun testProductEntity() {
        val now = LocalDateTime.now()
        val product = Product(
            id = 1L,
            name = "Test Product",
            description = "Description",
            price = BigDecimal("100.00"),
            imageUrl = "http://image.url",
            shopId = 1L,
            sellerId = 1L,
            status = ProductStatus.PENDING,
            rejectionReason = null,
            createdAt = now,
            updatedAt = now
        )

        assert(product.id == 1L)
        assert(product.name == "Test Product")
        assert(product.description == "Description")
        assert(product.price == BigDecimal("100.00"))
        assert(product.imageUrl == "http://image.url")
        assert(product.shopId == 1L)
        assert(product.sellerId == 1L)
        assert(product.status == ProductStatus.PENDING)
    }

    @Test
    @DisplayName("Product should allow null optional fields")
    fun testProductNullOptionals() {
        val product = Product(
            name = "Product",
            price = BigDecimal("50.00"),
            shopId = 1L,
            sellerId = 1L
        )

        assert(product.description == null)
        assert(product.imageUrl == null)
        assert(product.rejectionReason == null)
    }

    @Test
    @DisplayName("Product should have default status PENDING")
    fun testProductDefaultStatus() {
        val product = Product(
            name = "Product",
            price = BigDecimal("50.00"),
            shopId = 1L,
            sellerId = 1L
        )

        assert(product.status == ProductStatus.PENDING)
    }

    @Test
    @DisplayName("Product should have default id 0")
    fun testProductDefaultId() {
        val product = Product(
            name = "Product",
            price = BigDecimal("50.00"),
            shopId = 1L,
            sellerId = 1L
        )

        assert(product.id == 0L)
    }

    @Test
    @DisplayName("Product copy function should work")
    fun testProductCopy() {
        val product = Product(
            id = 1L,
            name = "Product",
            price = BigDecimal("50.00"),
            shopId = 1L,
            sellerId = 1L,
            status = ProductStatus.PENDING
        )

        val approved = product.copy(status = ProductStatus.APPROVED)

        assert(approved.status == ProductStatus.APPROVED)
        assert(approved.id == product.id)
        assert(approved.name == product.name)
    }

    @Test
    @DisplayName("Product copy with rejection reason")
    fun testProductCopyWithRejection() {
        val product = Product(
            id = 1L,
            name = "Product",
            price = BigDecimal("50.00"),
            shopId = 1L,
            sellerId = 1L,
            status = ProductStatus.PENDING
        )

        val rejected = product.copy(
            status = ProductStatus.REJECTED,
            rejectionReason = "Inappropriate content"
        )

        assert(rejected.status == ProductStatus.REJECTED)
        assert(rejected.rejectionReason == "Inappropriate content")
    }

    @Test
    @DisplayName("Product data class equality")
    fun testProductEquality() {
        val now = LocalDateTime.now()
        val product1 = Product(1L, "Product", null, BigDecimal("50.00"), null, 1L, 1L, ProductStatus.PENDING, null, now, now)
        val product2 = Product(1L, "Product", null, BigDecimal("50.00"), null, 1L, 1L, ProductStatus.PENDING, null, now, now)

        assert(product1 == product2)
    }

    // ==================== Shop Entity Tests ====================

    @Test
    @DisplayName("Shop should store all fields")
    fun testShopEntity() {
        val now = LocalDateTime.now()
        val shop = Shop(
            id = 1L,
            name = "Test Shop",
            description = "Description",
            avatarUrl = "http://avatar.url",
            sellerId = 1L,
            createdAt = now,
            updatedAt = now
        )

        assert(shop.id == 1L)
        assert(shop.name == "Test Shop")
        assert(shop.description == "Description")
        assert(shop.avatarUrl == "http://avatar.url")
        assert(shop.sellerId == 1L)
    }

    @Test
    @DisplayName("Shop should allow null optional fields")
    fun testShopNullOptionals() {
        val shop = Shop(
            name = "Shop",
            sellerId = 1L
        )

        assert(shop.description == null)
        assert(shop.avatarUrl == null)
    }

    @Test
    @DisplayName("Shop should have default id 0")
    fun testShopDefaultId() {
        val shop = Shop(
            name = "Shop",
            sellerId = 1L
        )

        assert(shop.id == 0L)
    }

    @Test
    @DisplayName("Shop copy function should work")
    fun testShopCopy() {
        val shop = Shop(
            id = 1L,
            name = "Shop",
            description = "Original",
            sellerId = 1L
        )

        val updated = shop.copy(name = "Updated Shop")

        assert(updated.name == "Updated Shop")
        assert(updated.id == shop.id)
        assert(updated.description == shop.description)
    }

    @Test
    @DisplayName("Shop data class equality")
    fun testShopEquality() {
        val now = LocalDateTime.now()
        val shop1 = Shop(1L, "Shop", "Desc", null, 1L, now, now)
        val shop2 = Shop(1L, "Shop", "Desc", null, 1L, now, now)

        assert(shop1 == shop2)
    }

    // ==================== ProductStatus Enum Tests ====================

    @Test
    @DisplayName("ProductStatus should have PENDING value")
    fun testProductStatusPending() {
        assert(ProductStatus.PENDING.name == "PENDING")
    }

    @Test
    @DisplayName("ProductStatus should have APPROVED value")
    fun testProductStatusApproved() {
        assert(ProductStatus.APPROVED.name == "APPROVED")
    }

    @Test
    @DisplayName("ProductStatus should have REJECTED value")
    fun testProductStatusRejected() {
        assert(ProductStatus.REJECTED.name == "REJECTED")
    }

    @Test
    @DisplayName("ProductStatus values should be three")
    fun testProductStatusValues() {
        assert(ProductStatus.entries.size == 3)
    }

    @Test
    @DisplayName("ProductStatus valueOf should work")
    fun testProductStatusValueOf() {
        assert(ProductStatus.valueOf("PENDING") == ProductStatus.PENDING)
        assert(ProductStatus.valueOf("APPROVED") == ProductStatus.APPROVED)
        assert(ProductStatus.valueOf("REJECTED") == ProductStatus.REJECTED)
    }
}
