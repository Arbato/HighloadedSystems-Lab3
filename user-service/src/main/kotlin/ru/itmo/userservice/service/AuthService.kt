package ru.itmo.userservice.service

import org.springframework.transaction.annotation.Transactional
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import ru.itmo.userservice.exception.BadRequestException
import ru.itmo.userservice.exception.ConflictException
import ru.itmo.userservice.exception.ResourceNotFoundException
import ru.itmo.userservice.model.dto.request.LoginRequest
import ru.itmo.userservice.model.dto.request.RegisterRequest
import ru.itmo.userservice.model.dto.response.AuthResponse
import ru.itmo.userservice.model.dto.response.UserResponse
import ru.itmo.userservice.model.entity.User
import ru.itmo.userservice.model.entity.UserRoleEntity
import ru.itmo.userservice.model.enums.UserRole
import ru.itmo.userservice.repository.UserRepository
import ru.itmo.userservice.repository.UserRoleRepository
import ru.itmo.userservice.kafka.publisher.UserEventPublisher
import java.time.LocalDateTime

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val userRoleRepository: UserRoleRepository,
    private val jwtService: JwtService,
    private val userEventPublisher: UserEventPublisher
) {
    private val passwordEncoder = BCryptPasswordEncoder()

    /**
     * Регистрация нового пользователя
     * THROWS: ConflictException если username или email уже существуют
     * THROWS: BadRequestException если валидация не прошла
     */
    @Transactional
    fun register(request: RegisterRequest): Mono<AuthResponse> {
        if (request.username.isBlank() || request.email.isBlank()) {
            return Mono.error(BadRequestException("Username and email cannot be empty"))
        }

        if (request.password.length < 8) {
            return Mono.error(BadRequestException("Password must be at least 8 characters"))
        }

        return userRepository.existsByUsername(request.username)
            .flatMap { exists ->
                if (exists) Mono.error(ConflictException("Username already taken: ${request.username}"))
                else Mono.just(false)
            }
            .flatMap {
                userRepository.existsByEmail(request.email)
            }
            .flatMap { exists ->
                if (exists) Mono.error(ConflictException("Email already registered: ${request.email}"))
                else Mono.just(false)
            }
            .flatMap {
                val hashedPassword = passwordEncoder.encode(request.password)

                val user = User(
                    username = request.username,
                    email = request.email,
                    password = hashedPassword,
                    firstName = request.firstName,
                    lastName = request.lastName,
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now()
                )

                userRepository.save(user)
            }
            .flatMap { savedUser ->
                val defaultRole = UserRoleEntity(
                    userId = savedUser.id!!,
                    role = UserRole.USER.name
                )
                userRoleRepository.save(defaultRole).then(Mono.just(savedUser))
            }
            .flatMap { user ->
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
                        val userResponse = UserResponse(
                            id = userId,
                            username = user.username,
                            email = user.email,
                            firstName = user.firstName,
                            lastName = user.lastName,
                            roles = roles.toSet(),
                            createdAt = user.createdAt!!,
                            updatedAt = user.updatedAt!!
                        )
                        
                        userEventPublisher.publishUserRegistered(userResponse)

                        AuthResponse(
                            token = token,
                            userId = userId,
                            username = user.username,
                            roles = roles.toSet()
                        )
                    }
            }
            .onErrorMap { throwable ->
                when (throwable) {
                    is ConflictException, is BadRequestException -> throwable
                    else -> RuntimeException("Failed to register user", throwable)
                }
            }
    }

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
