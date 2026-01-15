package ru.itmo.userservice.controller

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.http.MediaType
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.containers.PostgreSQLContainer
import ru.itmo.userservice.model.dto.request.AssignRoleRequest
import ru.itmo.userservice.model.dto.request.LoginRequest
import ru.itmo.userservice.model.dto.request.RegisterRequest
import ru.itmo.userservice.model.dto.response.AuthResponse
import ru.itmo.userservice.model.enums.UserRole
import ru.itmo.userservice.repository.UserRepository
import ru.itmo.userservice.repository.UserRoleRepository
import ru.itmo.userservice.service.AuthService
import ru.itmo.userservice.service.UserService

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
@Testcontainers
@DisplayName("Auth Controller Tests")
class AuthControllerTest {

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
    private lateinit var webTestClient: WebTestClient

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var userRoleRepository: UserRoleRepository

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var authService: AuthService

    @BeforeEach
    fun setUp() {
        userRoleRepository.deleteAll().block()
        userRepository.deleteAll().block()
    }

    private fun createTestUser(
        username: String = "testuser",
        email: String = "test@example.com",
        password: String = "Password123"
    ): AuthResponse {
        val request = RegisterRequest(
            username = username,
            email = email,
            password = password,
            firstName = "Test",
            lastName = "User"
        )
        return authService.register(request).block()!!
    }

    private fun createAdminUser(): AuthResponse {
        val user = createTestUser("admin", "admin@example.com")
        userService.addRole(user.userId, UserRole.ADMIN).block()
        return user
    }

    // ==================== Register Endpoint Tests ====================

