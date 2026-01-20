package ru.itmo.productservice.config

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
    fun userServiceRequestsTopic(): NewTopic {
        return NewTopic("user-service-requests", 3, 3.toShort())
    }

    @Bean
    fun productServiceRepliesTopic(): NewTopic {
        return NewTopic("product-service-replies", 3, 3.toShort())
    }
}
