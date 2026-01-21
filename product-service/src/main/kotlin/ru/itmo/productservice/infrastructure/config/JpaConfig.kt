package ru.itmo.productservice.infrastructure.config

import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@Configuration
@EnableJpaRepositories(basePackages = ["ru.itmo.productservice.infrastructure.repository"])
class JpaConfig
