package ru.itmo.notificationservice.exception

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import ru.itmo.notificationservice.exception.DomainException
import ru.itmo.notificationservice.model.dto.response.ErrorResponse
import java.time.LocalDateTime

@RestControllerAdvice
class GlobalExceptionHandler {
    private val logger = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(DomainException::class)
    fun handleDomainException(ex: DomainException): ResponseEntity<ErrorResponse> {
        logger.warn("Domain exception: ${ex.message}")
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ErrorResponse(
                message = ex.message ?: "Domain error",
                status = HttpStatus.BAD_REQUEST.value(),
                timestamp = LocalDateTime.now()
            )
        )
    }

    @ExceptionHandler(Exception::class)
    fun handleException(ex: Exception): ResponseEntity<ErrorResponse> {
        logger.error("Unexpected exception", ex)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            ErrorResponse(
                message = "Internal server error",
                status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
                timestamp = LocalDateTime.now()
            )
        )
    }
}

