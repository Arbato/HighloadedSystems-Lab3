package ru.itmo.userservice.infrastructure.config

import org.apache.kafka.clients.admin.AdminClientConfig
import org.apache.kafka.clients.admin.NewTopic
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.core.KafkaAdmin


@Configuration
@EnableKafka
class KafkaConfig(
    @Value("\${spring.kafka.bootstrap-servers:kafka:9092,kafka-2:9093,kafka-3:9094}")
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
