package ru.itmo.userservice.adapter.out.messaging.kafka.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.support.MessageBuilder
import org.springframework.stereotype.Component
import ru.itmo.userservice.domain.model.dto.kafka.UserServiceRequest
import ru.itmo.userservice.domain.model.dto.kafka.UserServiceResponse
import ru.itmo.userservice.domain.model.dto.kafka.UserData
import ru.itmo.userservice.application.service.UserService
import reactor.core.scheduler.Schedulers

@Component
class ProductServiceRequestListener(
    private val userService: UserService,
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = arrayOf("user-service-requests"),
        groupId = "user-service-request-processor",
        containerFactory = "kafkaListenerContainerFactory"
    )
    fun handleProductServiceRequest(
        @Payload requestJson: String,
        @Header("requestId") requestId: String,
        @Header("replyTopic", required = false) replyTopic: String? = "product-service-replies"
    ) {
        try {
            logger.info("Received RPC request from product-service: requestId=$requestId")
            
            val request = objectMapper.readValue(requestJson, UserServiceRequest::class.java)
            
            val response = when (request.requestType) {
                "GET_USER_BY_ID" -> handleGetUserById(request.userId!!)
                else -> {
                    logger.warn("Unknown request type: ${request.requestType}")
                    UserServiceResponse(
                        requestId = requestId,
                        success = false,
                        errorMessage = "Unknown request type: ${request.requestType}"
                    )
                }
            }
            
            sendResponse(response, requestId, replyTopic!!)
            
        } catch (e: Exception) {
            logger.error("Error processing request: requestId=$requestId", e)
            val errorResponse = UserServiceResponse(
                requestId = requestId,
                success = false,
                errorMessage = e.message ?: "Internal error"
            )
            sendResponse(errorResponse, requestId, replyTopic!!)
        }
    }

    private fun handleGetUserById(userId: Long): UserServiceResponse {
        return try {
            val user = userService.getUserById(userId)
                .subscribeOn(Schedulers.boundedElastic())
                .block()
            
            if (user != null) {
                UserServiceResponse(
                    requestId = "",
                    success = true,
                    user = UserData(
                        id = user.id,
                        username = user.username,
                        email = user.email,
                        firstName = user.firstName,
                        lastName = user.lastName,
                        roles = user.roles.toSet(),
                        createdAt = user.createdAt, 
                        updatedAt = user.updatedAt, 
                    )
                )
            } else {
                UserServiceResponse(
                    requestId = "",
                    success = false,
                    errorMessage = "User not found: $userId"
                )
            }
        } catch (e: Exception) {
            UserServiceResponse(
                requestId = "",
                success = false,
                errorMessage = e.message ?: "Internal error"
            )
        }
    }

    private fun sendResponse(
        response: UserServiceResponse,
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
            logger.info("Sent RPC response: requestId=$requestId, success=${response.success}")
        } catch (e: Exception) {
            logger.error("Error sending response: requestId=$requestId", e)
        }
    }
}
