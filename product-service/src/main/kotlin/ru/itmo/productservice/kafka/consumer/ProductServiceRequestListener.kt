package ru.itmo.productservice.kafka.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.support.MessageBuilder
import org.springframework.stereotype.Component
import ru.itmo.productservice.model.dto.kafka.ProductServiceRequest
import ru.itmo.productservice.model.dto.kafka.ProductServiceResponse
import ru.itmo.productservice.model.dto.kafka.ProductData
import ru.itmo.productservice.service.ProductService
import java.time.LocalDateTime

@Component
class ProductServiceRequestListener(
    private val productService: ProductService,
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = arrayOf("product-service-requests"),
        groupId = "product-service-request-processor"
    )
    fun handleProductRequest(
        @Payload requestJson: String,
        @Header("requestId") requestId: String,
        @Header("replyTopic", required = true) replyTopic: String
    ) {
        try {
            logger.info("Received RPC request: requestId=$requestId, replyTopic=$replyTopic")
            
            val request = objectMapper.readValue(requestJson, ProductServiceRequest::class.java)
            
            val response = when (request.requestType) {
                "GET_PRODUCT_BY_ID" -> handleGetProductById(request.productId!!)
                else -> {
                    logger.warn("Unknown request type: ${request.requestType}")
                    ProductServiceResponse(
                        success = false,
                        errorMessage = "Unknown request type: ${request.requestType}"
                    )
                }
            }
            
            sendResponse(response, requestId, replyTopic)
            
        } catch (e: Exception) {
            logger.error("Error processing request: requestId=$requestId", e)
            val errorResponse = ProductServiceResponse(
                success = false,
                errorMessage = e.message ?: "Internal error"
            )
            sendResponse(errorResponse, requestId, replyTopic)
        }
    }


    private fun handleGetProductById(productId: Long): ProductServiceResponse {
        return try {
            val product = productService.getProductById(productId)
            
            ProductServiceResponse(
                success = true,
                product = ProductData(
                    id = product.id,
                    name = product.name,
                    description = product.description,
                    price = product.price,
                    imageUrl = product.imageUrl,
                    shopId = product.shopId,
                    sellerId = product.sellerId,
                    status = product.status,
                    rejectionReason = product.rejectionReason,
                    averageRating = null,  // TODO: добавить рейтинг если нужно
                    commentsCount = null,  // TODO: добавить комментарии если нужно
                    createdAt = product.createdAt,
                    updatedAt = product.updatedAt
                )
            )
        } catch (e: Exception) {
            ProductServiceResponse(
                success = false,
                errorMessage = e.message ?: "Internal error"
            )
        }
    }


    private fun sendResponse(
        response: ProductServiceResponse,
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
