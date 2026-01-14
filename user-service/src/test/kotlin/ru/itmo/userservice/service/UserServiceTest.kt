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
import ru.itmo.userservice.exception.ConflictException
import ru.itmo.userservice.exception.ResourceNotFoundException
import ru.itmo.userservice.model.dto.request.RegisterRequest
import ru.itmo.userservice.model.dto.request.UpdateProfileRequest
import ru.itmo.userservice.model.enums.UserRole
import ru.itmo.userservice.repository.UserRepository
import ru.itmo.userservice.repository.UserRoleRepository
import java.time.Duration

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@DisplayName("User Service Tests")
class UserServiceTest {

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

    // ==================== Register Tests ====================

    @Test
    @DisplayName("Should register user successfully")
    fun testRegisterUserSuccess() {
        val request = RegisterRequest(
            username = "testuser",
            email = "test@example.com",
            password = "Password123",
            firstName = "Test",
            lastName = "User"
        )

        StepVerifier.create(userService.register(request))
            .assertNext { response ->
                assert(response.username == "testuser")
                assert(response.userId > 0)
                assert(response.token.isNotBlank())
                assert(response.roles.contains("USER"))
            }
            .expectComplete()
            .verify(Duration.ofSeconds(10))
    }

    @Test
    @DisplayName("Should fail registration with duplicate username")
    fun testRegisterDuplicateUsername() {
        val request1 = RegisterRequest(
            username = "testuser",
            email = "test1@example.com",
            password = "Password123",
            firstName = "Test",
            lastName = "User"
        )

        userService.register(request1).block()

        val request2 = RegisterRequest(
            username = "testuser",
            email = "test2@example.com",
            password = "Password123",
            firstName = "Test",
            lastName = "User"
        )

        StepVerifier.create(userService.register(request2))
            .expectError(ConflictException::class.java)
            .verify(Duration.ofSeconds(10))
    }

    @Test
    @DisplayName("Should fail registration with duplicate email")
    fun testRegisterDuplicateEmail() {
        val request1 = RegisterRequest(
            username = "testuser1",
            email = "test@example.com",
            password = "Password123",
            firstName = "Test",
            lastName = "User"
        )

        userService.register(request1).block()

        val request2 = RegisterRequest(
            username = "testuser2",
            email = "test@example.com",
            password = "Password123",
            firstName = "Test",
            lastName = "User"
        )

        StepVerifier.create(userService.register(request2))
            .expectError(ConflictException::class.java)
            .verify(Duration.ofSeconds(10))
    }

    @Test
    @DisplayName("Should fail registration with short password")
    fun testRegisterShortPassword() {
        val request = RegisterRequest(
            username = "testuser",
            email = "test@example.com",
            password = "short",
            firstName = "Test",
            lastName = "User"
        )

        StepVerifier.create(userService.register(request))
            .expectError(BadRequestException::class.java)
            .verify(Duration.ofSeconds(10))
    }

    @Test
    @DisplayName("Should fail registration with empty username")
    fun testRegisterEmptyUsername() {
        val request = RegisterRequest(
            username = "   ",
            email = "test@example.com",
            password = "Password123",
            firstName = "Test",
            lastName = "User"
        )

        StepVerifier.create(userService.register(request))
            .expectError(BadRequestException::class.java)
            .verify(Duration.ofSeconds(10))
    }

    @Test
    @DisplayName("Should fail registration with empty email")
    fun testRegisterEmptyEmail() {
        val request = RegisterRequest(
            username = "testuser",
            email = "   ",
            password = "Password123",
            firstName = "Test",
            lastName = "User"
        )

        StepVerifier.create(userService.register(request))
            .expectError(BadRequestException::class.java)
            .verify(Duration.ofSeconds(10))
    }

    // ==================== Get User By ID Tests ====================

    @Test
    @DisplayName("Should get user by ID")
    fun testGetUserById() {
        val registerRequest = RegisterRequest(
            username = "testuser",
            email = "test@example.com",
            password = "Password123",
            firstName = "Test",
            lastName = "User"
        )

        val registeredUser = userService.register(registerRequest).block()!!

        StepVerifier.create(userService.getUserById(registeredUser.userId))
            .assertNext { response ->
                assert(response.id == registeredUser.userId)
                assert(response.username == "testuser")
            }
            .expectComplete()
            .verify(Duration.ofSeconds(10))
    }

    @Test
    @DisplayName("Should fail getting non-existent user")
    fun testGetNonExistentUser() {
        StepVerifier.create(userService.getUserById(999L))
            .expectError(ResourceNotFoundException::class.java)
            .verify(Duration.ofSeconds(10))
    }

    @Test
    @DisplayName("Should fail getting user with invalid ID")
    fun testGetUserByInvalidId() {
        StepVerifier.create(userService.getUserById(-1L))
            .expectError(BadRequestException::class.java)
            .verify(Duration.ofSeconds(10))
    }

