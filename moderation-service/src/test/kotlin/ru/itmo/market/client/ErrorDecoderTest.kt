package ru.itmo.market.client

import feign.Request
import feign.Response
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.BeforeEach
import ru.itmo.market.adapters.exception.BadRequestException
import ru.itmo.market.adapters.exception.ResourceNotFoundException
import ru.itmo.market.adapters.exception.ServiceUnavailableException
import ru.itmo.market.infrastructure.client.ProductServiceFeignConfig
import ru.itmo.market.infrastructure.client.UserServiceFeignConfig
import java.nio.charset.StandardCharsets

@DisplayName("Error Decoder Tests")
class ErrorDecoderTest {

    private lateinit var productConfig: ProductServiceFeignConfig
    private lateinit var userConfig: UserServiceFeignConfig

    @BeforeEach
    fun setUp() {
        productConfig = ProductServiceFeignConfig()
        userConfig = UserServiceFeignConfig()
    }

    private fun createResponse(status: Int): Response {
        val request = Request.create(
            Request.HttpMethod.GET,
            "/test",
            emptyMap(),
            null,
            StandardCharsets.UTF_8,
            null
        )
        return Response.builder()
            .status(status)
            .reason("Test")
            .request(request)
            .headers(emptyMap())
            .build()
    }

    // ==================== Product Service Error Decoder Tests ====================

    @Test
    @DisplayName("Product decoder should return BadRequestException for 400")
    fun testProductDecoder400() {
        val decoder = productConfig.productServiceErrorDecoder()
        val response = createResponse(400)
        val exception = decoder.decode("test", response)
        assert(exception is BadRequestException)
    }

    @Test
    @DisplayName("Product decoder should return ResourceNotFoundException for 404")
    fun testProductDecoder404() {
        val decoder = productConfig.productServiceErrorDecoder()
        val response = createResponse(404)
        val exception = decoder.decode("test", response)
        assert(exception is ResourceNotFoundException)
    }

    @Test
    @DisplayName("Product decoder should return ServiceUnavailableException for 500")
    fun testProductDecoder500() {
        val decoder = productConfig.productServiceErrorDecoder()
        val response = createResponse(500)
        val exception = decoder.decode("test", response)
        assert(exception is ServiceUnavailableException)
    }

    @Test
    @DisplayName("Product decoder should return ServiceUnavailableException for 502")
    fun testProductDecoder502() {
        val decoder = productConfig.productServiceErrorDecoder()
        val response = createResponse(502)
        val exception = decoder.decode("test", response)
        assert(exception is ServiceUnavailableException)
    }

    @Test
    @DisplayName("Product decoder should return ServiceUnavailableException for 503")
    fun testProductDecoder503() {
        val decoder = productConfig.productServiceErrorDecoder()
        val response = createResponse(503)
        val exception = decoder.decode("test", response)
        assert(exception is ServiceUnavailableException)
    }

    @Test
    @DisplayName("Product decoder should return RuntimeException for unknown status")
    fun testProductDecoderUnknown() {
        val decoder = productConfig.productServiceErrorDecoder()
        val response = createResponse(418) // I'm a teapot
        val exception = decoder.decode("test", response)
        assert(exception is RuntimeException)
        assert(exception.message?.contains("418") == true)
    }

    // ==================== User Service Error Decoder Tests ====================

    @Test
    @DisplayName("User decoder should return BadRequestException for 400")
    fun testUserDecoder400() {
        val decoder = userConfig.userServiceErrorDecoder()
        val response = createResponse(400)
        val exception = decoder.decode("test", response)
        assert(exception is BadRequestException)
    }

    @Test
    @DisplayName("User decoder should return ResourceNotFoundException for 404")
    fun testUserDecoder404() {
        val decoder = userConfig.userServiceErrorDecoder()
        val response = createResponse(404)
        val exception = decoder.decode("test", response)
        assert(exception is ResourceNotFoundException)
    }

    @Test
    @DisplayName("User decoder should return ServiceUnavailableException for 500")
    fun testUserDecoder500() {
        val decoder = userConfig.userServiceErrorDecoder()
        val response = createResponse(500)
        val exception = decoder.decode("test", response)
        assert(exception is ServiceUnavailableException)
    }

    @Test
    @DisplayName("User decoder should return ServiceUnavailableException for 502")
    fun testUserDecoder502() {
        val decoder = userConfig.userServiceErrorDecoder()
        val response = createResponse(502)
        val exception = decoder.decode("test", response)
        assert(exception is ServiceUnavailableException)
    }

    @Test
    @DisplayName("User decoder should return ServiceUnavailableException for 503")
    fun testUserDecoder503() {
        val decoder = userConfig.userServiceErrorDecoder()
        val response = createResponse(503)
        val exception = decoder.decode("test", response)
        assert(exception is ServiceUnavailableException)
    }

    @Test
    @DisplayName("User decoder should return RuntimeException for unknown status")
    fun testUserDecoderUnknown() {
        val decoder = userConfig.userServiceErrorDecoder()
        val response = createResponse(429) // Too many requests
        val exception = decoder.decode("test", response)
        assert(exception is RuntimeException)
        assert(exception.message?.contains("429") == true)
    }
}
