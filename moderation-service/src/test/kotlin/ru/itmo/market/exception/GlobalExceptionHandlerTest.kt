package ru.itmo.market.adapter.exception

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.BeforeEach
import org.springframework.http.HttpStatus

@DisplayName("GlobalExceptionHandler Tests")
class GlobalExceptionHandlerTest {

    private lateinit var handler: GlobalExceptionHandler

    @BeforeEach
    fun setUp() {
        handler = GlobalExceptionHandler()
    }

    @Test
    @DisplayName("Should handle ForbiddenException with 403 status")
    fun testHandleForbiddenException() {
        val exception = ForbiddenException("Access denied")
        val response = handler.handleForbiddenException(exception)

        assert(response.statusCode == HttpStatus.FORBIDDEN)
        assert(response.body?.status == 403)
        assert(response.body?.message == "Access denied")
    }

    @Test
    @DisplayName("Should handle ForbiddenException with detailed message")
    fun testHandleForbiddenExceptionDetailedMessage() {
        val exception = ForbiddenException("User ID 123 is not authorized")
        val response = handler.handleForbiddenException(exception)

        assert(response.statusCode == HttpStatus.FORBIDDEN)
        assert(response.body?.message?.contains("123") == true)
    }

    @Test
    @DisplayName("Should handle ResourceNotFoundException with 404 status")
    fun testHandleResourceNotFoundException() {
        val exception = ResourceNotFoundException("Product not found")
        val response = handler.handleResourceNotFoundException(exception)

        assert(response.statusCode == HttpStatus.NOT_FOUND)
        assert(response.body?.status == 404)
        assert(response.body?.message == "Product not found")
    }

    @Test
    @DisplayName("Should handle ResourceNotFoundException with ID in message")
    fun testHandleResourceNotFoundExceptionWithId() {
        val exception = ResourceNotFoundException("Product with ID 999 not found")
        val response = handler.handleResourceNotFoundException(exception)

        assert(response.statusCode == HttpStatus.NOT_FOUND)
        assert(response.body?.message?.contains("999") == true)
    }

    @Test
    @DisplayName("Should handle BadRequestException with 400 status")
    fun testHandleBadRequestException() {
        val exception = BadRequestException("Invalid input")
        val response = handler.handleBadRequestException(exception)

        assert(response.statusCode == HttpStatus.BAD_REQUEST)
        assert(response.body?.status == 400)
        assert(response.body?.message == "Invalid input")
    }

    @Test
    @DisplayName("Should handle BadRequestException with validation details")
    fun testHandleBadRequestExceptionWithDetails() {
        val exception = BadRequestException("Field 'name' cannot be empty")
        val response = handler.handleBadRequestException(exception)

        assert(response.statusCode == HttpStatus.BAD_REQUEST)
        assert(response.body?.message?.contains("name") == true)
    }

    @Test
    @DisplayName("Should handle ServiceUnavailableException with 503 status")
    fun testHandleServiceUnavailableException() {
        val exception = ServiceUnavailableException("Service down")
        val response = handler.handleServiceUnavailableException(exception)

        assert(response.statusCode == HttpStatus.SERVICE_UNAVAILABLE)
        assert(response.body?.status == 503)
        assert(response.body?.message == "Service down")
    }

    @Test
    @DisplayName("Should handle ServiceUnavailableException with service name")
    fun testHandleServiceUnavailableExceptionWithServiceName() {
        val exception = ServiceUnavailableException("Product Service is currently unavailable")
        val response = handler.handleServiceUnavailableException(exception)

        assert(response.statusCode == HttpStatus.SERVICE_UNAVAILABLE)
        assert(response.body?.message?.contains("Product Service") == true)
    }

    @Test
    @DisplayName("Should handle IllegalArgumentException with 400 status")
    fun testHandleIllegalArgumentException() {
        val exception = IllegalArgumentException("Invalid argument")
        val response = handler.handleIllegalArgumentException(exception)

        assert(response.statusCode == HttpStatus.BAD_REQUEST)
        assert(response.body?.status == 400)
        assert(response.body?.message == "Invalid argument")
    }

    @Test
    @DisplayName("Should handle IllegalArgumentException with null message")
    fun testHandleIllegalArgumentExceptionNullMessage() {
        val exception = IllegalArgumentException()
        val response = handler.handleIllegalArgumentException(exception)

        assert(response.statusCode == HttpStatus.BAD_REQUEST)
        assert(response.body?.message == "Bad request")
    }

    @Test
    @DisplayName("Should handle generic Exception with 500 status")
    fun testHandleGenericException() {
        val exception = Exception("Something went wrong")
        val response = handler.handleGenericException(exception)

        assert(response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR)
        assert(response.body?.status == 500)
        assert(response.body?.message == "Something went wrong")
    }

    @Test
    @DisplayName("Should handle generic Exception with null message")
    fun testHandleGenericExceptionNullMessage() {
        val exception = Exception()
        val response = handler.handleGenericException(exception)

        assert(response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR)
        assert(response.body?.message == "Internal server error")
    }

    @Test
    @DisplayName("Should include timestamp in response")
    fun testResponseContainsTimestamp() {
        val exception = ForbiddenException("Test")
        val response = handler.handleForbiddenException(exception)

        assert(response.body?.timestamp != null)
    }

    @Test
    @DisplayName("Should include path in response")
    fun testResponseContainsPath() {
        val exception = ForbiddenException("Test")
        val response = handler.handleForbiddenException(exception)

        assert(response.body?.path != null)
    }

    @Test
    @DisplayName("All handlers should return non-null body")
    fun testAllHandlersReturnNonNullBody() {
        assert(handler.handleForbiddenException(ForbiddenException("test")).body != null)
        assert(handler.handleResourceNotFoundException(ResourceNotFoundException("test")).body != null)
        assert(handler.handleBadRequestException(BadRequestException("test")).body != null)
        assert(handler.handleServiceUnavailableException(ServiceUnavailableException("test")).body != null)
        assert(handler.handleIllegalArgumentException(IllegalArgumentException("test")).body != null)
        assert(handler.handleGenericException(Exception("test")).body != null)
    }
}