    @Test
    @DisplayName("Should fail getting user with zero ID")
    fun testGetUserByZeroId() {
        StepVerifier.create(userService.getUserById(0L))
            .expectError(BadRequestException::class.java)
            .verify(Duration.ofSeconds(10))
    }

    // ==================== Get Current User Tests ====================

    @Test
    @DisplayName("Should get current user successfully")
    fun testGetCurrentUser() {
        val registerRequest = RegisterRequest(
            username = "testuser",
            email = "test@example.com",
            password = "Password123",
            firstName = "Test",
            lastName = "User"
        )

        val registeredUser = userService.register(registerRequest).block()!!

        StepVerifier.create(userService.getCurrentUser(registeredUser.userId))
            .assertNext { response ->
                assert(response.id == registeredUser.userId)
                assert(response.username == "testuser")
                assert(response.email == "test@example.com")
            }
            .expectComplete()
            .verify(Duration.ofSeconds(10))
    }

    @Test
    @DisplayName("Should fail getting current user with invalid ID")
    fun testGetCurrentUserInvalidId() {
        StepVerifier.create(userService.getCurrentUser(-1L))
            .expectError(BadRequestException::class.java)
            .verify(Duration.ofSeconds(10))
    }

    @Test
    @DisplayName("Should fail getting non-existent current user")
    fun testGetCurrentUserNotFound() {
        StepVerifier.create(userService.getCurrentUser(999L))
            .expectError(ResourceNotFoundException::class.java)
            .verify(Duration.ofSeconds(10))
    }

    // ==================== Get User By Username Tests ====================

    @Test
    @DisplayName("Should get user by username")
    fun testGetUserByUsername() {
        val registerRequest = RegisterRequest(
            username = "testuser",
            email = "test@example.com",
            password = "Password123",
            firstName = "Test",
            lastName = "User"
        )

        userService.register(registerRequest).block()

        StepVerifier.create(userService.getUserByUsername("testuser"))
            .assertNext { response ->
                assert(response.username == "testuser")
                assert(response.email == "test@example.com")
            }
            .expectComplete()
            .verify(Duration.ofSeconds(10))
    }

    @Test
    @DisplayName("Should fail getting user by non-existent username")
    fun testGetUserByNonExistentUsername() {
        StepVerifier.create(userService.getUserByUsername("nonexistent"))
            .expectError(ResourceNotFoundException::class.java)
            .verify(Duration.ofSeconds(10))
    }

    @Test
    @DisplayName("Should fail getting user by empty username")
    fun testGetUserByEmptyUsername() {
        StepVerifier.create(userService.getUserByUsername("   "))
            .expectError(BadRequestException::class.java)
            .verify(Duration.ofSeconds(10))
    }

    // ==================== Update Profile Tests ====================

    @Test
    @DisplayName("Should update user profile")
    fun testUpdateProfile() {
        val registerRequest = RegisterRequest(
            username = "testuser",
            email = "test@example.com",
            password = "Password123",
            firstName = "Test",
            lastName = "User"
        )

        val registeredUser = userService.register(registerRequest).block()!!

        val updateRequest = UpdateProfileRequest(
            email = "newemail@example.com",
            firstName = "Updated",
            lastName = "Name"
        )

        StepVerifier.create(userService.updateProfile(registeredUser.userId, updateRequest))
            .assertNext { response ->
                assert(response.email == "newemail@example.com")
                assert(response.firstName == "Updated")
                assert(response.lastName == "Name")
            }
            .expectComplete()
            .verify(Duration.ofSeconds(10))
    }

    @Test
    @DisplayName("Should update profile with partial data")
    fun testUpdateProfilePartial() {
        val registerRequest = RegisterRequest(
            username = "testuser",
            email = "test@example.com",
            password = "Password123",
            firstName = "Test",
            lastName = "User"
        )

        val registeredUser = userService.register(registerRequest).block()!!

        val updateRequest = UpdateProfileRequest(
            email = null,
            firstName = "Updated",
            lastName = null
        )

        StepVerifier.create(userService.updateProfile(registeredUser.userId, updateRequest))
            .assertNext { response ->
                assert(response.email == "test@example.com")
                assert(response.firstName == "Updated")
                assert(response.lastName == "User")
            }
            .expectComplete()
            .verify(Duration.ofSeconds(10))
    }

    @Test
    @DisplayName("Should fail update profile for non-existent user")
    fun testUpdateProfileNotFound() {
        val updateRequest = UpdateProfileRequest(
            email = "new@example.com",
            firstName = "Test",
            lastName = "User"
        )

        StepVerifier.create(userService.updateProfile(999L, updateRequest))
            .expectError(ResourceNotFoundException::class.java)
            .verify(Duration.ofSeconds(10))
    }

    @Test
    @DisplayName("Should fail update profile with invalid user ID")
    fun testUpdateProfileInvalidId() {
        val updateRequest = UpdateProfileRequest(
            email = "new@example.com",
            firstName = "Test",
            lastName = "User"
        )

        StepVerifier.create(userService.updateProfile(-1L, updateRequest))
            .expectError(BadRequestException::class.java)
            .verify(Duration.ofSeconds(10))
    }

