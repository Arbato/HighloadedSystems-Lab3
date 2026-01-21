package ru.itmo.orderservice.exception

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import ru.itmo.orderservice.adapters.exception.BadRequestException
import ru.itmo.orderservice.adapters.exception.ConflictException
import ru.itmo.orderservice.adapters.exception.ForbiddenException
import ru.itmo.orderservice.adapters.exception.ResourceNotFoundException
import ru.itmo.orderservice.adapters.exception.ServiceUnavailableException

@DisplayName("Exception Tests")
class ExceptionTest {

    @Test
    @DisplayName("BadRequestException has correct message")
    fun testBadRequestException() {
        val exception = BadRequestException("Invalid input")

        assert(exception.message == "Invalid input")
        assert(exception is RuntimeException)
    }

    @Test
    @DisplayName("ResourceNotFoundException has correct message")
    fun testResourceNotFoundException() {
        val exception = ResourceNotFoundException("Resource not found")

        assert(exception.message == "Resource not found")
        assert(exception is RuntimeException)
    }

    @Test
    @DisplayName("ForbiddenException has correct message")
    fun testForbiddenException() {
        val exception = ForbiddenException("Access denied")

        assert(exception.message == "Access denied")
        assert(exception is RuntimeException)
    }

    @Test
    @DisplayName("ServiceUnavailableException has correct message")
    fun testServiceUnavailableException() {
        val exception = ServiceUnavailableException("Service unavailable")

        assert(exception.message == "Service unavailable")
        assert(exception is RuntimeException)
    }

    @Test
    @DisplayName("ConflictException has correct message")
    fun testConflictException() {
        val exception = ConflictException("Conflict occurred")

        assert(exception.message == "Conflict occurred")
        assert(exception is RuntimeException)
    }

    @Test
    @DisplayName("Exceptions are distinct types")
    fun testExceptionsAreDistinctTypes() {
        val badRequest = BadRequestException("test")
        val notFound = ResourceNotFoundException("test")
        val forbidden = ForbiddenException("test")
        val unavailable = ServiceUnavailableException("test")
        val conflict = ConflictException("test")

        assert(badRequest::class != notFound::class)
        assert(notFound::class != forbidden::class)
        assert(forbidden::class != unavailable::class)
        assert(unavailable::class != conflict::class)
    }

    @Test
    @DisplayName("Exceptions can be thrown and caught")
    fun testExceptionsCanBeThrownAndCaught() {
        var caught = false

        try {
            throw BadRequestException("Test error")
        } catch (e: BadRequestException) {
            caught = true
            assert(e.message == "Test error")
        }

        assert(caught)
    }

    @Test
    @DisplayName("Exceptions can be caught as RuntimeException")
    fun testExceptionsInheritFromRuntimeException() {
        val exceptions = listOf(
            BadRequestException("test"),
            ResourceNotFoundException("test"),
            ForbiddenException("test"),
            ServiceUnavailableException("test"),
            ConflictException("test")
        )

        exceptions.forEach { exception ->
            var caught = false
            try {
                throw exception
            } catch (e: RuntimeException) {
                caught = true
            }
            assert(caught)
        }
    }
}
