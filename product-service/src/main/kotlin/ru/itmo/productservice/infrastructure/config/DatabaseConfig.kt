package ru.itmo.productservice.infrastructure.config

import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

/**
 * Конфигурация для JPA аудита (создание и изменение)
 */
@Configuration
@EnableJpaAuditing
class DatabaseConfig
