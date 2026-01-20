package ru.itmo.market.client

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.context.annotation.Configuration
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import ru.itmo.market.domain.model.dto.response.UserResponse
import org.springframework.context.annotation.Bean
import feign.Response
import feign.codec.ErrorDecoder
import ru.itmo.market.adapter.exception.ServiceUnavailableException
import ru.itmo.market.adapter.exception.ResourceNotFoundException
import ru.itmo.market.adapter.exception.BadRequestException
import org.springframework.boot.autoconfigure.http.HttpMessageConverters
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import com.fasterxml.jackson.databind.ObjectMapper

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
@org.springframework.stereotype.Component
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
