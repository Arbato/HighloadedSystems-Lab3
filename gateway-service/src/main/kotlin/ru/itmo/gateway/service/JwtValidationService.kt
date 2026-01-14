package ru.itmo.gateway.service

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Service
import ru.itmo.gateway.config.JwtProperties
import javax.crypto.SecretKey

@Service
class JwtValidationService(private val jwtProperties: JwtProperties) {

    private val secretKey: SecretKey by lazy {
        Keys.hmacShaKeyFor(jwtProperties.secret.toByteArray())
    }

    fun validateToken(token: String): Claims? {
        return try {
            Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .payload
        } catch (e: Exception) {
            null
        }
    }

    fun getUserId(claims: Claims): String = claims.subject

    @Suppress("UNCHECKED_CAST")
    fun getRoles(claims: Claims): List<String> =
        claims["roles"] as? List<String> ?: emptyList()
}
