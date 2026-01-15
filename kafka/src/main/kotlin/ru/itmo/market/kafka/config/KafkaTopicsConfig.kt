package ru.itmo.market.kafka.config

import org.apache.kafka.clients.admin.AdminClientConfig
import org.apache.kafka.clients.admin.NewTopic
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.KafkaAdmin

/**
 * Kafka Topics configuration
 * Automatically creates topics on startup
 */
@Configuration
class KafkaTopicsConfig(
    @Value("\${kafka.bootstrap-servers:localhost:9092}")
    private val bootstrapServers: String,

    @Value("\${kafka.topics.partitions:3}")
    private val partitions: Int,

    @Value("\${kafka.topics.replication-factor:1}")
    private val replicationFactor: Short
) {

    @Bean
    fun kafkaAdmin(): KafkaAdmin {
        return KafkaAdmin(mapOf(
            AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers
        ))
    }

    // ============ USER SERVICE TOPICS ============

    @Bean
    fun userRegisteredTopic(): NewTopic {
        return NewTopic("user.registered", partitions, replicationFactor)
            .configs(
                mapOf(
                    "retention.ms" to "604800000",  // 7 days
                    "compression.type" to "snappy",
                    "cleanup.policy" to "delete"
                )
            )
    }

    @Bean
    fun userRoleAssignedTopic(): NewTopic {
        return NewTopic("user.role.assigned", partitions, replicationFactor)
            .configs(
                mapOf(
                    "retention.ms" to "604800000",
                    "compression.type" to "snappy",
                    "cleanup.policy" to "delete"
                )
            )
    }

    @Bean
    fun userDeletedTopic(): NewTopic {
        return NewTopic("user.deleted", partitions, replicationFactor)
            .configs(
                mapOf(
                    "retention.ms" to "2592000000",  // 30 days (audit)
                    "compression.type" to "snappy",
                    "cleanup.policy" to "delete"
                )
            )
    }

    @Bean
    fun userProfileUpdatedTopic(): NewTopic {
        return NewTopic("user.profile.updated", partitions, replicationFactor)
            .configs(
                mapOf(
                    "retention.ms" to "604800000",
                    "compression.type" to "snappy",
                    "cleanup.policy" to "delete"
                )
            )
    }

    // ============ PRODUCT SERVICE TOPICS ============

    @Bean
    fun productCreatedTopic(): NewTopic {
        return NewTopic("product.created", partitions, replicationFactor)
            .configs(
                mapOf(
                    "retention.ms" to "1209600000",  // 14 days
                    "compression.type" to "snappy",
                    "cleanup.policy" to "delete"
                )
            )
    }

    @Bean
    fun productUpdatedTopic(): NewTopic {
        return NewTopic("product.updated", partitions, replicationFactor)
            .configs(
                mapOf(
                    "retention.ms" to "604800000",
                    "compression.type" to "snappy",
                    "cleanup.policy" to "delete"
                )
            )
    }

    @Bean
    fun productDeletedTopic(): NewTopic {
        return NewTopic("product.deleted", partitions, replicationFactor)
            .configs(
                mapOf(
                    "retention.ms" to "2592000000",  // 30 days
                    "compression.type" to "snappy",
                    "cleanup.policy" to "delete"
                )
            )
    }

    @Bean
    fun moderationRequestedTopic(): NewTopic {
        return NewTopic("moderation.requested", partitions, replicationFactor)
            .configs(
                mapOf(
                    "retention.ms" to "1209600000",  // 14 days
                    "compression.type" to "snappy",
                    "cleanup.policy" to "delete"
                )
            )
    }

    @Bean
    fun inventoryChangedTopic(): NewTopic {
        return NewTopic("inventory.changed", partitions, replicationFactor)
            .configs(
                mapOf(
                    "retention.ms" to "604800000",
                    "compression.type" to "snappy",
                    "cleanup.policy" to "delete"
                )
            )
    }

    // ============ ORDER SERVICE TOPICS ============

    @Bean
    fun orderCreatedTopic(): NewTopic {
        return NewTopic("order.created", partitions, replicationFactor)
            .configs(
                mapOf(
                    "retention.ms" to "2592000000",  // 30 days (important)
                    "compression.type" to "snappy",
                    "cleanup.policy" to "delete"
                )
            )
    }

    @Bean
    fun orderPaidTopic(): NewTopic {
        return NewTopic("order.paid", partitions, replicationFactor)
            .configs(
                mapOf(
                    "retention.ms" to "2592000000",
                    "compression.type" to "snappy",
                    "cleanup.policy" to "delete"
                )
            )
    }

    @Bean
    fun orderShippedTopic(): NewTopic {
        return NewTopic("order.shipped", partitions, replicationFactor)
            .configs(
                mapOf(
                    "retention.ms" to "2592000000",
                    "compression.type" to "snappy",
                    "cleanup.policy" to "delete"
                )
            )
    }

    @Bean
    fun orderDeliveredTopic(): NewTopic {
        return NewTopic("order.delivered", partitions, replicationFactor)
            .configs(
                mapOf(
                    "retention.ms" to "2592000000",
                    "compression.type" to "snappy",
                    "cleanup.policy" to "delete"
                )
            )
    }

    @Bean
    fun orderCancelledTopic(): NewTopic {
        return NewTopic("order.cancelled", partitions, replicationFactor)
            .configs(
                mapOf(
                    "retention.ms" to "2592000000",
                    "compression.type" to "snappy",
                    "cleanup.policy" to "delete"
                )
            )
    }

    @Bean
    fun cartItemsAddedTopic(): NewTopic {
        return NewTopic("cart.items.added", partitions, replicationFactor)
            .configs(
                mapOf(
                    "retention.ms" to "604800000",  // 7 days
                    "compression.type" to "snappy",
                    "cleanup.policy" to "delete"
                )
            )
    }

    // ============ NOTIFICATION SERVICE TOPICS ============

    @Bean
    fun sendEmailNotificationTopic(): NewTopic {
        return NewTopic("notification.email.send", partitions, replicationFactor)
            .configs(
                mapOf(
                    "retention.ms" to "604800000",  // 7 days
                    "compression.type" to "snappy",
                    "cleanup.policy" to "delete"
                )
            )
    }

    @Bean
    fun sendSmsNotificationTopic(): NewTopic {
        return NewTopic("notification.sms.send", partitions, replicationFactor)
            .configs(
                mapOf(
                    "retention.ms" to "604800000",
                    "compression.type" to "snappy",
                    "cleanup.policy" to "delete"
                )
            )
    }

    @Bean
    fun sendPushNotificationTopic(): NewTopic {
        return NewTopic("notification.push.send", partitions, replicationFactor)
            .configs(
                mapOf(
                    "retention.ms" to "604800000",
                    "compression.type" to "snappy",
                    "cleanup.policy" to "delete"
                )
            )
    }

    @Bean
    fun createInAppNotificationTopic(): NewTopic {
        return NewTopic("notification.inapp.create", partitions, replicationFactor)
            .configs(
                mapOf(
                    "retention.ms" to "604800000",
                    "compression.type" to "snappy",
                    "cleanup.policy" to "delete"
                )
            )
    }

    @Bean
    fun notificationSentTopic(): NewTopic {
        return NewTopic("notification.sent", partitions, replicationFactor)
            .configs(
                mapOf(
                    "retention.ms" to "604800000",
                    "compression.type" to "snappy",
                    "cleanup.policy" to "delete"
                )
            )
    }

    @Bean
    fun notificationFailedTopic(): NewTopic {
        return NewTopic("notification.failed", partitions, replicationFactor)
            .configs(
                mapOf(
                    "retention.ms" to "1209600000",  // 14 days (audit)
                    "compression.type" to "snappy",
                    "cleanup.policy" to "delete"
                )
            )
    }

    // ============ MODERATION SERVICE TOPICS ============

    @Bean
    fun moderationTaskCreatedTopic(): NewTopic {
        return NewTopic("moderation.task.created", partitions, replicationFactor)
            .configs(
                mapOf(
                    "retention.ms" to "1209600000",  // 14 days
                    "compression.type" to "snappy",
                    "cleanup.policy" to "delete"
                )
            )
    }

    @Bean
    fun productApprovedTopic(): NewTopic {
        return NewTopic("product.approved", partitions, replicationFactor)
            .configs(
                mapOf(
                    "retention.ms" to "1209600000",
                    "compression.type" to "snappy",
                    "cleanup.policy" to "delete"
                )
            )
    }

    @Bean
    fun productRejectedTopic(): NewTopic {
        return NewTopic("product.rejected", partitions, replicationFactor)
            .configs(
                mapOf(
                    "retention.ms" to "1209600000",
                    "compression.type" to "snappy",
                    "cleanup.policy" to "delete"
                )
            )
    }

    @Bean
    fun productRevisionRequestedTopic(): NewTopic {
        return NewTopic("product.revision.requested", partitions, replicationFactor)
            .configs(
                mapOf(
                    "retention.ms" to "1209600000",
                    "compression.type" to "snappy",
                    "cleanup.policy" to "delete"
                )
            )
    }

    @Bean
    fun moderationTaskCompletedTopic(): NewTopic {
        return NewTopic("moderation.task.completed", partitions, replicationFactor)
            .configs(
                mapOf(
                    "retention.ms" to "1209600000",  // 14 days
                    "compression.type" to "snappy",
                    "cleanup.policy" to "delete"
                )
            )
    }
}