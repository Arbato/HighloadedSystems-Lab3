package ru.itmo.market.adapter.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.bind.MethodArgumentNotValidException
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import ru.itmo.market.domain.model.dto.response.ErrorResponse
import jakarta.validation.ConstraintViolationException

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(ForbiddenException::class)
    fun handleForbiddenException(ex: ForbiddenException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ErrorResponse(
                message = ex.message ?: "Forbidden",
                status = HttpStatus.FORBIDDEN.value(),
                timestamp = LocalDateTime.now(),
                path = ""
            ))
    }

    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleResourceNotFoundException(ex: ResourceNotFoundException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse(
                message = ex.message ?: "Resource not found",
                status = HttpStatus.NOT_FOUND.value(),
                timestamp = LocalDateTime.now(),
                path = ""
            ))
    }

    @ExceptionHandler(BadRequestException::class)
    fun handleBadRequestException(ex: BadRequestException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(
                message = ex.message ?: "Bad request",
                status = HttpStatus.BAD_REQUEST.value(),
                timestamp = LocalDateTime.now(),
                path = ""
            ))
    }

    @ExceptionHandler(ServiceUnavailableException::class)
    fun handleServiceUnavailableException(ex: ServiceUnavailableException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(ErrorResponse(
                message = ex.message ?: "Service unavailable",
                status = HttpStatus.SERVICE_UNAVAILABLE.value(),
                timestamp = LocalDateTime.now(),
                path = ""
            ))
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(ex: IllegalArgumentException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(
                message = ex.message ?: "Bad request",
                status = HttpStatus.BAD_REQUEST.value(),
                timestamp = LocalDateTime.now(),
                path = ""
            ))
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolationException(
        ex: ConstraintViolationException,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<ErrorResponse>> {
        val errors = ex.constraintViolations
            .map { "${it.propertyPath}: ${it.message}" }
        
        return Mono.just(
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse(
                    message = "Validation failed",
                    status = HttpStatus.BAD_REQUEST.value(),
                    timestamp = LocalDateTime.now(),
                    path = exchange.request.path.value(),
                    errors = errors
                ))
        )
    }
    
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationExceptions(
        ex: MethodArgumentNotValidException,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<ErrorResponse>> {
        val errors = ex.bindingResult.fieldErrors
            .map { "${it.field}: ${it.defaultMessage}" }
        
        return Mono.just(
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse(
                    message = "Validation failed",
                    status = HttpStatus.BAD_REQUEST.value(),
                    timestamp = LocalDateTime.now(),
                    path = exchange.request.path.value(),
                    errors = errors
                ))
        )
    } 
    
    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse(
                message = ex.message ?: "Internal server error",
                status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
                timestamp = LocalDateTime.now(),
                path = ""
            ))
    }
}
