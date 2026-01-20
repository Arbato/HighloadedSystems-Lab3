package ru.itmo.market.client

import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.timelimiter.annotation.TimeLimiter
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.*
import ru.itmo.market.domain.model.dto.response.ProductResponse
import ru.itmo.market.domain.model.dto.response.PaginatedResponse
import ru.itmo.market.adapter.exception.ServiceUnavailableException
import ru.itmo.market.adapter.exception.ResourceNotFoundException
import ru.itmo.market.adapter.exception.BadRequestException
import java.util.concurrent.CompletableFuture
import feign.Response
import feign.codec.ErrorDecoder
import org.springframework.boot.autoconfigure.http.HttpMessageConverters
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import com.fasterxml.jackson.databind.ObjectMapper

@FeignClient(
    name = "product-service",
    fallback = ProductServiceClientFallback::class,
    configuration = [ProductServiceFeignConfig::class]
)
interface ProductServiceClient {
    
    @GetMapping("/api/products/pending/{id}")
    fun getPendingProductById(
        @PathVariable id: Long
    ): ProductResponse
    
    @GetMapping("/api/products/pending")
    fun getPendingProducts(
        @RequestParam page: Int = 1,
        @RequestParam pageSize: Int = 20
    ): PaginatedResponse<ProductResponse>
    
    @PostMapping("/api/products/{id}/approve")
    fun approveProduct(
        @PathVariable id: Long,
        @RequestParam moderatorId: Long
    ): ProductResponse
    
    @PostMapping("/api/products/{id}/reject")
    fun rejectProduct(
        @PathVariable id: Long,
        @RequestParam moderatorId: Long,
        @RequestParam reason: String
    ): ProductResponse
}

/**
 * Fallback реализация при недоступности Product Service
 */
@org.springframework.stereotype.Component
class ProductServiceClientFallback : ProductServiceClient {
    
    override fun getPendingProductById(id: Long): ProductResponse {
        throw ServiceUnavailableException(
            message = "Product Service недоступен. ID: $id"
        )
    }
    
    override fun getPendingProducts(page: Int, pageSize: Int): PaginatedResponse<ProductResponse> {
        throw ServiceUnavailableException(
            message = "Product Service недоступен при получении pending products"
        )
    }
    
    override fun approveProduct(id: Long, moderatorId: Long): ProductResponse {
        throw ServiceUnavailableException(
            "Product Service недоступен при одобрении товара"
        )
    }
    
    override fun rejectProduct(id: Long, moderatorId: Long, reason: String): ProductResponse {
        throw ServiceUnavailableException(
            message = "Product Service недоступен при отклонении товара"
        )
    }
}


@Configuration
class ProductServiceFeignConfig {

    @Bean
    fun httpMessageConverters(objectMapper: ObjectMapper): HttpMessageConverters {
        return HttpMessageConverters(
            MappingJackson2HttpMessageConverter(objectMapper)
        )
    }
    
    @Bean
    fun productServiceErrorDecoder(): ErrorDecoder {
        return ErrorDecoder { _, response ->
            when (response.status()) {
                400 -> BadRequestException("Invalid product data")
                404 -> ResourceNotFoundException("Product not found")
                500, 502, 503 -> ServiceUnavailableException(
                    message = "Product service is currently unavailable"
                )
                else -> RuntimeException("HTTP ${response.status()}")
            }
        }
    }
}