package ru.itmo.market.config

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.BeforeEach
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import ru.itmo.market.infrastructure.config.WebConfig

@DisplayName("WebConfig Tests")
class WebConfigTest {

    private lateinit var webConfig: WebConfig

    @BeforeEach
    fun setUp() {
        webConfig = WebConfig()
    }

    @Test
    @DisplayName("Should create CorsConfigurationSource bean")
    fun testCorsConfigurationSource() {
        val source = webConfig.corsConfigurationSource()
        assert(source != null)
    }

    @Test
    @DisplayName("Should create CorsWebFilter bean")
    fun testCorsWebFilter() {
        val source = webConfig.corsConfigurationSource()
        val filter = webConfig.corsWebFilter(source)
        assert(filter != null)
    }

    @Test
    @DisplayName("CorsConfiguration should allow specific origins")
    fun testCorsAllowedOrigins() {
        val source = webConfig.corsConfigurationSource()
        val request = MockServerHttpRequest.get("/").build()
        val exchange = MockServerWebExchange.from(request)
        val config = source.getCorsConfiguration(exchange)

        assert(config != null)
        assert(config?.allowedOrigins?.contains("http://localhost:3000") == true)
        assert(config?.allowedOrigins?.contains("http://localhost:5173") == true)
    }

    @Test
    @DisplayName("CorsConfiguration should allow common HTTP methods")
    fun testCorsAllowedMethods() {
        val source = webConfig.corsConfigurationSource()
        val request = MockServerHttpRequest.get("/api/test").build()
        val exchange = MockServerWebExchange.from(request)
        val config = source.getCorsConfiguration(exchange)

        assert(config != null)
        assert(config?.allowedMethods?.contains("GET") == true)
        assert(config?.allowedMethods?.contains("POST") == true)
        assert(config?.allowedMethods?.contains("PUT") == true)
        assert(config?.allowedMethods?.contains("DELETE") == true)
    }

    @Test
    @DisplayName("CorsConfiguration should allow credentials")
    fun testCorsAllowCredentials() {
        val source = webConfig.corsConfigurationSource()
        val request = MockServerHttpRequest.get("/").build()
        val exchange = MockServerWebExchange.from(request)
        val config = source.getCorsConfiguration(exchange)

        assert(config?.allowCredentials == true)
    }

    @Test
    @DisplayName("CorsConfiguration should have max age set")
    fun testCorsMaxAge() {
        val source = webConfig.corsConfigurationSource()
        val request = MockServerHttpRequest.get("/").build()
        val exchange = MockServerWebExchange.from(request)
        val config = source.getCorsConfiguration(exchange)

        assert(config?.maxAge == 3600L)
    }

    @Test
    @DisplayName("CorsConfiguration should allow all headers")
    fun testCorsAllowedHeaders() {
        val source = webConfig.corsConfigurationSource()
        val request = MockServerHttpRequest.get("/").build()
        val exchange = MockServerWebExchange.from(request)
        val config = source.getCorsConfiguration(exchange)

        assert(config?.allowedHeaders?.contains("*") == true)
    }

    @Test
    @DisplayName("CorsConfiguration should apply to all paths")
    fun testCorsAppliesToAllPaths() {
        val source = webConfig.corsConfigurationSource()

        val paths = listOf("/", "/api/moderation", "/api/moderation/products", "/any/path")
        for (path in paths) {
            val request = MockServerHttpRequest.get(path).build()
            val exchange = MockServerWebExchange.from(request)
            val config = source.getCorsConfiguration(exchange)
            assert(config != null) { "CORS config should be available for path: $path" }
        }
    }
}
