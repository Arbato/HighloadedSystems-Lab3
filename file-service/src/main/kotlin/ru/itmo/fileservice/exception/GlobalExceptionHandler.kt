package ru.itmo.fileservice.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import mu.KotlinLogging
import java.time.LocalDateTime

private val logger = KotlinLogging.logger {}

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(FileNotFoundException::class)
    fun handleFileNotFoundException(
        ex: FileNotFoundException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error(ex) { "File not found: ${ex.message}" }
        return ResponseEntity(
            ErrorResponse(
                message = ex.message,
                status = HttpStatus.NOT_FOUND.value(),
                path = request.getDescription(false).replace("uri=", "")
            ),
            HttpStatus.NOT_FOUND
        )
    }

    @ExceptionHandler(FileValidationException::class)
    fun handleFileValidationException(
        ex: FileValidationException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn { "Validation error: ${ex.message}" }
        return ResponseEntity(
            ErrorResponse(
                message = ex.message,
                status = HttpStatus.BAD_REQUEST.value(),
                path = request.getDescription(false).replace("uri=", "")
            ),
            HttpStatus.BAD_REQUEST
        )
    }

    @ExceptionHandler(QuotaExceededException::class)
    fun handleQuotaExceededException(
        ex: QuotaExceededException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn { "Quota exceeded: ${ex.message}" }
        return ResponseEntity(
            ErrorResponse(
                message = ex.message,
                status = HttpStatus.PAYLOAD_TOO_LARGE.value(),
                path = request.getDescription(false).replace("uri=", "")
            ),
            HttpStatus.PAYLOAD_TOO_LARGE
        )
    }

    @ExceptionHandler(StorageException::class)
    fun handleStorageException(
        ex: StorageException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error(ex) { "Storage error: ${ex.message}" }
        return ResponseEntity(
            ErrorResponse(
                message = ex.message,
                status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
                path = request.getDescription(false).replace("uri=", "")
            ),
            HttpStatus.INTERNAL_SERVER_ERROR
        )
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(
        ex: MethodArgumentNotValidException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val errors = ex.bindingResult.fieldErrors.map { 
            "${it.field}: ${it.defaultMessage}" 
        }
        logger.warn { "Validation failed: $errors" }
        return ResponseEntity(
            ErrorResponse(
                message = "Validation failed",
                status = HttpStatus.BAD_REQUEST.value(),
                path = request.getDescription(false).replace("uri=", ""),
                errors = errors
            ),
            HttpStatus.BAD_REQUEST
        )
    }

    @ExceptionHandler(Exception::class)
    fun handleGlobalException(
        ex: Exception,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error(ex) { "Unexpected error" }
        return ResponseEntity(
            ErrorResponse(
                message = ex.message ?: "Internal server error",
                status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
                path = request.getDescription(false).replace("uri=", "")
            ),
            HttpStatus.INTERNAL_SERVER_ERROR
        )
    }
}