package ru.itmo.market.infrastructure.client

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.context.annotation.Configuration
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import ru.itmo.market.application.dto.response.UserResponse
import org.springframework.context.annotation.Bean
import feign.codec.ErrorDecoder
import org.springframework.stereotype.Component
import ru.itmo.market.adapters.exception.ServiceUnavailableException
import ru.itmo.market.adapters.exception.ResourceNotFoundException
import ru.itmo.market.adapters.exception.BadRequestException

@FeignClient(
    name = "user-service",
    fallback = UserServiceClientFallback::class,
    configuration = [UserServiceFeignConfig::class]
)
interface UserServiceClient {
    
    @GetMapping("/api/users/{id}")
    fun getUserById(@PathVariable id: Long): UserResponse
}

/**
 * Fallback реализация при недоступности User Service
 */
@Component
class UserServiceClientFallback : UserServiceClient {
    
    override fun getUserById(id: Long): UserResponse {
        throw ServiceUnavailableException(
            message = "User Service недоступен. ID: $id"
        )
    }
}

@Configuration
class UserServiceFeignConfig {

    @Bean
    fun userServiceErrorDecoder(): ErrorDecoder {
        return ErrorDecoder { _, response ->
            when (response.status()) {
                400 -> BadRequestException("Invalid user_id")
                404 -> ResourceNotFoundException("User not found")
                500, 502, 503 -> ServiceUnavailableException(
                    message = "User service is currently unavailable"
                )
                else -> RuntimeException("HTTP ${response.status()}")
            }
        }
    }
}
