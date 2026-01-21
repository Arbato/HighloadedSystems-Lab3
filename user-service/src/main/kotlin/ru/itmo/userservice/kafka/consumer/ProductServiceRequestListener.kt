package ru.itmo.userservice.kafka.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.support.MessageBuilder
import org.springframework.stereotype.Component
import ru.itmo.userservice.model.dto.kafka.GetUserPayload
import ru.itmo.userservice.model.dto.kafka.UserServiceResponse
import ru.itmo.userservice.model.dto.response.UserResponse
import ru.itmo.userservice.service.UserService
import reactor.core.scheduler.Schedulers

@Component
class UserServiceRequestListener(
    private val userService: UserService,
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = ["user-service-requests"],
        groupId = "user-service-request-processor"
    )
    fun handleUserRequest(
        @Payload requestJson: String,
        @Header("requestId") requestId: String,
        @Header("replyTopic", required = true) replyTopic: String
    ) {
        try {
            logger.info("Received RPC request: requestId=$requestId, replyTopic=$replyTopic")
            
            val requestNode = objectMapper.readTree(requestJson)
            val requestType = requestNode.get("request_type")?.asText()
            
            val response: UserServiceResponse<*> = when (requestType) {
                "GET_USER_BY_ID" -> {
                    val payload = objectMapper.treeToValue(
                        requestNode.get("payload"),
                        GetUserPayload::class.java
                    )
                    handleGetUserById(payload.userId)
                }
                
                else -> {
                    logger.warn("Unknown request type: $requestType")
                    UserServiceResponse<UserResponse>(
                        success = false,
                        errorMessage = "Unknown request type: $requestType"
                    )
                }
            }
            
            sendResponse(response, requestId, replyTopic)
            
        } catch (e: Exception) {
            logger.error("Error processing request: requestId=$requestId", e)
            val errorResponse = UserServiceResponse<UserResponse>(
                success = false,
                errorMessage = e.message ?: "Internal error"
            )
            sendResponse(errorResponse, requestId, replyTopic)
        }
    }

    private fun handleGetUserById(userId: Long): UserServiceResponse<UserResponse> {
        return try {
            val user = userService.getUserById(userId)
                .subscribeOn(Schedulers.boundedElastic())
                .block()
            
            if (user != null) {
                UserServiceResponse(
                    success = true,
                    data = UserResponse(
                        id = user.id,
                        username = user.username,
                        email = user.email,
                        firstName = user.firstName,
                        lastName = user.lastName,
                        roles = user.roles.toSet(),
                        createdAt = user.createdAt,
                        updatedAt = user.updatedAt
                    )
                )
            } else {
                UserServiceResponse(
                    success = false,
                    errorMessage = "User not found: $userId"
                )
            }
        } catch (e: Exception) {
            logger.error("Error getting user with id: $userId", e)
            UserServiceResponse(
                success = false,
                errorMessage = e.message ?: "Internal error"
            )
        }
    }

    private fun sendResponse(
        response: UserServiceResponse<*>,
        requestId: String,
        replyTopic: String
    ) {
        try {
            val responseJson = objectMapper.writeValueAsString(response)
            val message = MessageBuilder.withPayload(responseJson)
                .setHeader(KafkaHeaders.TOPIC, replyTopic)
                .setHeader("requestId", requestId)
                .build()
            
            kafkaTemplate.send(message)
            logger.info("Sent RPC response: requestId=$requestId, replyTopic=$replyTopic, success=${response.success}")
        } catch (e: Exception) {
            logger.error("Error sending response: requestId=$requestId", e)
        }
    }
}
