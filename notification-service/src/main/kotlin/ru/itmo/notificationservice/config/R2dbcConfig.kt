package ru.itmo.notificationservice.config

import io.r2dbc.postgresql.PostgresqlConnectionConfiguration
import io.r2dbc.postgresql.PostgresqlConnectionFactory
import io.r2dbc.spi.ConnectionFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration
import org.springframework.r2dbc.core.DatabaseClient

@Configuration
@EnableConfigurationProperties(R2dbcProperties::class)
class R2dbcConfig : AbstractR2dbcConfiguration() {
    
    override fun connectionFactory(): ConnectionFactory {
        return PostgresqlConnectionFactory(
            PostgresqlConnectionConfiguration.builder()
                .host("localhost")
                .port(5432)
                .database("itmo_market_notification")
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
