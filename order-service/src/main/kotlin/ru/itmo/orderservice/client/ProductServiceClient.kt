package ru.itmo.orderservice.client

import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.timelimiter.annotation.TimeLimiter
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import ru.itmo.orderservice.adapter.exception.ServiceUnavailableException
import ru.itmo.orderservice.adapter.exception.ResourceNotFoundException
import ru.itmo.orderservice.adapter.exception.ConflictException
import ru.itmo.orderservice.adapter.exception.BadRequestException
import ru.itmo.orderservice.domain.model.dto.response.ProductResponse
import java.util.concurrent.CompletableFuture
import feign.Response
import feign.codec.ErrorDecoder

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
@org.springframework.stereotype.Component
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
