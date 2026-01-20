package ru.itmo.productservice.client

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.Message
import org.springframework.messaging.support.MessageBuilder
import org.springframework.stereotype.Component
import ru.itmo.productservice.model.dto.kafka.UserServiceRequest
import ru.itmo.productservice.model.dto.kafka.UserServiceResponse
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@Component
class KafkaRpcClient(
    private val kafkaTemplate: KafkaTemplate<String, Any>,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val pendingRequests: ConcurrentHashMap<String, PendingRequest> = ConcurrentHashMap()
    
    companion object {
        private const val USER_SERVICE_REQUEST_TOPIC = "user-service-requests"
        private const val PRODUCT_SERVICE_REPLY_TOPIC = "product-service-replies"
        private const val REQUEST_TIMEOUT_SECONDS = 10L
    }
    
    fun getUserById(userId: Long): UserServiceResponse {
        val requestId = UUID.randomUUID().toString()
        logger.info("Requesting user with ID $userId (requestId: $requestId)")
        
        val request = UserServiceRequest(
            requestId = requestId,
            requestType = "GET_USER_BY_ID",
            userId = userId
        )
        
        return sendRequest(requestId, request)
    }
    
    private fun sendRequest(requestId: String, request: UserServiceRequest): UserServiceResponse {
        val latch = CountDownLatch(1)
        val pendingRequest = PendingRequest(latch = latch)
        pendingRequests[requestId] = pendingRequest
        
        try {
            
            val message: Message<UserServiceRequest> = MessageBuilder
                .withPayload(request)
                .setHeader(KafkaHeaders.TOPIC, "user-service-requests")
                .setHeader("requestId", requestId)
                .setHeader("replyTopic", PRODUCT_SERVICE_REPLY_TOPIC)
                .build()
            
            kafkaTemplate.send(message)
            logger.debug("Request sent to Kafka (requestId: $requestId)")
            
            val received = latch.await(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            
            if (!received) {
                logger.error("Request timeout after $REQUEST_TIMEOUT_SECONDS seconds (requestId: $requestId)")
                throw RuntimeException("User service request timeout")
            }
            
            val response = pendingRequest.response
                ?: throw RuntimeException("No response received from user service")
            
            if (!response.success) {
                logger.warn("User service returned error: ${response.errorMessage}")
                throw RuntimeException("User service error: ${response.errorMessage}")
            }
            
            logger.info("Received response for requestId: $requestId")
            return response
            
        } finally {
            pendingRequests.remove(requestId)
        }
    }
    
    fun handleResponse(responseJson: String, requestId: String) {
        try {
            logger.debug("Handling response for requestId: $requestId")
            val response = objectMapper.readValue(responseJson, UserServiceResponse::class.java)
            
            val pendingRequest = pendingRequests[requestId]
            if (pendingRequest != null) {
                pendingRequest.response = response
                pendingRequest.latch.countDown()
            } else {
                logger.warn("Received response for unknown requestId: $requestId")
            }
        } catch (e: Exception) {
            logger.error("Error processing response for requestId: $requestId", e)
            val pendingRequest = pendingRequests[requestId]
            if (pendingRequest != null) {
                pendingRequest.latch.countDown()
            }
        }
    }
    
    private data class PendingRequest(
        val latch: CountDownLatch,
        var response: UserServiceResponse? = null
    )
}
