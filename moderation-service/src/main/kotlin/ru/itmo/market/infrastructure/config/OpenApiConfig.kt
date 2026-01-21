package ru.itmo.market.infrastructure.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {
    
    @Bean
    fun openAPI(): OpenAPI {
        return OpenAPI().apply {
            info = Info().apply {
                title = "ITMO Market - Moderation Service API"
                description = "REST API для модерации товаров (Reactor + R2DBC)"
                version = "1.0.0"
            }
        }
    }
}
