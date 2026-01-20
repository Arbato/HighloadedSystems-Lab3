package ru.itmo.market.kafka.dlq

import jakarta.persistence.*
import mu.KotlinLogging
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class DeadLetterQueueService(
    private val dlqRepository: DeadLetterQueueRepository
) {
    companion object {
        private val logger = KotlinLogging.logger { }
        private const val MAX_RETRIES = 5
    }

    @Transactional
    fun recordFailedMessage(
        eventId: String,
        eventType: String,
        topic: String,
        partition: Int,
        offset: Long,
        payload: String,
        error: Exception
    ): DeadLetterMessage {
        try {
            val dlqMessage = DeadLetterMessage(
                eventId = eventId,
                eventType = eventType,
                topic = topic,
                partition = partition,
                offset = offset,
                payload = payload,
                errorMessage = error.message,
                errorStackTrace = error.stackTraceToString(),
                retryCount = 0,
                maxRetries = MAX_RETRIES,
                status = DLQStatus.PENDING
            )

            val saved = dlqRepository.save(dlqMessage)
            logger.error { "Message saved to DLQ. EventId: $eventId, Topic: $topic" }
            return saved
        } catch (e: Exception) {
            logger.error(e) { "Failed to save message to DLQ" }
            throw e
        }
    }

    @Transactional
    fun markAsResolved(dlqMessageId: Long) {
        val message = dlqRepository.findById(dlqMessageId)
        if (message.isPresent) {
            val dlqMessage = message.get()
            dlqMessage.status = DLQStatus.RESOLVED
            dlqMessage.updatedAt = LocalDateTime.now()
            dlqMessage.resolvedAt = LocalDateTime.now()
            dlqRepository.save(dlqMessage)
        }
    }

    @Transactional
    fun incrementRetry(dlqMessageId: Long) {
        val message = dlqRepository.findById(dlqMessageId)
        if (message.isPresent) {
            val dlqMessage = message.get()
            dlqMessage.retryCount++
            dlqMessage.updatedAt = LocalDateTime.now()
            if (dlqMessage.retryCount >= dlqMessage.maxRetries) {
                dlqMessage.status = DLQStatus.ABANDONED
            }
            dlqRepository.save(dlqMessage)
        }
    }

    fun getRetryableMessages(): List<DeadLetterMessage> {
        return dlqRepository.findRetryableMessages(DLQStatus.PENDING)
    }

    fun getMessageByEventId(eventId: String): DeadLetterMessage? {
        return dlqRepository.findByEventId(eventId)
    }
}

@Service
class DeadLetterQueueListener(
    private val dlqService: DeadLetterQueueService
) {
    companion object {
        private val logger = KotlinLogging.logger { }
    }

    @KafkaListener(topics = ["\${kafka.topics.suffix:}.dlq"])
    @Transactional
    fun handleDLQMessage(
        @Payload payload: String,
        @Header(name = "eventId", required = false) eventId: String?,
        @Header(name = "eventType", required = false) eventType: String?,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
        @Header(KafkaHeaders.RECEIVED_PARTITION) partition: Int,
        @Header(KafkaHeaders.OFFSET) offset: Long,
        acknowledgment: Acknowledgment
    ) {
        try {
            dlqService.recordFailedMessage(
                eventId = eventId ?: "unknown",
                eventType = eventType ?: "unknown",
                topic = topic,
                partition = partition,
                offset = offset,
                payload = payload,
                error = Exception("Message failed after retries")
            )
            acknowledgment.acknowledge()
        } catch (e: Exception) {
            logger.error(e) { "Error processing DLQ message" }
        }
    }
}