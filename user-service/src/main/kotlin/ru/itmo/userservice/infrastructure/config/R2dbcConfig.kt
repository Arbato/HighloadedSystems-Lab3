package ru.itmo.userservice.infrastructure.config

import io.r2dbc.postgresql.PostgresqlConnectionConfiguration
import io.r2dbc.postgresql.PostgresqlConnectionFactory
import io.r2dbc.spi.ConnectionFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration

/**
 * R2DBC конфигурация для PostgreSQL
 * Используется для реактивного доступа к БД
 */
@Configuration
@EnableConfigurationProperties(R2dbcProperties::class)
class R2dbcConfig : AbstractR2dbcConfiguration() {
    
    /**
     * Создание ConnectionFactory для R2DBC
     * Автоматически используется через application.yml конфигурацию
     */
    override fun connectionFactory(): ConnectionFactory {
        return PostgresqlConnectionFactory(
            PostgresqlConnectionConfiguration.builder()
                .host("localhost")
                .port(5432)
                .database("itmo_market")
                .username("itmo_user")
                .password("itmo_password")
                .build()
        )
    }
}

@ConfigurationProperties(prefix = "spring.r2dbc")
data class R2dbcProperties(
    var url: String = "",
    var username: String = "",
    var password: String = ""
)
