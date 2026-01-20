package ru.itmo.fileservice.adapter.exception

import io.minio.errors.ErrorResponseException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import ru.itmo.fileservice.domain.model.dto.response.ErrorResponse

@RestControllerAdvice
class GlobalExceptionHandler {
    private val logger = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(ErrorResponseException::class)
    fun handleMinioError(
        ex: ErrorResponseException,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<ErrorResponse>> {
        logger.error("MinIO error: ${ex.message}", ex)

        val status = when (ex.errorResponse().code()) {
            "NoSuchKey" -> HttpStatus.NOT_FOUND
            "NoSuchBucket" -> HttpStatus.NOT_FOUND
            "AccessDenied" -> HttpStatus.FORBIDDEN
            else -> HttpStatus.INTERNAL_SERVER_ERROR
        }

        return Mono.just(
            ResponseEntity.status(status).body(
                ErrorResponse(
                    status = status.value(),
                    error = status.reasonPhrase,
                    message = ex.message ?: "Storage error",
                    path = exchange.request.path.value()
                )
            )
        )
    }

    @ExceptionHandler(FileNotFoundException::class)
    fun handleFileNotFound(
        ex: FileNotFoundException,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<ErrorResponse>> {
        logger.warn("File not found: ${ex.message}")
        return Mono.just(
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ErrorResponse(
                    status = HttpStatus.NOT_FOUND.value(),
                    error = "Not Found",
                    message = ex.message ?: "File not found",
                    path = exchange.request.path.value()
                )
            )
        )
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(
        ex: Exception,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<ErrorResponse>> {
        logger.error("Unexpected error: ${ex.message}", ex)
        return Mono.just(
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ErrorResponse(
                    status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    error = "Internal Server Error",
                    message = ex.message ?: "An unexpected error occurred",
                    path = exchange.request.path.value()
                )
            )
        )
    }
}

class FileNotFoundException(message: String) : RuntimeException(message)
