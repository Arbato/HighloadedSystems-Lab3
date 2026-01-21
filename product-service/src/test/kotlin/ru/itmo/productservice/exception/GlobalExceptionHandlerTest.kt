package ru.itmo.productservice.exception

import jakarta.validation.ConstraintViolation
import jakarta.validation.ConstraintViolationException
import jakarta.validation.Path
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.BeforeEach
import org.mockito.kotlin.*
import org.springframework.http.HttpStatus
import org.springframework.validation.BindingResult
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.context.request.WebRequest
import ru.itmo.productservice.adapters.exception.BadRequestException
import ru.itmo.productservice.adapters.exception.ConflictException
import ru.itmo.productservice.adapters.exception.ForbiddenException
import ru.itmo.productservice.adapters.exception.ResourceNotFoundException
import ru.itmo.productservice.presentation.adapter.exception.GlobalExceptionHandler

@DisplayName("GlobalExceptionHandler Tests")
class GlobalExceptionHandlerTest {

    private lateinit var handler: GlobalExceptionHandler
    private lateinit var webRequest: WebRequest

    @BeforeEach
    fun setUp() {
        handler = GlobalExceptionHandler()
        webRequest = mock {
            on { getDescription(false) } doReturn "uri=/api/test"
        }
    }

    @Test
    @DisplayName("handleResourceNotFoundException returns 404")
    fun testHandleResourceNotFoundException() {
        val exception = ResourceNotFoundException("Product not found")

        val response = handler.handleResourceNotFoundException(exception, webRequest)

        assert(response.statusCode == HttpStatus.NOT_FOUND)
        assert(response.body?.message == "Product not found")
        assert(response.body?.status == 404)
    }

    @Test
    @DisplayName("handleResourceNotFoundException uses default message when null")
    fun testHandleResourceNotFoundExceptionDefaultMessage() {
        val exception = ResourceNotFoundException("Test")

        val response = handler.handleResourceNotFoundException(exception, webRequest)

        assert(response.body?.message == "Test")
    }

    @Test
    @DisplayName("handleBadRequestException returns 400")
    fun testHandleBadRequestException() {
        val exception = BadRequestException("Invalid input")

        val response = handler.handleBadRequestException(exception, webRequest)

        assert(response.statusCode == HttpStatus.BAD_REQUEST)
        assert(response.body?.message == "Invalid input")
        assert(response.body?.status == 400)
    }

    @Test
    @DisplayName("handleBadRequestException uses default message when null")
    fun testHandleBadRequestExceptionDefaultMessage() {
        val exception = BadRequestException("Custom message")

        val response = handler.handleBadRequestException(exception, webRequest)

        assert(response.body?.message == "Custom message")
    }

    @Test
    @DisplayName("handleForbiddenException returns 403")
    fun testHandleForbiddenException() {
        val exception = ForbiddenException("Access denied")

        val response = handler.handleForbiddenException(exception, webRequest)

        assert(response.statusCode == HttpStatus.FORBIDDEN)
        assert(response.body?.message == "Access denied")
        assert(response.body?.status == 403)
    }

    @Test
    @DisplayName("handleForbiddenException uses default message when null")
    fun testHandleForbiddenExceptionDefaultMessage() {
        val exception = ForbiddenException("Only admin can do this")

        val response = handler.handleForbiddenException(exception, webRequest)

        assert(response.body?.message == "Only admin can do this")
    }

    @Test
    @DisplayName("handleConflictException returns 409")
    fun testHandleConflictException() {
        val exception = ConflictException("Resource already exists")

        val response = handler.handleConflictException(exception, webRequest)

        assert(response.statusCode == HttpStatus.CONFLICT)
        assert(response.body?.message == "Resource already exists")
        assert(response.body?.status == 409)
    }

    @Test
    @DisplayName("handleIllegalStateException returns 400")
    fun testHandleIllegalStateException() {
        val exception = IllegalStateException("Invalid state")

        val response = handler.handleIllegalStateException(exception, webRequest)

        assert(response.statusCode == HttpStatus.BAD_REQUEST)
        assert(response.body?.message == "Invalid state")
        assert(response.body?.status == 400)
    }

    @Test
    @DisplayName("handleGlobalException returns 500")
    fun testHandleGlobalException() {
        val exception = RuntimeException("Unexpected error")

        val response = handler.handleGlobalException(exception, webRequest)

        assert(response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR)
        assert(response.body?.message?.contains("Unexpected error") == true)
        assert(response.body?.status == 500)
    }

    @Test
    @DisplayName("handleConstraintViolationException returns 400 with errors")
    fun testHandleConstraintViolationException() {
        val path: Path = mock {
            on { toString() } doReturn "name"
        }
        val violation: ConstraintViolation<*> = mock {
            on { propertyPath } doReturn path
            on { message } doReturn "must not be blank"
        }
        val exception = ConstraintViolationException(setOf(violation))

        val response = handler.handleConstraintViolationException(exception, webRequest)

        assert(response.statusCode == HttpStatus.BAD_REQUEST)
        assert(response.body?.message == "Validation failed")
        assert(response.body?.errors?.isNotEmpty() == true)
        assert(response.body?.errors?.first()?.contains("name") == true)
    }

    @Test
    @DisplayName("handleValidationExceptions returns 400 with field errors")
    fun testHandleValidationExceptions() {
        val fieldError = FieldError("request", "name", "must not be blank")
        val bindingResult: BindingResult = mock {
            on { fieldErrors } doReturn listOf(fieldError)
        }
        val exception: MethodArgumentNotValidException = mock {
            on { this.bindingResult } doReturn bindingResult
        }

        val response = handler.handleValidationExceptions(exception, webRequest)

        assert(response.statusCode == HttpStatus.BAD_REQUEST)
        assert(response.body?.message == "Validation failed")
        assert(response.body?.errors?.isNotEmpty() == true)
        assert(response.body?.errors?.first()?.contains("name") == true)
    }

    @Test
    @DisplayName("ErrorResponse includes path")
    fun testErrorResponseIncludesPath() {
        val exception = BadRequestException("Test")

        val response = handler.handleBadRequestException(exception, webRequest)

        assert(response.body?.path == "/api/test")
    }

    @Test
    @DisplayName("ErrorResponse includes timestamp")
    fun testErrorResponseIncludesTimestamp() {
        val exception = BadRequestException("Test")

        val response = handler.handleBadRequestException(exception, webRequest)

        assert(response.body?.timestamp != null)
    }
}
