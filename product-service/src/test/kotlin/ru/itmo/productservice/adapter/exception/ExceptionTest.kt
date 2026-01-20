package ru.itmo.productservice.adapter.exception

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertThrows

@DisplayName("Exception Tests")
class ExceptionTest {

    @Test
    @DisplayName("BadRequestException should store message")
    fun testBadRequestExceptionMessage() {
        val exception = BadRequestException("Bad request occurred")
        assert(exception.message == "Bad request occurred")
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
    @DisplayName("BadRequestException should extend RuntimeException")
    fun testBadRequestExceptionExtends() {
        val exception = BadRequestException("test")
        assert(exception is RuntimeException)
    }

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
        val exception = ForbiddenException("Only moderators can perform this action")
        assert(exception.message?.contains("moderators") == true)
    }

    @Test
    @DisplayName("ForbiddenException should extend RuntimeException")
    fun testForbiddenExceptionExtends() {
        val exception = ForbiddenException("test")
        assert(exception is RuntimeException)
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
    @DisplayName("ResourceNotFoundException should extend RuntimeException")
    fun testResourceNotFoundExceptionExtends() {
        val exception = ResourceNotFoundException("test")
        assert(exception is RuntimeException)
    }

    @Test
    @DisplayName("ServiceUnavailableException should store message")
    fun testServiceUnavailableExceptionMessage() {
        val exception = ServiceUnavailableException("Service is down")
        assert(exception.message == "Service is down")
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
        val exception = ServiceUnavailableException("User service is currently unavailable")
        assert(exception.message?.contains("User service") == true)
    }

    @Test
    @DisplayName("ServiceUnavailableException should extend RuntimeException")
    fun testServiceUnavailableExceptionExtends() {
        val exception = ServiceUnavailableException("test")
        assert(exception is RuntimeException)
    }

    @Test
    @DisplayName("All exceptions should be distinct types")
    fun testExceptionsAreDistinctTypes() {
        val badRequest = BadRequestException("test")
        val forbidden = ForbiddenException("test")
        val notFound = ResourceNotFoundException("test")
        val unavailable = ServiceUnavailableException("test")

        assert(badRequest::class != forbidden::class)
        assert(badRequest::class != notFound::class)
        assert(badRequest::class != unavailable::class)
        assert(forbidden::class != notFound::class)
        assert(forbidden::class != unavailable::class)
        assert(notFound::class != unavailable::class)
    }
}
