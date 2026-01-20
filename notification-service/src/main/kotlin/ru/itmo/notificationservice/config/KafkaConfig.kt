package ru.itmo.notificationservice.config

import org.apache.kafka.clients.admin.NewTopic
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.TopicBuilder

@Configuration
@ConditionalOnProperty(name = ["spring.kafka.enabled"], havingValue = "true", matchIfMissing = true)
class KafkaConfig {

    @Bean
    fun orderEventsTopic(): NewTopic {
        return TopicBuilder.name("order-events")
            .partitions(3)
            .replicas(3)
            .build()
    }

    @Bean
    fun moderationEventsTopic(): NewTopic {
        return TopicBuilder.name("moderation-events")
            .partitions(3)
            .replicas(3)
            .build()
    }

    @Bean
    fun notificationsTopic(): NewTopic {
        return TopicBuilder.name("notifications")
            .partitions(3)
            .replicas(3)
            .build()
    }
}
