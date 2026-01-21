package ru.itmo.orderservice.client

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.support.MessageBuilder
import org.springframework.stereotype.Component
import ru.itmo.orderservice.exception.ResourceNotFoundException
import ru.itmo.orderservice.model.dto.kafka.ProductServiceRequest
import ru.itmo.orderservice.model.dto.kafka.GetProductPayload
import ru.itmo.orderservice.model.dto.kafka.GetUserPayload
import ru.itmo.orderservice.model.dto.kafka.UserServiceResponse
import ru.itmo.orderservice.model.dto.kafka.ProductServiceResponse
import ru.itmo.orderservice.model.dto.response.ProductResponse
import ru.itmo.orderservice.model.dto.response.UserResponse
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@Component
class KafkaRpcClient(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val pendingRequests: ConcurrentHashMap<String, PendingRequest> = ConcurrentHashMap()
    
    companion object {
        private const val PRODUCT_SERVICE_REQUEST_TOPIC = "product-service-requests"
        private const val USER_SERVICE_REQUEST_TOPIC = "user-service-requests"
        private const val ORDER_SERVICE_REPLY_TOPIC = "order-service-replies"
        private const val REQUEST_TIMEOUT_SECONDS = 10L
    }
    
    fun <T> sendRequest(
        requestTopic: String,
        replyTopic: String,
        request: Any,
        responseType: TypeReference<T>
    ): T {
        val requestId = UUID.randomUUID().toString()
        logger.info("Sending RPC request: requestId=$requestId, topic=$requestTopic")
        
        val latch = CountDownLatch(1)
        val pendingRequest = PendingRequest(latch = latch)
        pendingRequests[requestId] = pendingRequest
        
        try {
            val requestJson = objectMapper.writeValueAsString(request)
            
            val message = MessageBuilder
                .withPayload(requestJson)
                .setHeader(KafkaHeaders.TOPIC, requestTopic)
                .setHeader("requestId", requestId)
                .setHeader("replyTopic", replyTopic)
                .build()
            
            kafkaTemplate.send(message)
            logger.debug("Request sent to Kafka (requestId: $requestId)")
            
            val received = latch.await(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            
            if (!received) {
                logger.error("Request timeout after $REQUEST_TIMEOUT_SECONDS seconds (requestId: $requestId)")
                throw RuntimeException("RPC request timeout for topic: $requestTopic")
            }
            
            val responseJson = pendingRequest.response as? String
                ?: throw RuntimeException("No response received for requestId: $requestId")
            
            val response = objectMapper.readValue(responseJson, responseType)
            
            logger.info("Received RPC response for requestId: $requestId")
            return response
            
        } finally {
            pendingRequests.remove(requestId)
        }
    }
    
    fun getProductById(productId: Long): ProductResponse {
        logger.info("Requesting product with ID $productId")
        
        val request = ProductServiceRequest(
            requestType = "GET_PRODUCT_BY_ID",
            payload = GetProductPayload(productId)
        )
        
        val response = sendRequest(
            requestTopic = PRODUCT_SERVICE_REQUEST_TOPIC,
            replyTopic = ORDER_SERVICE_REPLY_TOPIC,
            request = request,
            responseType = object : TypeReference<ProductServiceResponse<ProductResponse>>() {}
        )
        
        if (!response.success || response.data == null) {
            throw ResourceNotFoundException("Product not found: $productId")
        }
        
        return response.data
    }
    
    fun getUserById(userId: Long): UserResponse {
        logger.info("Requesting user with ID $userId")
        
        val request = ProductServiceRequest(
            requestType = "GET_USER_BY_ID",
            payload = GetUserPayload(userId)
        )
        
        val response = sendRequest(
            requestTopic = USER_SERVICE_REQUEST_TOPIC,
            replyTopic = ORDER_SERVICE_REPLY_TOPIC,
            request = request,
            responseType = object : TypeReference<UserServiceResponse<UserResponse>>() {}
        )
        
        if (!response.success || response.data == null) {
            throw ResourceNotFoundException("User not found: $userId")
        }
        
        return response.data
    }
    
    fun handleResponse(responseJson: String, requestId: String) {
        try {
            logger.debug("Handling response for requestId: $requestId")
            
            val pendingRequest = pendingRequests[requestId]
            if (pendingRequest == null) {
                logger.warn("Received response for unknown requestId: $requestId")
                return
            }
            
            pendingRequest.response = responseJson
            pendingRequest.latch.countDown()
            logger.debug("Response received for requestId: $requestId")
            
        } catch (e: Exception) {
            logger.error("Error processing response for requestId: $requestId", e)
            val pendingRequest = pendingRequests[requestId]
            pendingRequest?.latch?.countDown()
        }
    }
    
    private data class PendingRequest(
        val latch: CountDownLatch,
        var response: Any? = null
    )
}
