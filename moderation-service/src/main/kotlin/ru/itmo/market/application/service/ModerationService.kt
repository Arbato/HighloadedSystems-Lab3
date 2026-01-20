package ru.itmo.market.application.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import ru.itmo.market.adapter.out.messaging.kafka.client.KafkaRpcClient
import ru.itmo.market.adapter.exception.ForbiddenException
import ru.itmo.market.adapter.exception.ResourceNotFoundException
import ru.itmo.market.domain.model.dto.request.BulkModerationRequest
import ru.itmo.market.domain.model.dto.response.ModerationResultResponse
import ru.itmo.market.domain.model.dto.response.PaginatedResponse
import ru.itmo.market.domain.model.dto.response.ProductResponse
import ru.itmo.market.domain.model.entity.ModerationAction
import ru.itmo.market.domain.model.entity.ModerationAudit
import ru.itmo.market.domain.repository.ModerationActionRepository
import ru.itmo.market.domain.repository.ModerationAuditRepository
import java.time.LocalDateTime

@Service
class ModerationService(
    private val kafkaRpcClient: KafkaRpcClient,
    private val moderationActionRepository: ModerationActionRepository,
    private val moderationAuditRepository: ModerationAuditRepository,
    private val moderationEventProducer: ru.itmo.market.adapter.out.messaging.kafka.producer.ModerationEventProducer? = null
) {
    
    private val logger = LoggerFactory.getLogger(ModerationService::class.java)
    
    // ========== ОСНОВНЫЕ МЕТОДЫ ==========
    
    fun getPendingProducts(moderatorId: Long, page: Int, pageSize: Int): Mono<PaginatedResponse<ProductResponse>> {
        return verifyModerator(moderatorId)
            .flatMap {
                Mono.fromCallable {
                    kafkaRpcClient.getPendingProducts(page, pageSize)
                }
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess { logger.info("Fetched pending products for moderator $moderatorId") }
            }
    }
    
    fun getPendingProductById(moderatorId: Long, productId: Long): Mono<ProductResponse> {
        return verifyModerator(moderatorId)
            .flatMap {
                Mono.fromCallable {
                    kafkaRpcClient.getPendingProductById(productId)
                }
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess { logger.info("Fetched product $productId") }
            }
    }
    
    @Transactional
    fun approveProduct(moderatorId: Long, productId: Long): Mono<ModerationResultResponse> {
        return verifyModerator(moderatorId)
            .flatMap { userResponse ->
                // Получаем товар и одобряем его
                Mono.fromCallable {
                    kafkaRpcClient.approveProduct(productId, moderatorId)
                }
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap { product ->
                    // Сохраняем действие модерации
                    val action = ModerationAction(
                        productId = productId,
                        moderatorId = moderatorId,
                        actionType = "APPROVE",
                        reason = null
                    )
                    
                    moderationActionRepository.save(action)
                        .flatMap { savedAction ->
                            // Сохраняем аудит
                            val audit = ModerationAudit(
                                actionId = savedAction.id,
                                productId = productId,
                                moderatorId = moderatorId,
                                oldStatus = "PENDING",
                                newStatus = "APPROVED"
                            )
                            moderationAuditRepository.save(audit)
                                .map {
                                    // Публикуем событие об одобрении продукта
                                    moderationEventProducer?.publishProductApproved(
                                        productId = productId,
                                        productName = product.name,
                                        sellerId = product.sellerId,
                                        moderatorId = moderatorId
                                    )
                                    ModerationResultResponse(
                                        productId = productId,
                                        productName = product.name,
                                        action = "APPROVE",
                                        reason = null,
                                        moderatorId = moderatorId,
                                        newStatus = "APPROVED",
                                        timestamp = LocalDateTime.now()
                                    )
                                }
                        }
                }
                .doOnSuccess { logger.info("Product $productId approved by moderator $moderatorId") }
            }
    }

    @Transactional
    fun rejectProduct(moderatorId: Long, productId: Long, reason: String): Mono<ModerationResultResponse> {
        return verifyModerator(moderatorId)
            .flatMap { userResponse ->
                // Получаем товар и отклоняем его
                Mono.fromCallable {
                    kafkaRpcClient.rejectProduct(productId, moderatorId, reason)
                }
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap { product ->
                    // Сохраняем действие модерации
                    val action = ModerationAction(
                        productId = productId,
                        moderatorId = moderatorId,
                        actionType = "REJECT",
                        reason = reason
                    )
                    
                    moderationActionRepository.save(action)
                        .flatMap { savedAction ->
                            // Сохраняем аудит
                            val audit = ModerationAudit(
                                actionId = savedAction.id,
                                productId = productId,
                                moderatorId = moderatorId,
                                oldStatus = "PENDING",
                                newStatus = "REJECTED"
                            )
                            moderationAuditRepository.save(audit)
                                .map {
                                    // Публикуем событие об отклонении продукта
                                    moderationEventProducer?.publishProductRejected(
                                        productId = productId,
                                        productName = product.name,
                                        sellerId = product.sellerId,
                                        moderatorId = moderatorId,
                                        reason = reason
                                    )
                                    ModerationResultResponse(
                                        productId = productId,
                                        productName = product.name,
                                        action = "REJECT",
                                        reason = reason,
                                        moderatorId = moderatorId,
                                        newStatus = "REJECTED",
                                        timestamp = LocalDateTime.now()
                                    )
                                }
                        }
                }
                .doOnSuccess { logger.info("Product $productId rejected by moderator $moderatorId") }
            }
    }

    // ========== BULK ОПЕРАЦИИ ==========
    
    @Transactional
    fun bulkModerate(moderatorId: Long, request: BulkModerationRequest): Flux<ModerationResultResponse> {
        return verifyModerator(moderatorId)
            .flatMapMany { userResponse ->
                Flux.fromIterable(request.productIds)
                    .flatMap { productId ->
                        if (request.action.uppercase() == "APPROVE") {
                            approveProduct(moderatorId, productId)
                        } else {
                            rejectProduct(moderatorId, productId, request.reason ?: "No reason provided")
                        }
                    }
            }
            .doOnNext { logger.info("Bulk moderation action: ${it.action} for product ${it.productId}") }
    }
    
    // ========== ИСТОРИИ И ЛОГИ ==========
    
    fun getModerationHistory(moderatorId: Long): Flux<ModerationAction> {
        return moderationActionRepository.findByModeratorIdOrderByCreatedAtDesc(moderatorId)
            .doOnNext { logger.info("Fetching history for moderator $moderatorId") }
    }
    
    fun getProductModerationHistory(productId: Long): Flux<ModerationAudit> {
        return moderationAuditRepository.findByProductIdOrderByCreatedAtDesc(productId)
            .doOnNext { logger.info("Fetching moderation history for product $productId") }
    }
    
    // ========== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ==========
    
    private fun verifyModerator(moderatorId: Long): Mono<ru.itmo.market.domain.model.dto.response.UserResponse> {
        return Mono.fromCallable {
            kafkaRpcClient.getUserById(moderatorId)
        }
        .subscribeOn(Schedulers.boundedElastic())
        .flatMap { user ->
            if (user.roles.contains("MODERATOR") || user.roles.contains("ADMIN")) {
                Mono.just(user)
            } else {
                Mono.error(ForbiddenException("User is not a moderator"))
            }
        }
    }
}
