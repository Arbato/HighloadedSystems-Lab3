package ru.itmo.market.client

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertThrows
import ru.itmo.market.adapters.exception.ServiceUnavailableException
import ru.itmo.market.infrastructure.client.ProductServiceClientFallback

@DisplayName("ProductServiceClientFallback Tests")
class ProductServiceClientFallbackTest {

    private val fallback = ProductServiceClientFallback()

    @Test
    @DisplayName("getPendingProductById should throw ServiceUnavailableException")
    fun testGetPendingProductByIdThrowsException() {
        val exception = assertThrows<ServiceUnavailableException> {
            fallback.getPendingProductById(100L)
        }
        assert(exception.message?.contains("100") == true)
    }

    @Test
    @DisplayName("getPendingProducts should throw ServiceUnavailableException")
    fun testGetPendingProductsThrowsException() {
        val exception = assertThrows<ServiceUnavailableException> {
            fallback.getPendingProducts(1, 20)
        }
        assert(exception.message?.contains("pending products") == true)
    }

    @Test
    @DisplayName("approveProduct should throw ServiceUnavailableException")
    fun testApproveProductThrowsException() {
        val exception = assertThrows<ServiceUnavailableException> {
            fallback.approveProduct(100L, 1L)
        }
        assert(exception.message != null)
    }

    @Test
    @DisplayName("rejectProduct should throw ServiceUnavailableException")
    fun testRejectProductThrowsException() {
        val exception = assertThrows<ServiceUnavailableException> {
            fallback.rejectProduct(100L, 1L, "Bad quality")
        }
        assert(exception.message != null)
    }
}
