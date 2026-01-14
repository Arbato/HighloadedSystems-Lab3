package ru.itmo.userservice.service

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import ru.itmo.userservice.exception.BadRequestException
import ru.itmo.userservice.exception.ResourceNotFoundException
import ru.itmo.userservice.model.dto.request.LoginRequest
import ru.itmo.userservice.model.dto.response.AuthResponse
import ru.itmo.userservice.repository.UserRepository
import ru.itmo.userservice.repository.UserRoleRepository

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val userRoleRepository: UserRoleRepository,
    private val jwtService: JwtService
) {
    private val passwordEncoder = BCryptPasswordEncoder()

    fun login(request: LoginRequest): Mono<AuthResponse> {
        return userRepository.findByUsername(request.username)
            .switchIfEmpty(Mono.error(ResourceNotFoundException("User not found")))
            .flatMap { user ->
                if (!passwordEncoder.matches(request.password, user.password)) {
                    Mono.error(BadRequestException("Invalid credentials"))
                } else {
                    val userId = user.id!!
                    userRoleRepository.findByUserId(userId)
                        .map { it.role }
                        .collectList()
                        .map { roles ->
                            val token = jwtService.generateToken(
                                userId = userId,
                                username = user.username,
                                roles = roles.toSet()
                            )
                            AuthResponse(
                                token = token,
                                userId = userId,
                                username = user.username,
                                roles = roles.toSet()
                            )
                        }
                }
            }
    }

    fun hashPassword(rawPassword: String): String {
        return passwordEncoder.encode(rawPassword)
    }

    fun verifyPassword(rawPassword: String, hashedPassword: String): Boolean {
        return passwordEncoder.matches(rawPassword, hashedPassword)
    }
}