    @Test
    @DisplayName("Should fail update profile with duplicate email")
    fun testUpdateProfileDuplicateEmail() {
        val request1 = RegisterRequest(
            username = "testuser1",
            email = "test1@example.com",
            password = "Password123",
            firstName = "Test",
            lastName = "User"
        )
        val request2 = RegisterRequest(
            username = "testuser2",
            email = "test2@example.com",
            password = "Password123",
            firstName = "Test",
            lastName = "User"
        )

        userService.register(request1).block()
        val user2 = userService.register(request2).block()!!

        val updateRequest = UpdateProfileRequest(
            email = "test1@example.com",
            firstName = null,
            lastName = null
        )

        StepVerifier.create(userService.updateProfile(user2.userId, updateRequest))
            .expectError(ConflictException::class.java)
            .verify(Duration.ofSeconds(10))
    }

    // ==================== Delete User Tests ====================

    @Test
    @DisplayName("Should delete user")
    fun testDeleteUser() {
        val registerRequest = RegisterRequest(
            username = "testuser",
            email = "test@example.com",
            password = "Password123",
            firstName = "Test",
            lastName = "User"
        )

        val registeredUser = userService.register(registerRequest).block()!!

        StepVerifier.create(userService.deleteUser(registeredUser.userId))
            .expectComplete()
            .verify(Duration.ofSeconds(10))

        StepVerifier.create(userService.getUserById(registeredUser.userId))
            .expectError(ResourceNotFoundException::class.java)
            .verify(Duration.ofSeconds(10))
    }

    @Test
    @DisplayName("Should fail delete non-existent user")
    fun testDeleteNonExistentUser() {
        StepVerifier.create(userService.deleteUser(999L))
            .expectError(ResourceNotFoundException::class.java)
            .verify(Duration.ofSeconds(10))
    }

    @Test
    @DisplayName("Should fail delete user with invalid ID")
    fun testDeleteUserInvalidId() {
        StepVerifier.create(userService.deleteUser(-1L))
            .expectError(BadRequestException::class.java)
            .verify(Duration.ofSeconds(10))
    }

    // ==================== Role Tests ====================

    @Test
    @DisplayName("Should check user has role")
    fun testHasRole() {
        val registerRequest = RegisterRequest(
            username = "testuser",
            email = "test@example.com",
            password = "Password123",
            firstName = "Test",
            lastName = "User"
        )

        val registeredUser = userService.register(registerRequest).block()!!

        StepVerifier.create(userService.hasRole(registeredUser.userId, UserRole.USER))
            .assertNext { hasRole ->
                assert(hasRole)
            }
            .expectComplete()
            .verify(Duration.ofSeconds(10))
    }

    @Test
    @DisplayName("Should return false when user does not have role")
    fun testHasRoleFalse() {
        val registerRequest = RegisterRequest(
            username = "testuser",
            email = "test@example.com",
            password = "Password123",
            firstName = "Test",
            lastName = "User"
        )

        val registeredUser = userService.register(registerRequest).block()!!

        StepVerifier.create(userService.hasRole(registeredUser.userId, UserRole.ADMIN))
            .assertNext { hasRole ->
                assert(!hasRole)
            }
            .expectComplete()
            .verify(Duration.ofSeconds(10))
    }

    @Test
    @DisplayName("Should fail hasRole with invalid user ID")
    fun testHasRoleInvalidId() {
        StepVerifier.create(userService.hasRole(-1L, UserRole.USER))
            .expectError(BadRequestException::class.java)
            .verify(Duration.ofSeconds(10))
    }

    @Test
    @DisplayName("Should add role to user")
    fun testAddRole() {
        val registerRequest = RegisterRequest(
            username = "testuser",
            email = "test@example.com",
            password = "Password123",
            firstName = "Test",
            lastName = "User"
        )

        val registeredUser = userService.register(registerRequest).block()!!

        StepVerifier.create(userService.addRole(registeredUser.userId, UserRole.ADMIN))
            .expectComplete()
            .verify(Duration.ofSeconds(10))

        StepVerifier.create(userService.hasRole(registeredUser.userId, UserRole.ADMIN))
            .assertNext { hasRole ->
                assert(hasRole)
            }
            .expectComplete()
            .verify(Duration.ofSeconds(10))
    }

    @Test
    @DisplayName("Should fail add role to non-existent user")
    fun testAddRoleNonExistentUser() {
        StepVerifier.create(userService.addRole(999L, UserRole.ADMIN))
            .expectError(ResourceNotFoundException::class.java)
            .verify(Duration.ofSeconds(10))
    }

    @Test
    @DisplayName("Should fail add role with invalid user ID")
    fun testAddRoleInvalidId() {
        StepVerifier.create(userService.addRole(-1L, UserRole.ADMIN))
            .expectError(BadRequestException::class.java)
            .verify(Duration.ofSeconds(10))
    }
}
