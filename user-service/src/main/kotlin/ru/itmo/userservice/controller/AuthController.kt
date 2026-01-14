package ru.itmo.userservice.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import ru.itmo.userservice.exception.BadRequestException
import ru.itmo.userservice.exception.ForbiddenException
import ru.itmo.userservice.model.dto.request.AssignRoleRequest
import ru.itmo.userservice.model.dto.request.LoginRequest
import ru.itmo.userservice.model.dto.response.AuthResponse
import ru.itmo.userservice.model.enums.UserRole
import ru.itmo.userservice.service.AuthService
import ru.itmo.userservice.service.UserService

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Authentication API")
class AuthController(
    private val authService: AuthService,
    private val userService: UserService
) {

    @PostMapping("/login")
    @Operation(summary = "Login user", description = "Authenticates user and returns JWT token")
    fun login(@Valid @RequestBody request: LoginRequest): Mono<ResponseEntity<AuthResponse>> {
        return authService.login(request)
            .map { ResponseEntity.ok(it) }
    }

    @PostMapping("/roles/assign")
    @Operation(summary = "Assign role to user", description = "Admin only - assigns SELLER or MODERATOR role")
    fun assignRole(
        @RequestHeader("X-User-Id") adminId: Long,
        @RequestHeader("X-User-Roles") roles: String,
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
