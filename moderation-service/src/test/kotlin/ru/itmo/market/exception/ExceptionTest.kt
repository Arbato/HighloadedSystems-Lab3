package ru.itmo.market.exception

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertThrows
import ru.itmo.market.adapters.exception.BadRequestException
import ru.itmo.market.adapters.exception.ForbiddenException
import ru.itmo.market.adapters.exception.ModerationException
import ru.itmo.market.adapters.exception.ResourceNotFoundException
import ru.itmo.market.adapters.exception.ServiceUnavailableException

@DisplayName("Exception Tests")
class ExceptionTest {

    @Test
    @DisplayName("ForbiddenException should store message")
    fun testForbiddenExceptionMessage() {
        val exception = ForbiddenException("Access denied")
        assert(exception.message == "Access denied")
    }

    @Test
    @DisplayName("ForbiddenException should be throwable")
    fun testForbiddenExceptionThrowable() {
        assertThrows<ForbiddenException> {
            throw ForbiddenException("Test")
        }
    }

    @Test
    @DisplayName("ForbiddenException should have descriptive message")
    fun testForbiddenExceptionDescriptiveMessage() {
        val exception = ForbiddenException("User is not authorized to perform this action")
        assert(exception.message?.contains("not authorized") == true)
    }

    @Test
    @DisplayName("ResourceNotFoundException should store message")
    fun testResourceNotFoundExceptionMessage() {
        val exception = ResourceNotFoundException("Product not found")
        assert(exception.message == "Product not found")
    }

    @Test
    @DisplayName("ResourceNotFoundException should be throwable")
    fun testResourceNotFoundExceptionThrowable() {
        assertThrows<ResourceNotFoundException> {
            throw ResourceNotFoundException("Test")
        }
    }

    @Test
    @DisplayName("ResourceNotFoundException should have descriptive message")
    fun testResourceNotFoundExceptionDescriptiveMessage() {
        val exception = ResourceNotFoundException("Product with ID 100 not found")
        assert(exception.message?.contains("100") == true)
    }

    @Test
    @DisplayName("BadRequestException should store message")
    fun testBadRequestExceptionMessage() {
        val exception = BadRequestException("Invalid input")
        assert(exception.message == "Invalid input")
    }

    @Test
    @DisplayName("BadRequestException should be throwable")
    fun testBadRequestExceptionThrowable() {
        assertThrows<BadRequestException> {
            throw BadRequestException("Test")
        }
    }

    @Test
    @DisplayName("BadRequestException should have descriptive message")
    fun testBadRequestExceptionDescriptiveMessage() {
        val exception = BadRequestException("Field 'name' is required")
        assert(exception.message?.contains("name") == true)
    }

    @Test
    @DisplayName("ServiceUnavailableException should store message")
    fun testServiceUnavailableExceptionMessage() {
        val exception = ServiceUnavailableException("Service down")
        assert(exception.message == "Service down")
    }

    @Test
    @DisplayName("ServiceUnavailableException should be throwable")
    fun testServiceUnavailableExceptionThrowable() {
        assertThrows<ServiceUnavailableException> {
            throw ServiceUnavailableException("Test")
        }
    }

    @Test
    @DisplayName("ServiceUnavailableException should have descriptive message")
    fun testServiceUnavailableExceptionDescriptiveMessage() {
        val exception = ServiceUnavailableException("Product service is currently unavailable")
        assert(exception.message?.contains("Product service") == true)
    }

    @Test
    @DisplayName("All exceptions should extend RuntimeException")
    fun testExceptionsExtendRuntimeException() {
        val forbidden = ForbiddenException("test")
        val notFound = ResourceNotFoundException("test")
        val badRequest = BadRequestException("test")
        val unavailable = ServiceUnavailableException("test")

        assert(forbidden is RuntimeException)
        assert(notFound is RuntimeException)
        assert(badRequest is RuntimeException)
        assert(unavailable is RuntimeException)
    }

    @Test
    @DisplayName("ForbiddenException and ResourceNotFoundException should extend ModerationException")
    fun testModerationExceptions() {
        val forbidden = ForbiddenException("test")
        val notFound = ResourceNotFoundException("test")

        assert(forbidden is ModerationException)
        assert(notFound is ModerationException)
    }

    @Test
    @DisplayName("BadRequestException should extend RuntimeException directly")
    fun testBadRequestExceptionHierarchy() {
        val badRequest = BadRequestException("test")
        assert(badRequest is RuntimeException)
    }

    @Test
    @DisplayName("ServiceUnavailableException should extend RuntimeException directly")
    fun testServiceUnavailableExceptionHierarchy() {
        val unavailable = ServiceUnavailableException("test")
        assert(unavailable is RuntimeException)
    }
}
