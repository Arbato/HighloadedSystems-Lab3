package ru.itmo.orderservice.client

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertThrows
import ru.itmo.orderservice.adapters.exception.ServiceUnavailableException
import ru.itmo.orderservice.infrastructure.client.ProductServiceClientFallback

@DisplayName("ProductServiceClient Tests")
class ProductServiceClientTest {

    @Test
    @DisplayName("Fallback throws ServiceUnavailableException")
    fun testFallbackThrowsServiceUnavailableException() {
        val fallback = ProductServiceClientFallback()

        assertThrows<ServiceUnavailableException> {
            fallback.getProductById(1L)
        }
    }

    @Test
    @DisplayName("Fallback exception has correct message")
    fun testFallbackExceptionMessage() {
        val fallback = ProductServiceClientFallback()

        val exception = assertThrows<ServiceUnavailableException> {
            fallback.getProductById(1L)
        }

        assert(exception.message?.contains("Product service") == true)
        assert(exception.message?.contains("unavailable") == true)
    }

    @Test
    @DisplayName("Fallback works for any product ID")
    fun testFallbackWorksForAnyProductId() {
        val fallback = ProductServiceClientFallback()

        listOf(1L, 100L, 999L, Long.MAX_VALUE).forEach { productId ->
            assertThrows<ServiceUnavailableException> {
                fallback.getProductById(productId)
            }
        }
    }
}
