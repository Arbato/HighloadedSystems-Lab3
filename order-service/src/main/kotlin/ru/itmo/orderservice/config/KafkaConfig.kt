package ru.itmo.orderservice.config

import org.apache.kafka.clients.admin.AdminClientConfig
import org.apache.kafka.clients.admin.NewTopic
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.core.KafkaAdmin

@Configuration
@EnableKafka
@ConditionalOnProperty(name = ["spring.kafka.enabled"], havingValue = "true", matchIfMissing = true)
class KafkaConfig(
    @Value("\${spring.kafka.bootstrap-servers}")
    private val bootstrapServers: String
) {

    @Bean
    fun kafkaAdmin(): KafkaAdmin {
        return KafkaAdmin(
            mapOf(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers)
        )
    }

    @Bean
    fun productServiceRequestsTopic(): NewTopic {
        return NewTopic("product-service-requests", 3, 3.toShort())
    }

    @Bean
    fun orderServiceRepliesTopic(): NewTopic {
        return NewTopic("order-service-replies", 3, 3.toShort())
    }
}
