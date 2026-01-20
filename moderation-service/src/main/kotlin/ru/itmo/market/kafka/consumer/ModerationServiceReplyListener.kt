package ru.itmo.market.listener

import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import ru.itmo.market.client.KafkaRpcClient

@Component
class ModerationServiceReplyListener(
    private val kafkaRpcClient: KafkaRpcClient
) {
    
    private val logger = LoggerFactory.getLogger(ModerationServiceReplyListener::class.java)
    
    @KafkaListener(
        topics = ["moderation-service-replies"],
        groupId = "moderation-service-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    fun listenReplies(
        @Payload responseJson: String,
        @Header("requestId") requestId: String
    ) {
        logger.debug("Received reply from Kafka: requestId=$requestId")
        kafkaRpcClient.handleResponse(responseJson, requestId)
    }
}
