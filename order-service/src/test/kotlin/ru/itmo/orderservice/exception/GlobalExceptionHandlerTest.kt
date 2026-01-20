package ru.itmo.orderservice.adapter.exception

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.BeforeEach
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import org.springframework.web.context.request.WebRequest
import org.springframework.validation.BindingResult
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException

@DisplayName("GlobalExceptionHandler Tests")
class GlobalExceptionHandlerTest {

    private lateinit var handler: GlobalExceptionHandler
    private lateinit var webRequest: WebRequest

    @BeforeEach
    fun setUp() {
        handler = GlobalExceptionHandler()
        webRequest = mock()
        whenever(webRequest.getDescription(false)).thenReturn("uri=/api/test")
    }

    @Test
    @DisplayName("handles ResourceNotFoundException with 404")
    fun testHandleResourceNotFoundException() {
        val exception = ResourceNotFoundException("Order not found")

        val response = handler.handleResourceNotFoundException(exception, webRequest)

        assert(response.statusCode == HttpStatus.NOT_FOUND)
        assert(response.body?.message == "Order not found")
        assert(response.body?.status == 404)
    }

    @Test
    @DisplayName("handles BadRequestException with 400")
    fun testHandleBadRequestException() {
        val exception = BadRequestException("Invalid input")

        val response = handler.handleBadRequestException(exception, webRequest)

        assert(response.statusCode == HttpStatus.BAD_REQUEST)
        assert(response.body?.message == "Invalid input")
        assert(response.body?.status == 400)
    }

    @Test
    @DisplayName("handles ForbiddenException with 403")
    fun testHandleForbiddenException() {
        val exception = ForbiddenException("Access denied")

        val response = handler.handleForbiddenException(exception, webRequest)

        assert(response.statusCode == HttpStatus.FORBIDDEN)
        assert(response.body?.message == "Access denied")
        assert(response.body?.status == 403)
    }

    @Test
    @DisplayName("handles ServiceUnavailableException with 503")
    fun testHandleServiceUnavailableException() {
        val exception = ServiceUnavailableException("Service down")

        val response = handler.handleServiceUnavailableException(exception, webRequest)

        assert(response.statusCode == HttpStatus.SERVICE_UNAVAILABLE)
        assert(response.body?.message == "Service down")
        assert(response.body?.status == 503)
    }

    @Test
    @DisplayName("handles MethodArgumentNotValidException with 400")
    fun testHandleValidationExceptions() {
        val bindingResult = mock<BindingResult>()
        val fieldError = FieldError("request", "field", "must not be blank")

        whenever(bindingResult.fieldErrors).thenReturn(listOf(fieldError))

        val exception = mock<MethodArgumentNotValidException>()
        whenever(exception.bindingResult).thenReturn(bindingResult)

        val response = handler.handleValidationExceptions(exception, webRequest)

        assert(response.statusCode == HttpStatus.BAD_REQUEST)
        assert(response.body?.message == "Validation failed")
        assert(response.body?.errors?.isNotEmpty() == true)
    }

    @Test
    @DisplayName("handles generic Exception with 500")
    fun testHandleGlobalException() {
        val exception = RuntimeException("Unexpected error")

        val response = handler.handleGlobalException(exception, webRequest)

        assert(response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR)
        assert(response.body?.message?.contains("Unexpected error") == true)
        assert(response.body?.status == 500)
    }

    @Test
    @DisplayName("includes path in error response")
    fun testErrorResponseIncludesPath() {
        val exception = BadRequestException("Test")

        val response = handler.handleBadRequestException(exception, webRequest)

        assert(response.body?.path == "/api/test")
    }

    @Test
    @DisplayName("includes timestamp in error response")
    fun testErrorResponseIncludesTimestamp() {
        val exception = BadRequestException("Test")

        val response = handler.handleBadRequestException(exception, webRequest)

        assert(response.body?.timestamp != null)
    }

    @Test
    @DisplayName("handles null message in exception")
    fun testHandlesNullMessage() {
        val exception = ResourceNotFoundException("Resource not found")

        val response = handler.handleResourceNotFoundException(exception, webRequest)

        assert(response.body?.message != null)
    }
}
