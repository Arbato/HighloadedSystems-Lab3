package ru.itmo.market.adapter.out.messaging.kafka.consumer

import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import ru.itmo.market.adapter.out.messaging.kafka.client.KafkaRpcClient

@Component
@ConditionalOnProperty(name = ["spring.kafka.enabled"], havingValue = "true", matchIfMissing = true)
class KafkaConsumer(
    private val kafkaRpcClient: KafkaRpcClient
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = ["moderation-service-replies"],
        groupId = "moderation-service-rpc-replies"
    )
    fun handleServiceResponse(
        @Payload response: String,
        @Header("requestId") requestId: String
    ) {
        logger.debug("Received response: requestId=$requestId")

        // Try to handle as product response first, then as user response
        try {
            if (response.contains("\"product\"") || response.contains("\"products\"")) {
                kafkaRpcClient.handleProductResponse(response, requestId)
            } else if (response.contains("\"user\"")) {
                kafkaRpcClient.handleUserResponse(response, requestId)
            } else {
                // Try both - one will succeed
                try {
                    kafkaRpcClient.handleProductResponse(response, requestId)
                } catch (e: Exception) {
                    kafkaRpcClient.handleUserResponse(response, requestId)
                }
            }
        } catch (e: Exception) {
            logger.error("Error handling response for requestId=$requestId", e)
        }
    }
}