    @Test
    @DisplayName("POST /api/auth/register - Should register user")
    fun testRegisterUser() {
        val request = RegisterRequest(
            username = "testuser",
            email = "test@example.com",
            password = "Password123",
            firstName = "Test",
            lastName = "User"
        )

        webTestClient.post()
            .uri("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isCreated
            .expectBody()
            .jsonPath("$.username").isEqualTo("testuser")
            .jsonPath("$.userId").isNotEmpty
            .jsonPath("$.token").isNotEmpty
            .jsonPath("$.roles").isArray
    }

    @Test
    @DisplayName("POST /api/auth/register - Should return 409 for duplicate username")
    fun testRegisterDuplicateUsername() {
        val request = RegisterRequest(
            username = "testuser",
            email = "test@example.com",
            password = "Password123",
            firstName = "Test",
            lastName = "User"
        )

        webTestClient.post()
            .uri("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isCreated

        val request2 = request.copy(email = "test2@example.com")

        webTestClient.post()
            .uri("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request2)
            .exchange()
            .expectStatus().isEqualTo(409)
    }

    @Test
    @DisplayName("POST /api/auth/register - Should return 409 for duplicate email")
    fun testRegisterDuplicateEmail() {
        val request = RegisterRequest(
            username = "testuser1",
            email = "test@example.com",
            password = "Password123",
            firstName = "Test",
            lastName = "User"
        )

        webTestClient.post()
            .uri("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isCreated

        val request2 = request.copy(username = "testuser2")

        webTestClient.post()
            .uri("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request2)
            .exchange()
            .expectStatus().isEqualTo(409)
    }

    @Test
    @DisplayName("POST /api/auth/register - Should return 400 for short password")
    fun testRegisterShortPassword() {
        val request = RegisterRequest(
            username = "testuser",
            email = "test@example.com",
            password = "short",
            firstName = "Test",
            lastName = "User"
        )

        webTestClient.post()
            .uri("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isBadRequest
    }

    // ==================== Login Endpoint Tests ====================

    @Test
    @DisplayName("POST /api/auth/login - Should login successfully")
    fun testLoginSuccess() {
        createTestUser()

        val loginRequest = LoginRequest(
            username = "testuser",
            password = "Password123"
        )

        webTestClient.post()
            .uri("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(loginRequest)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.token").isNotEmpty
            .jsonPath("$.username").isEqualTo("testuser")
            .jsonPath("$.roles").isArray
    }

    @Test
    @DisplayName("POST /api/auth/login - Should return 404 for non-existent user")
    fun testLoginUserNotFound() {
        val loginRequest = LoginRequest(
            username = "nonexistent",
            password = "Password123"
        )

        webTestClient.post()
            .uri("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(loginRequest)
            .exchange()
            .expectStatus().isNotFound
    }

    @Test
    @DisplayName("POST /api/auth/login - Should return 400 for invalid password")
    fun testLoginInvalidPassword() {
        createTestUser()

        val loginRequest = LoginRequest(
            username = "testuser",
            password = "WrongPassword"
        )

        webTestClient.post()
            .uri("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(loginRequest)
            .exchange()
            .expectStatus().isBadRequest
    }

    // ==================== Assign Role Endpoint Tests ====================

    @Test
    @DisplayName("POST /api/auth/roles/assign - Should assign SELLER role as admin")
    fun testAssignSellerRole() {
        val admin = createAdminUser()
        val user = createTestUser("seller", "seller@example.com")

        val assignRequest = AssignRoleRequest(
            userId = user.userId,
            role = "SELLER"
        )

        webTestClient.post()
            .uri("/api/auth/roles/assign")
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-User-Id", admin.userId.toString())
            .header("X-User-Roles", "USER,ADMIN")
            .bodyValue(assignRequest)
            .exchange()
            .expectStatus().isOk
    }

    @Test
    @DisplayName("POST /api/auth/roles/assign - Should assign MODERATOR role as admin")
    fun testAssignModeratorRole() {
        val admin = createAdminUser()
        val user = createTestUser("moderator", "moderator@example.com")

        val assignRequest = AssignRoleRequest(
            userId = user.userId,
            role = "MODERATOR"
        )

        webTestClient.post()
            .uri("/api/auth/roles/assign")
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-User-Id", admin.userId.toString())
            .header("X-User-Roles", "USER,ADMIN")
            .bodyValue(assignRequest)
            .exchange()
            .expectStatus().isOk
    }

    @Test
    @DisplayName("POST /api/auth/roles/assign - Should return 403 for non-admin user")
    fun testAssignRoleNonAdmin() {
        val user = createTestUser()
        val targetUser = createTestUser("target", "target@example.com")

        val assignRequest = AssignRoleRequest(
            userId = targetUser.userId,
            role = "SELLER"
        )

        webTestClient.post()
            .uri("/api/auth/roles/assign")
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-User-Id", user.userId.toString())
            .header("X-User-Roles", "USER")
            .bodyValue(assignRequest)
            .exchange()
            .expectStatus().isForbidden
    }

    @Test
    @DisplayName("POST /api/auth/roles/assign - Should return 400 when trying to assign ADMIN role")
    fun testAssignAdminRoleForbidden() {
        val admin = createAdminUser()
        val user = createTestUser("target", "target@example.com")

        val assignRequest = AssignRoleRequest(
            userId = user.userId,
            role = "ADMIN"
        )

        webTestClient.post()
            .uri("/api/auth/roles/assign")
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-User-Id", admin.userId.toString())
            .header("X-User-Roles", "USER,ADMIN")
            .bodyValue(assignRequest)
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test
    @DisplayName("POST /api/auth/roles/assign - Should return 400 for invalid role")
    fun testAssignInvalidRole() {
        val admin = createAdminUser()
        val user = createTestUser("target", "target@example.com")

        val assignRequest = AssignRoleRequest(
            userId = user.userId,
            role = "INVALID_ROLE"
        )

        webTestClient.post()
            .uri("/api/auth/roles/assign")
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-User-Id", admin.userId.toString())
            .header("X-User-Roles", "USER,ADMIN")
            .bodyValue(assignRequest)
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test
    @DisplayName("POST /api/auth/roles/assign - Should return 404 for non-existent user")
    fun testAssignRoleUserNotFound() {
        val admin = createAdminUser()

        val assignRequest = AssignRoleRequest(
            userId = 9999L,
            role = "SELLER"
        )

        webTestClient.post()
            .uri("/api/auth/roles/assign")
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-User-Id", admin.userId.toString())
            .header("X-User-Roles", "USER,ADMIN")
            .bodyValue(assignRequest)
            .exchange()
            .expectStatus().isNotFound
    }

    @Test
    @DisplayName("POST /api/auth/roles/assign - Should handle lowercase role name")
    fun testAssignRoleLowercase() {
        val admin = createAdminUser()
        val user = createTestUser("seller", "seller@example.com")

        val assignRequest = AssignRoleRequest(
            userId = user.userId,
            role = "seller"
        )

        webTestClient.post()
            .uri("/api/auth/roles/assign")
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-User-Id", admin.userId.toString())
            .header("X-User-Roles", "USER,ADMIN")
            .bodyValue(assignRequest)
            .exchange()
            .expectStatus().isOk
    }
}
