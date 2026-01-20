package ru.itmo.orderservice.config

import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@Configuration
@EnableJpaRepositories(basePackages = ["ru.itmo.orderservice.domain.repository"])
class JpaConfig
