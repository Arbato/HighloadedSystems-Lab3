package ru.itmo.orderservice.infrastructure.config

import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@Configuration
@EnableJpaRepositories(basePackages = ["ru.itmo.orderservice.infrastructure.repository"])
class JpaConfig
