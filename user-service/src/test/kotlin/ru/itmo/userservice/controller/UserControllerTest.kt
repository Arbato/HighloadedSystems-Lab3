package ru.itmo.userservice.controller

import com.fasterxml.jackson.databind.ObjectMapper
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
import ru.itmo.userservice.model.dto.request.RegisterRequest
import ru.itmo.userservice.model.dto.request.UpdateProfileRequest
import ru.itmo.userservice.model.dto.response.AuthResponse
import ru.itmo.userservice.repository.UserRepository
import ru.itmo.userservice.repository.UserRoleRepository
import ru.itmo.userservice.service.UserService

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
@Testcontainers
@DisplayName("User Controller Tests")
class UserControllerTest {

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
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var userRoleRepository: UserRoleRepository

    @Autowired
    private lateinit var userService: UserService

    @BeforeEach
    fun setUp() {
        userRoleRepository.deleteAll().block()
        userRepository.deleteAll().block()
    }

    private fun createTestUser(
        username: String = "testuser",
        email: String = "test@example.com"
    ): AuthResponse {
        val request = RegisterRequest(
            username = username,
            email = email,
            password = "Password123",
            firstName = "Test",
            lastName = "User"
        )
        return userService.register(request).block()!!
    }

    // ==================== Register Endpoint Tests ====================

    @Test
    @DisplayName("POST /api/users/register - Should register user")
    fun testRegisterUser() {
        val request = RegisterRequest(
            username = "testuser",
            email = "test@example.com",
            password = "Password123",
            firstName = "Test",
            lastName = "User"
        )

        webTestClient.post()
            .uri("/api/users/register")
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
    @DisplayName("POST /api/users/register - Should return 409 for duplicate username")
    fun testRegisterDuplicateUsername() {
        val request = RegisterRequest(
            username = "testuser",
            email = "test@example.com",
            password = "Password123",
            firstName = "Test",
            lastName = "User"
        )

        webTestClient.post()
            .uri("/api/users/register")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isCreated

        val request2 = request.copy(email = "test2@example.com")

        webTestClient.post()
            .uri("/api/users/register")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request2)
            .exchange()
            .expectStatus().isEqualTo(409)
    }

