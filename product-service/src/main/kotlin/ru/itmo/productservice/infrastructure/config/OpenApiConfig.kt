package ru.itmo.productservice.infrastructure.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.servers.Server
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Конфигурация OpenAPI / Swagger документации
 */
@Configuration
class OpenApiConfig {
    
    @Bean
    fun customOpenAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("Product Service API")
                    .version("1.0.0")
                    .description("Product and Shop management microservice")
                    .contact(
                        Contact()
                            .name("ITMO Market")
                            .url("https://github.com/itmo-market")
                            .email("support@itmo-market.ru")
                    )
                    .license(
                        License()
                            .name("Apache 2.0")
                            .url("https://www.apache.org/licenses/LICENSE-2.0.html")
                    )
            )
            .addServersItem(Server().url("http://localhost:8080").description("Development"))
            .addServersItem(Server().url("http://product-service:8080").description("Docker"))
    }
}
