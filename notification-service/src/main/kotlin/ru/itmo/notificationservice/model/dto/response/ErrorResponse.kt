package ru.itmo.notificationservice.model.dto.response

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import ru.itmo.notificationservice.exception.DomainException
import ru.itmo.notificationservice.model.dto.response.ErrorResponse
import java.time.LocalDateTime

data class ErrorResponse(
    val message: String,
    val status: Int,
    val timestamp: LocalDateTime = LocalDateTime.now()
)