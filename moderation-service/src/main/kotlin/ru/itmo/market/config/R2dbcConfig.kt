package ru.itmo.market.config

import io.r2dbc.pool.ConnectionPool
import io.r2dbc.pool.ConnectionPoolConfiguration
import io.r2dbc.postgresql.PostgresqlConnectionFactory
import io.r2dbc.postgresql.api.PostgresqlResult
import io.r2dbc.spi.ConnectionFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories
import org.springframework.r2dbc.core.DatabaseClient
import java.time.Duration

@Configuration
@EnableR2dbcRepositories(basePackages = ["ru.itmo.market.domain.repository"])
class R2dbcConfig {
    
    @Bean
    fun connectionFactory(): ConnectionFactory {
        val config = io.r2dbc.postgresql.PostgresqlConnectionConfiguration.builder()
            .host(System.getenv("DB_HOST") ?: "localhost")
            .port(System.getenv("DB_PORT")?.toInt() ?: 5432)
            .username(System.getenv("DB_USER") ?: "itmouser")
            .password(System.getenv("DB_PASSWORD") ?: "itmopassword")
            .database(System.getenv("DB_NAME") ?: "itmomarket")
            .build()
        
        val pgConnectionFactory = PostgresqlConnectionFactory(config)
        
        val poolConfig = ConnectionPoolConfiguration.builder(pgConnectionFactory)
            .maxIdleTime(Duration.ofMinutes(5))
            .initialSize(5)
            .maxSize(20)
            .validationQuery("SELECT 1")
            .build()
        
        return ConnectionPool(poolConfig)
    }
    
    @Bean
    fun databaseClient(connectionFactory: ConnectionFactory): DatabaseClient {
        return DatabaseClient.create(connectionFactory)
    }
}
