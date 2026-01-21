package ru.itmo.userservice.presentation.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import ru.itmo.userservice.adapters.exception.BadRequestException
import ru.itmo.userservice.adapters.exception.ForbiddenException
import ru.itmo.userservice.application.dto.request.AssignRoleRequest
import ru.itmo.userservice.application.dto.request.LoginRequest
import ru.itmo.userservice.application.dto.request.RegisterRequest
import ru.itmo.userservice.application.dto.response.AuthResponse
import ru.itmo.userservice.domain.enums.UserRole
import ru.itmo.userservice.application.service.AuthService
import ru.itmo.userservice.application.service.UserService

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Authentication API")
class AuthController(
    private val authService: AuthService,
    private val userService: UserService
) {

    /**
     * Регистрация нового пользователя
     * POST /api/auth/register
     *
     * @param request RegisterRequest с username, email, password, firstName, lastName
     * @return 201 Created с UserResponse
     * @throws ConflictException если username или email уже существуют
     * @throws BadRequestException если валидация не прошла
     */
    @PostMapping("/register")
    @Operation(
        summary = "Register new user",
        description = "Creates a new user with USER role by default and returns JWT token",
        responses = [
            ApiResponse(
                responseCode = "201",
                description = "User registered successfully",
                content = [Content(schema = Schema(implementation = AuthResponse::class))]
            ),
            ApiResponse(responseCode = "400", description = "Invalid input"),
            ApiResponse(responseCode = "409", description = "Username or email already exists"),
            ApiResponse(responseCode = "500", description = "Internal server error")
        ]
    )
    fun register(@Valid @RequestBody request: RegisterRequest): Mono<ResponseEntity<AuthResponse>> {
        return authService.register(request)
            .map { ResponseEntity.status(HttpStatus.CREATED).body(it) }
    }

    @PostMapping("/login")
    @Operation(summary = "Login user", description = "Authenticates user and returns JWT token")
    fun login(@Valid @RequestBody request: LoginRequest): Mono<ResponseEntity<AuthResponse>> {
        return authService.login(request)
            .map { ResponseEntity.ok(it) }
    }

    @PostMapping("/roles/assign")
    @Operation(summary = "Assign role to user", description = "Admin only - assigns SELLER or MODERATOR role")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Role assigned successfully"),
        ApiResponse(responseCode = "400", description = "Invalid role or cannot assign ADMIN"),
        ApiResponse(responseCode = "403", description = "User is not an admin"),
        ApiResponse(responseCode = "404", description = "Target user not found")
    ])
    fun assignRole(
        @RequestHeader("X-User-Id")
        @Parameter(hidden = true)
        adminId: Long,
        @RequestHeader("X-User-Roles")
        @Parameter(hidden = true)
        roles: String,
        @Valid @RequestBody request: AssignRoleRequest
    ): Mono<ResponseEntity<Void>> {
        if (!roles.contains("ADMIN")) {
            return Mono.error(ForbiddenException("Only admins can assign roles"))
        }

        val roleUpper = request.role.uppercase()
        if (roleUpper == "ADMIN") {
            return Mono.error(BadRequestException("Cannot assign ADMIN role via API"))
        }

        val userRole = try {
            UserRole.valueOf(roleUpper)
        } catch (e: IllegalArgumentException) {
            return Mono.error(BadRequestException("Invalid role: ${request.role}"))
        }

        return userService.addRole(request.userId, userRole)
            .then(Mono.just(ResponseEntity.ok().build()))
    }
}
