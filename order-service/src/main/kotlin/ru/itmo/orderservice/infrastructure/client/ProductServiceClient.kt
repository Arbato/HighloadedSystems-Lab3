package ru.itmo.orderservice.infrastructure.client

import org.springframework.context.annotation.Bean
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import ru.itmo.orderservice.adapters.exception.ServiceUnavailableException
import ru.itmo.orderservice.adapters.exception.ResourceNotFoundException
import ru.itmo.orderservice.adapters.exception.BadRequestException
import ru.itmo.orderservice.application.dto.response.ProductResponse
import feign.codec.ErrorDecoder
import org.springframework.stereotype.Component

@FeignClient(
    name = "product-service",
    fallback = ProductServiceClientFallback::class,
    configuration = [ProductServiceFeignConfig::class]
)
interface ProductServiceClient {
    
    /**
     * Получить товар по ID
     */
    @GetMapping("/api/products/{productId}")
    fun getProductById(@PathVariable("productId") productId: Long): ProductResponse
    
}

/**
 * Fallback реализация при недоступности сервиса
 */
@Component
class ProductServiceClientFallback : ProductServiceClient {
    override fun getProductById(productId: Long): ProductResponse {
        throw ServiceUnavailableException("Product service is currently unavailable. Please try again later.")
    }
}

class ProductServiceFeignConfig {
    @Bean
    fun productServiceErrorDecoder(): ErrorDecoder {
        return ErrorDecoder { _, response ->
            when (response.status()) {
                400 -> BadRequestException("Invalid product_id")
                404 -> ResourceNotFoundException("Product not found")
                500, 502, 503 -> ServiceUnavailableException("Product service is currently unavailable")
                else -> RuntimeException("HTTP ${response.status()}")
            }
        }
    }
}
