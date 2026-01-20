package ru.itmo.orderservice.client

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.Message
import org.springframework.messaging.support.MessageBuilder
import org.springframework.stereotype.Component
import ru.itmo.orderservice.model.dto.kafka.ProductServiceRequest
import ru.itmo.orderservice.model.dto.kafka.ProductServiceResponse
import ru.itmo.orderservice.model.dto.kafka.UserServiceRequest
import ru.itmo.orderservice.model.dto.kafka.UserServiceResponse
import ru.itmo.orderservice.model.dto.response.ProductResponse
import ru.itmo.orderservice.model.dto.response.UserResponse
import ru.itmo.orderservice.exception.ResourceNotFoundException
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
    
    fun getProductById(productId: Long): ProductResponse {
        val requestId = UUID.randomUUID().toString()
        logger.info("Requesting product with ID $productId (requestId: $requestId)")
        
        val request = ProductServiceRequest(
            requestType = "GET_PRODUCT_BY_ID",
            productId = productId
        )
        
        val response = sendProductRequest(requestId, request)
        
        if (!response.success) {
            logger.warn("Product service returned error: ${response.errorMessage}")
            throw ResourceNotFoundException("Product not found: $productId")
        }
        
        val product = response.product
            ?: throw ResourceNotFoundException("Product not found: $productId")
        
        return ProductResponse(
            id = product.id,
            name = product.name,
            description = product.description,
            price = product.price,
            imageUrl = product.imageUrl,
            shopId = product.shopId,
            sellerId = product.sellerId,
            status = product.status,
            rejectionReason = product.rejectionReason,
            averageRating = product.averageRating,
            commentsCount = product.commentsCount,
            createdAt = product.createdAt,
            updatedAt = product.updatedAt
        )
    }
    
    fun getUserById(userId: Long): UserResponse {
        val requestId = UUID.randomUUID().toString()
        logger.info("Requesting user with ID $userId (requestId: $requestId)")
        
        val request = UserServiceRequest(
            requestType = "GET_USER_BY_ID",
            userId = userId
        )
        
        val response = sendUserRequest(requestId, request)
        
        if (!response.success) {
            logger.warn("User service returned error: ${response.errorMessage}")
            throw ResourceNotFoundException("User not found: $userId")
        }
        
        val user = response.user
            ?: throw ResourceNotFoundException("User not found: $userId")
        
        return UserResponse(
            id = user.id,
            username = user.username,
            email = user.email,
            firstName = user.firstName,
            lastName = user.lastName,
            roles = user.roles,
            createdAt = user.createdAt,
            updatedAt = user.updatedAt
        )
    }
    
    private fun sendProductRequest(requestId: String, request: ProductServiceRequest): ProductServiceResponse {
        val latch = CountDownLatch(1)
        val pendingRequest = PendingRequest(latch = latch)
        pendingRequests[requestId] = pendingRequest
        
        try {
            val requestJson = objectMapper.writeValueAsString(request)
            
            val message: Message<String> = MessageBuilder
                .withPayload(requestJson)
                .setHeader(KafkaHeaders.TOPIC, PRODUCT_SERVICE_REQUEST_TOPIC)
                .setHeader("requestId", requestId)
                .setHeader("replyTopic", ORDER_SERVICE_REPLY_TOPIC)
                .build()
            
            kafkaTemplate.send(message)
            logger.debug("Product request sent to Kafka (requestId: $requestId)")
            
            val received = latch.await(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            
            if (!received) {
                logger.error("Request timeout after $REQUEST_TIMEOUT_SECONDS seconds (requestId: $requestId)")
                throw RuntimeException("Product service request timeout")
            }
            
            @Suppress("UNCHECKED_CAST")
            val response = pendingRequest.response as? ProductServiceResponse
                ?: throw RuntimeException("No response received from product service")
            
            logger.info("Received product response for requestId: $requestId")
            return response
            
        } finally {
            pendingRequests.remove(requestId)
        }
    }
    
    private fun sendUserRequest(requestId: String, request: UserServiceRequest): UserServiceResponse {
        val latch = CountDownLatch(1)
        val pendingRequest = PendingRequest(latch = latch)
        pendingRequests[requestId] = pendingRequest
        
        try {
            val requestJson = objectMapper.writeValueAsString(request)
            
            val message: Message<String> = MessageBuilder
                .withPayload(requestJson)
                .setHeader(KafkaHeaders.TOPIC, USER_SERVICE_REQUEST_TOPIC)
                .setHeader("requestId", requestId)
                .setHeader("replyTopic", ORDER_SERVICE_REPLY_TOPIC)
                .build()
            
            kafkaTemplate.send(message)
            logger.debug("User request sent to Kafka (requestId: $requestId)")
            
            val received = latch.await(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            
            if (!received) {
                logger.error("Request timeout after $REQUEST_TIMEOUT_SECONDS seconds (requestId: $requestId)")
                throw RuntimeException("User service request timeout")
            }
            
            @Suppress("UNCHECKED_CAST")
            val response = pendingRequest.response as? UserServiceResponse
                ?: throw RuntimeException("No response received from user service")
            
            logger.info("Received user response for requestId: $requestId")
            return response
            
        } finally {
            pendingRequests.remove(requestId)
        }
    }
    
    fun handleResponse(responseJson: String, requestId: String) {
        try {
            logger.debug("Handling response for requestId: $requestId")
            
            val pendingRequest = pendingRequests[requestId]
            if (pendingRequest == null) {
                logger.warn("Received response for unknown requestId: $requestId")
                return
            }
            
            val productResponse = try {
                objectMapper.readValue(responseJson, ProductServiceResponse::class.java)
            } catch (e: Exception) {
                null
            }
            
            if (productResponse != null) {
                pendingRequest.response = productResponse
                pendingRequest.latch.countDown()
                return
            }
            
            val userResponse = try {
                objectMapper.readValue(responseJson, UserServiceResponse::class.java)
            } catch (e: Exception) {
                null
            }
            
            if (userResponse != null) {
                pendingRequest.response = userResponse
                pendingRequest.latch.countDown()
                return
            }
            
            logger.error("Could not deserialize response for requestId: $requestId")
            pendingRequest.latch.countDown()
            
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
        var response: Any? = null
    )
}
