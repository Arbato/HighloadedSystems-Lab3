package ru.itmo.userservice.application.service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Assertions.*
import ru.itmo.userservice.application.service.JwtService
import ru.itmo.userservice.config.JwtProperties

@DisplayName("JWT Service Tests")
class JwtServiceTest {

    private lateinit var jwtService: JwtService
    private lateinit var jwtProperties: JwtProperties

    @BeforeEach
    fun setUp() {
        jwtProperties = JwtProperties(
            secret = "test-secret-key-for-jwt-min-32-characters-long",
            expiration = 86400000 // 24 hours
        )
        jwtService = JwtService(jwtProperties)
    }

    // ==================== Generate Token Tests ====================

    @Test
    @DisplayName("Should generate valid JWT token")
    fun testGenerateToken() {
        val userId = 1L
        val username = "testuser"
        val roles = setOf("USER", "SELLER")

        val token = jwtService.generateToken(userId, username, roles)

        assertNotNull(token)
        assertTrue(token.isNotBlank())
        assertTrue(token.split(".").size == 3) // JWT has 3 parts
    }

    @Test
    @DisplayName("Should generate token with correct claims")
    fun testGenerateTokenWithCorrectClaims() {
        val userId = 42L
        val username = "testuser"
        val roles = setOf("USER", "ADMIN")

        val token = jwtService.generateToken(userId, username, roles)
        val claims = jwtService.validateToken(token)

        assertNotNull(claims)
        assertEquals(userId.toString(), claims!!.subject)
        assertEquals(username, claims["username"])
        assertTrue((claims["roles"] as List<*>).containsAll(roles))
    }

    // ==================== Validate Token Tests ====================

    @Test
    @DisplayName("Should validate correct token")
    fun testValidateToken() {
        val token = jwtService.generateToken(1L, "testuser", setOf("USER"))

        val claims = jwtService.validateToken(token)

        assertNotNull(claims)
    }

    @Test
    @DisplayName("Should return null for invalid token")
    fun testValidateInvalidToken() {
        val invalidToken = "invalid.token.here"

        val claims = jwtService.validateToken(invalidToken)

        assertNull(claims)
    }

    @Test
    @DisplayName("Should return null for malformed token")
    fun testValidateMalformedToken() {
        val malformedToken = "not-a-jwt-token"

        val claims = jwtService.validateToken(malformedToken)

        assertNull(claims)
    }

    @Test
    @DisplayName("Should return null for empty token")
    fun testValidateEmptyToken() {
        val claims = jwtService.validateToken("")

        assertNull(claims)
    }

    @Test
    @DisplayName("Should return null for token signed with different secret")
    fun testValidateTokenWithWrongSecret() {
        val otherJwtProperties = JwtProperties(
            secret = "different-secret-key-min-32-characters-long",
            expiration = 86400000
        )
        val otherJwtService = JwtService(otherJwtProperties)

        val token = otherJwtService.generateToken(1L, "testuser", setOf("USER"))
        val claims = jwtService.validateToken(token)

        assertNull(claims)
    }

    // ==================== Get User ID From Token Tests ====================

    @Test
    @DisplayName("Should get user ID from token")
    fun testGetUserIdFromToken() {
        val userId = 123L
        val token = jwtService.generateToken(userId, "testuser", setOf("USER"))

        val extractedUserId = jwtService.getUserIdFromToken(token)

        assertEquals(userId, extractedUserId)
    }

    @Test
    @DisplayName("Should return null for invalid token when getting user ID")
    fun testGetUserIdFromInvalidToken() {
        val userId = jwtService.getUserIdFromToken("invalid.token")

        assertNull(userId)
    }

    // ==================== Get Roles From Token Tests ====================

    @Test
    @DisplayName("Should get roles from token")
    fun testGetRolesFromToken() {
        val roles = setOf("USER", "SELLER", "ADMIN")
        val token = jwtService.generateToken(1L, "testuser", roles)

        val extractedRoles = jwtService.getRolesFromToken(token)

        assertEquals(roles.size, extractedRoles.size)
        assertTrue(extractedRoles.containsAll(roles))
    }

    @Test
    @DisplayName("Should return empty list for invalid token when getting roles")
    fun testGetRolesFromInvalidToken() {
        val roles = jwtService.getRolesFromToken("invalid.token")

        assertTrue(roles.isEmpty())
    }

    @Test
    @DisplayName("Should handle single role")
    fun testGetSingleRoleFromToken() {
        val roles = setOf("USER")
        val token = jwtService.generateToken(1L, "testuser", roles)

        val extractedRoles = jwtService.getRolesFromToken(token)

        assertEquals(1, extractedRoles.size)
        assertTrue(extractedRoles.contains("USER"))
    }

    @Test
    @DisplayName("Should handle empty roles set")
    fun testGetEmptyRolesFromToken() {
        val token = jwtService.generateToken(1L, "testuser", emptySet())

        val extractedRoles = jwtService.getRolesFromToken(token)

        assertTrue(extractedRoles.isEmpty())
    }
}
