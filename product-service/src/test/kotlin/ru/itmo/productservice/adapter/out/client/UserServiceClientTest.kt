package ru.itmo.productservice.adapter.out.client

import feign.Request
import feign.Response
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.BeforeEach
import ru.itmo.productservice.adapter.exception.ResourceNotFoundException
import ru.itmo.productservice.adapter.exception.ServiceUnavailableException
import java.nio.charset.StandardCharsets

@DisplayName("UserServiceClient Tests")
class UserServiceClientTest {

    private lateinit var fallback: UserServiceClientFallback
    private lateinit var config: FeignClientConfig

    @BeforeEach
    fun setUp() {
        fallback = UserServiceClientFallback()
        config = FeignClientConfig()
    }

    // ==================== Fallback Tests ====================

    @Test
    @DisplayName("Fallback throws ServiceUnavailableException")
    fun testFallbackThrowsServiceUnavailableException() {
        assertThrows<ServiceUnavailableException> {
            fallback.getUserById(1L)
        }
    }

    @Test
    @DisplayName("Fallback exception has proper message")
    fun testFallbackExceptionMessage() {
        val exception = assertThrows<ServiceUnavailableException> {
            fallback.getUserById(1L)
        }
        assert(exception.message?.contains("User service") == true)
        assert(exception.message?.contains("unavailable") == true)
    }

    @Test
    @DisplayName("Fallback works with any user ID")
    fun testFallbackWithDifferentIds() {
        assertThrows<ServiceUnavailableException> {
            fallback.getUserById(100L)
        }

        assertThrows<ServiceUnavailableException> {
            fallback.getUserById(999L)
        }
    }

    // ==================== Error Decoder Tests ====================

    @Test
    @DisplayName("Error decoder returns ResourceNotFoundException for 404")
    fun testErrorDecoder404() {
        val decoder = config.errorDecoder()
        val request = createMockRequest()
        val response = createMockResponse(404, request)

        val exception = decoder.decode("getUserById", response)

        assert(exception is ResourceNotFoundException)
    }

    @Test
    @DisplayName("Error decoder returns ServiceUnavailableException for 500")
    fun testErrorDecoder500() {
        val decoder = config.errorDecoder()
        val request = createMockRequest()
        val response = createMockResponse(500, request)

        val exception = decoder.decode("getUserById", response)

        assert(exception is ServiceUnavailableException)
    }

    @Test
    @DisplayName("Error decoder returns ServiceUnavailableException for 502")
    fun testErrorDecoder502() {
        val decoder = config.errorDecoder()
        val request = createMockRequest()
        val response = createMockResponse(502, request)

        val exception = decoder.decode("getUserById", response)

        assert(exception is ServiceUnavailableException)
    }

    @Test
    @DisplayName("Error decoder returns ServiceUnavailableException for 503")
    fun testErrorDecoder503() {
        val decoder = config.errorDecoder()
        val request = createMockRequest()
        val response = createMockResponse(503, request)

        val exception = decoder.decode("getUserById", response)

        assert(exception is ServiceUnavailableException)
    }

    @Test
    @DisplayName("Error decoder returns RuntimeException for other status codes")
    fun testErrorDecoderOtherStatus() {
        val decoder = config.errorDecoder()
        val request = createMockRequest()
        val response = createMockResponse(418, request)

        val exception = decoder.decode("getUserById", response)

        assert(exception is RuntimeException)
        assert(exception.message?.contains("418") == true)
    }

    @Test
    @DisplayName("Error decoder returns RuntimeException for 401")
    fun testErrorDecoder401() {
        val decoder = config.errorDecoder()
        val request = createMockRequest()
        val response = createMockResponse(401, request)

        val exception = decoder.decode("getUserById", response)

        assert(exception is RuntimeException)
        assert(exception.message?.contains("401") == true)
    }

    @Test
    @DisplayName("Error decoder returns RuntimeException for 403")
    fun testErrorDecoder403() {
        val decoder = config.errorDecoder()
        val request = createMockRequest()
        val response = createMockResponse(403, request)

        val exception = decoder.decode("getUserById", response)

        assert(exception is RuntimeException)
        assert(exception.message?.contains("403") == true)
    }

    private fun createMockRequest(): Request {
        return Request.create(
            Request.HttpMethod.GET,
            "/api/users/1",
            emptyMap(),
            null,
            StandardCharsets.UTF_8,
            null
        )
    }

    private fun createMockResponse(status: Int, request: Request): Response {
        return Response.builder()
            .status(status)
            .reason("Test reason")
            .headers(emptyMap())
            .request(request)
            .build()
    }
}
