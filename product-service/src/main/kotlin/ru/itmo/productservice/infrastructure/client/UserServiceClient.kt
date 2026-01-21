package ru.itmo.productservice.infrastructure.client

import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import ru.itmo.productservice.application.dto.response.UserResponse
import ru.itmo.productservice.adapters.exception.ServiceUnavailableException
import ru.itmo.productservice.adapters.exception.ResourceNotFoundException
import feign.codec.ErrorDecoder

@FeignClient(
    name = "user-service",
    fallback = UserServiceClientFallback::class,
    configuration = [FeignClientConfig::class]
)
interface UserServiceClient {
    
    /**
     * Получить пользователя по ID
     * ВНУТРЕННИЙ ЭНДПОИНТ
     */
    @GetMapping("/api/users/{userId}")
    fun getUserById(@PathVariable("userId") userId: Long): UserResponse
}

/**
 * Fallback реализация при недоступности сервиса
 */
@Component
class UserServiceClientFallback : UserServiceClient {
    override fun getUserById(userId: Long): UserResponse {
        throw ServiceUnavailableException("User service is currently unavailable. Please try again later.")
    }
}

/**
 * Конфигурация Feign Client
 */
@Configuration
class FeignClientConfig {
    @Bean
    fun errorDecoder(): ErrorDecoder {
        return ErrorDecoder { _, response ->
            when (response.status()) {
                404 -> ResourceNotFoundException("User not found")
                500, 502, 503 -> ServiceUnavailableException(
                    "User service is currently unavailable"
                )
                else -> RuntimeException("HTTP ${response.status()}")
            }
        }
    }
}
