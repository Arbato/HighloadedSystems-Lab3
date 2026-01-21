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

class FileUploadException(message: String, cause: Throwable? = null) : 
    FileServiceException(message, cause)