    @Test
    @DisplayName("POST /api/users/register - Should return 409 for duplicate email")
    fun testRegisterDuplicateEmail() {
        val request = RegisterRequest(
            username = "testuser1",
            email = "test@example.com",
            password = "Password123",
            firstName = "Test",
            lastName = "User"
        )

        webTestClient.post()
            .uri("/api/users/register")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isCreated

        val request2 = request.copy(username = "testuser2")

        webTestClient.post()
            .uri("/api/users/register")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request2)
            .exchange()
            .expectStatus().isEqualTo(409)
    }

    @Test
    @DisplayName("POST /api/users/register - Should return 400 for short password")
    fun testRegisterShortPassword() {
        val request = RegisterRequest(
            username = "testuser",
            email = "test@example.com",
            password = "short",
            firstName = "Test",
            lastName = "User"
        )

        webTestClient.post()
            .uri("/api/users/register")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isBadRequest
    }

    // ==================== Get User By ID Endpoint Tests ====================

    @Test
    @DisplayName("GET /api/users/{userId} - Should get user by ID")
    fun testGetUserById() {
        val user = createTestUser()

        webTestClient.get()
            .uri("/api/users/${user.userId}")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.id").isEqualTo(user.userId)
            .jsonPath("$.username").isEqualTo("testuser")
            .jsonPath("$.email").isEqualTo("test@example.com")
    }

    @Test
    @DisplayName("GET /api/users/{userId} - Should return 404 for non-existent user")
    fun testGetUserByIdNotFound() {
        webTestClient.get()
            .uri("/api/users/999")
            .exchange()
            .expectStatus().isNotFound
    }

    @Test
    @DisplayName("GET /api/users/{userId} - Should return 400 for invalid user ID")
    fun testGetUserByInvalidId() {
        webTestClient.get()
            .uri("/api/users/-1")
            .exchange()
            .expectStatus().isBadRequest
    }

    // ==================== Get Current User Endpoint Tests ====================

    @Test
    @DisplayName("GET /api/users/me - Should get current user")
    fun testGetCurrentUser() {
        val user = createTestUser()

        webTestClient.get()
            .uri("/api/users/me")
            .header("X-User-Id", user.userId.toString())
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.id").isEqualTo(user.userId)
            .jsonPath("$.username").isEqualTo("testuser")
    }

    @Test
    @DisplayName("GET /api/users/me - Should return 404 for non-existent user")
    fun testGetCurrentUserNotFound() {
        webTestClient.get()
            .uri("/api/users/me")
            .header("X-User-Id", "999")
            .exchange()
            .expectStatus().isNotFound
    }

    @Test
    @DisplayName("GET /api/users/me - Should return 400 for invalid user ID")
    fun testGetCurrentUserInvalidId() {
        webTestClient.get()
            .uri("/api/users/me")
            .header("X-User-Id", "-1")
            .exchange()
            .expectStatus().isBadRequest
    }

    // ==================== Get User By Username Endpoint Tests ====================

    @Test
    @DisplayName("GET /api/users/username/{username} - Should get user by username")
    fun testGetUserByUsername() {
        createTestUser()

        webTestClient.get()
            .uri("/api/users/username/testuser")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.username").isEqualTo("testuser")
            .jsonPath("$.email").isEqualTo("test@example.com")
    }

    @Test
    @DisplayName("GET /api/users/username/{username} - Should return 404 for non-existent username")
    fun testGetUserByUsernameNotFound() {
        webTestClient.get()
            .uri("/api/users/username/nonexistent")
            .exchange()
            .expectStatus().isNotFound
    }

    // ==================== Update Profile Endpoint Tests ====================

    @Test
    @DisplayName("PUT /api/users/me - Should update user profile")
    fun testUpdateProfile() {
        val user = createTestUser()

        val updateRequest = UpdateProfileRequest(
            email = "newemail@example.com",
            firstName = "Updated",
            lastName = "Name"
        )

        webTestClient.put()
            .uri("/api/users/me")
            .header("X-User-Id", user.userId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(updateRequest)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.email").isEqualTo("newemail@example.com")
            .jsonPath("$.firstName").isEqualTo("Updated")
            .jsonPath("$.lastName").isEqualTo("Name")
    }

    @Test
    @DisplayName("PUT /api/users/me - Should update profile with partial data")
    fun testUpdateProfilePartial() {
        val user = createTestUser()

        val updateRequest = UpdateProfileRequest(
            email = null,
            firstName = "Updated",
            lastName = null
        )

        webTestClient.put()
            .uri("/api/users/me")
            .header("X-User-Id", user.userId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(updateRequest)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.email").isEqualTo("test@example.com")
            .jsonPath("$.firstName").isEqualTo("Updated")
            .jsonPath("$.lastName").isEqualTo("User")
    }

    @Test
    @DisplayName("PUT /api/users/me - Should return 404 for non-existent user")
    fun testUpdateProfileNotFound() {
        val updateRequest = UpdateProfileRequest(
            email = "new@example.com",
            firstName = "Test",
            lastName = "User"
        )

        webTestClient.put()
            .uri("/api/users/me")
            .header("X-User-Id", "999")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(updateRequest)
            .exchange()
            .expectStatus().isNotFound
    }

    @Test
    @DisplayName("PUT /api/users/me - Should return 400 for invalid user ID")
    fun testUpdateProfileInvalidId() {
        val updateRequest = UpdateProfileRequest(
            email = "new@example.com",
            firstName = "Test",
            lastName = "User"
        )

        webTestClient.put()
            .uri("/api/users/me")
            .header("X-User-Id", "-1")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(updateRequest)
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test
    @DisplayName("PUT /api/users/me - Should return 409 for duplicate email")
    fun testUpdateProfileDuplicateEmail() {
        createTestUser("testuser1", "test1@example.com")
        val user2 = createTestUser("testuser2", "test2@example.com")

        val updateRequest = UpdateProfileRequest(
            email = "test1@example.com",
            firstName = null,
            lastName = null
        )

        webTestClient.put()
            .uri("/api/users/me")
            .header("X-User-Id", user2.userId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(updateRequest)
            .exchange()
            .expectStatus().isEqualTo(409)
    }

    // ==================== Delete User Endpoint Tests ====================

    @Test
    @DisplayName("DELETE /api/users/{userId} - Should delete user")
    fun testDeleteUser() {
        val user = createTestUser()

        webTestClient.delete()
            .uri("/api/users/${user.userId}")
            .exchange()
            .expectStatus().isNoContent

        webTestClient.get()
            .uri("/api/users/${user.userId}")
            .exchange()
            .expectStatus().isNotFound
    }

    @Test
    @DisplayName("DELETE /api/users/{userId} - Should return 404 for non-existent user")
    fun testDeleteUserNotFound() {
        webTestClient.delete()
            .uri("/api/users/999")
            .exchange()
            .expectStatus().isNotFound
    }

    @Test
    @DisplayName("DELETE /api/users/{userId} - Should return 400 for invalid user ID")
    fun testDeleteUserInvalidId() {
        webTestClient.delete()
            .uri("/api/users/-1")
            .exchange()
            .expectStatus().isBadRequest
    }
}
