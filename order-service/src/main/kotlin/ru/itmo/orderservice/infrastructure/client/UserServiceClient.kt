package ru.itmo.orderservice.infrastructure.client

import org.springframework.context.annotation.Bean
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import ru.itmo.orderservice.application.dto.response.UserResponse
import ru.itmo.orderservice.adapters.exception.ServiceUnavailableException
import ru.itmo.orderservice.adapters.exception.ResourceNotFoundException
import ru.itmo.orderservice.adapters.exception.BadRequestException
import feign.codec.ErrorDecoder
import org.springframework.stereotype.Component

@FeignClient(
    name = "user-service",
    fallback = UserServiceClientFallback::class,
    configuration = [UserServiceFeignConfig::class]
)
interface UserServiceClient {
    /**
     * Получить пользователя по ID
     */
    @GetMapping("/api/users/{userId}")
    fun getUserById(@PathVariable("userId") userId: Long): UserResponse
}

@Component
class UserServiceClientFallback : UserServiceClient {
    override fun getUserById(productId: Long): UserResponse {
        throw ServiceUnavailableException("User service is currently unavailable. Please try again later.")
    }
}

class UserServiceFeignConfig {
    
    @Bean
    fun userServiceErrorDecoder(): ErrorDecoder {
        return ErrorDecoder { _, response ->
            when (response.status()) {
                400 -> BadRequestException("Invalid user_id")
                404 -> ResourceNotFoundException("User not found")
                500, 502, 503 -> ServiceUnavailableException(message = "User service is currently unavailable")
                else -> RuntimeException("HTTP ${response.status()}")
            }
        }
    }
}

