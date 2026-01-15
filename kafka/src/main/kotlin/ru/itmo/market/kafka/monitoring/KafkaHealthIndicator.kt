package ru.itmo.market.kafka.monitoring

import mu.KotlinLogging
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class KafkaHealthIndicator(
    private val kafkaTemplate: KafkaTemplate<String, Any>
) : HealthIndicator {

    companion object {
        private val logger = KotlinLogging.logger {}
    }

    override fun health(): Health {
        return try {
            val future = kafkaTemplate.send("health-check", "check")
            future.get(5, TimeUnit.SECONDS)
            logger.debug { "Kafka health check passed" }
            Health.up().withDetail("kafka", "Connected").build()
        } catch (e: Exception) {
            logger.error(e) { "Kafka health check failed" }
            Health.down().withDetail("error", e.message).build()
        }
    }
}
