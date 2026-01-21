package ru.itmo.notificationservice.config

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

    /**
     * Топик для событий пользователя (3 реплики)
     */
    @Bean
    fun userEventsTopic(): NewTopic {
        return NewTopic("user-events", 1, 3.toShort())
    }

    /**
     * Топик для команд отправки уведомлений (3 реплики)
     */
    @Bean
    fun notificationCommandsTopic(): NewTopic {
        return NewTopic("notification-commands", 1, 3.toShort())
    }

    /**
     * Dead Letter Queue топик (3 реплики)
     */
    @Bean
    fun notificationDlqTopic(): NewTopic {
        return NewTopic("notification-dlq", 1, 3.toShort())
    }
}
