package ru.itmo.userservice.service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.containers.PostgreSQLContainer
import reactor.test.StepVerifier
import ru.itmo.userservice.exception.BadRequestException
import ru.itmo.userservice.exception.ResourceNotFoundException
import ru.itmo.userservice.model.dto.request.LoginRequest
import ru.itmo.userservice.model.dto.request.RegisterRequest
import ru.itmo.userservice.repository.UserRepository
import ru.itmo.userservice.repository.UserRoleRepository
import java.time.Duration

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@DisplayName("Auth Service Tests")
class AuthServiceTest {

    companion object {
        @Container
        val postgres = PostgreSQLContainer<Nothing>("postgres:15").apply {
            withDatabaseName("itmo_market_test")
            withUsername("itmo_user")
            withPassword("itmo_password")
        }

        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.r2dbc.url") {
                "r2dbc:postgresql://${postgres.host}:${postgres.getMappedPort(5432)}/${postgres.databaseName}"
            }
            registry.add("spring.r2dbc.username") { postgres.username }
            registry.add("spring.r2dbc.password") { postgres.password }
            registry.add("spring.flyway.url") { postgres.jdbcUrl }
            registry.add("spring.flyway.user") { postgres.username }
            registry.add("spring.flyway.password") { postgres.password }
        }
    }

    @Autowired
    private lateinit var authService: AuthService

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var userRoleRepository: UserRoleRepository

    @BeforeEach
    fun setUp() {
        userRoleRepository.deleteAll().block()
        userRepository.deleteAll().block()
    }

    private fun createTestUser(
        username: String = "testuser",
        email: String = "test@example.com",
        password: String = "Password123"
    ) {
        val request = RegisterRequest(
            username = username,
            email = email,
            password = password,
            firstName = "Test",
            lastName = "User"
        )
        userService.register(request).block()
    }

    // ==================== Login Tests ====================

    @Test
    @DisplayName("Should login successfully with correct credentials")
    fun testLoginSuccess() {
        createTestUser()

        val loginRequest = LoginRequest(
            username = "testuser",
            password = "Password123"
        )

        StepVerifier.create(authService.login(loginRequest))
            .assertNext { response ->
                assert(response.username == "testuser")
                assert(response.token.isNotBlank())
                assert(response.roles.contains("USER"))
            }
            .expectComplete()
            .verify(Duration.ofSeconds(10))
    }

    @Test
    @DisplayName("Should fail login with non-existent username")
    fun testLoginUserNotFound() {
        val loginRequest = LoginRequest(
            username = "nonexistent",
            password = "Password123"
        )

        StepVerifier.create(authService.login(loginRequest))
            .expectError(ResourceNotFoundException::class.java)
            .verify(Duration.ofSeconds(10))
    }

    @Test
    @DisplayName("Should fail login with incorrect password")
    fun testLoginInvalidPassword() {
        createTestUser()

        val loginRequest = LoginRequest(
            username = "testuser",
            password = "WrongPassword123"
        )

        StepVerifier.create(authService.login(loginRequest))
            .expectError(BadRequestException::class.java)
            .verify(Duration.ofSeconds(10))
    }

    @Test
    @DisplayName("Should return valid JWT token on login")
    fun testLoginReturnsValidToken() {
        createTestUser()

        val loginRequest = LoginRequest(
            username = "testuser",
            password = "Password123"
        )

        StepVerifier.create(authService.login(loginRequest))
            .assertNext { response ->
                assert(response.token.split(".").size == 3) // JWT has 3 parts
                assert(response.userId > 0)
            }
            .expectComplete()
            .verify(Duration.ofSeconds(10))
    }

    @Test
    @DisplayName("Should return correct roles in login response")
    fun testLoginReturnsCorrectRoles() {
        createTestUser()

        val loginRequest = LoginRequest(
            username = "testuser",
            password = "Password123"
        )

        StepVerifier.create(authService.login(loginRequest))
            .assertNext { response ->
                assert(response.roles.size == 1)
                assert(response.roles.contains("USER"))
            }
            .expectComplete()
            .verify(Duration.ofSeconds(10))
    }

    // ==================== Password Hashing Tests ====================

    @Test
    @DisplayName("Should hash password correctly")
    fun testHashPassword() {
        val rawPassword = "Password123"

        val hashedPassword = authService.hashPassword(rawPassword)

        assert(hashedPassword != rawPassword)
        assert(hashedPassword.startsWith("\$2a\$") || hashedPassword.startsWith("\$2b\$"))
    }

    @Test
    @DisplayName("Should verify correct password")
    fun testVerifyPasswordCorrect() {
        val rawPassword = "Password123"
        val hashedPassword = authService.hashPassword(rawPassword)

        val result = authService.verifyPassword(rawPassword, hashedPassword)

        assert(result)
    }

    @Test
    @DisplayName("Should reject incorrect password")
    fun testVerifyPasswordIncorrect() {
        val rawPassword = "Password123"
        val hashedPassword = authService.hashPassword(rawPassword)

        val result = authService.verifyPassword("WrongPassword", hashedPassword)

        assert(!result)
    }

    @Test
    @DisplayName("Should generate different hashes for same password")
    fun testHashPasswordGeneratesDifferentHashes() {
        val rawPassword = "Password123"

        val hash1 = authService.hashPassword(rawPassword)
        val hash2 = authService.hashPassword(rawPassword)

        assert(hash1 != hash2) // BCrypt generates different salts
        assert(authService.verifyPassword(rawPassword, hash1))
        assert(authService.verifyPassword(rawPassword, hash2))
    }
}
