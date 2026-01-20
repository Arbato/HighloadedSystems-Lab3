package ru.itmo.orderservice

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = [
    "spring.cloud.config.enabled=false",
    "spring.cloud.config.import-check.enabled=false",
    "eureka.client.enabled=false",
    "spring.kafka.enabled=false"
])
@DisplayName("Application Context Tests")
class OrderServiceApplicationTests {

    @Test
    @DisplayName("Context loads successfully")
    fun contextLoads() {
        // Verify application context loads
    }
}
