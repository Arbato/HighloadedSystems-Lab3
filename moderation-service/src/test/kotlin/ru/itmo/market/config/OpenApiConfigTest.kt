package ru.itmo.market.config

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.BeforeEach
import ru.itmo.market.infrastructure.config.OpenApiConfig

@DisplayName("OpenApiConfig Tests")
class OpenApiConfigTest {

    private lateinit var openApiConfig: OpenApiConfig

    @BeforeEach
    fun setUp() {
        openApiConfig = OpenApiConfig()
    }

    @Test
    @DisplayName("Should create OpenAPI bean with correct title")
    fun testOpenApiTitle() {
        val openAPI = openApiConfig.openAPI()
        assert(openAPI.info.title == "ITMO Market - Moderation Service API")
    }

    @Test
    @DisplayName("Should create OpenAPI bean with correct version")
    fun testOpenApiVersion() {
        val openAPI = openApiConfig.openAPI()
        assert(openAPI.info.version == "1.0.0")
    }

    @Test
    @DisplayName("Should create OpenAPI bean with description")
    fun testOpenApiDescription() {
        val openAPI = openApiConfig.openAPI()
        assert(openAPI.info.description != null)
        assert(openAPI.info.description.contains("модерации") || openAPI.info.description.contains("Reactor"))
    }
}
