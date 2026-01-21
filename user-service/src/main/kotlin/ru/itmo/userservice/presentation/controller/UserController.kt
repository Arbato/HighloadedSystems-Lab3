package ru.itmo.userservice.presentation.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import ru.itmo.userservice.application.dto.request.UpdateProfileRequest
import ru.itmo.userservice.application.dto.response.UserResponse
import ru.itmo.userservice.application.dto.response.ErrorResponse
import ru.itmo.userservice.application.service.UserService
import org.springframework.validation.annotation.Validated

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "User management API")
@Validated
class UserController(
    private val userService: UserService
) {
    /**
     * Получить информацию о текущем пользователе
     * GET /api/users/me
     *
     * @param userId ID пользователя из заголовка X-User-Id
     * @return 200 OK с UserResponse
     * @throws BadRequestException если userId некорректный
     * @throws ResourceNotFoundException если пользователь не найден
     */
    @GetMapping("/me")
    @Operation(
        summary = "Get current user profile",
        description = "Returns the profile of the authenticated user",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "User profile retrieved",
                content = [Content(schema = Schema(implementation = UserResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Invalid userId",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "404",
                description = "User not found",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal server error",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    fun getCurrentUser(
        @RequestHeader("X-User-Id")
        @Parameter(hidden = true)
        userId: Long
    ): Mono<ResponseEntity<UserResponse>> {
        return userService.getCurrentUser(userId)
            .map { ResponseEntity.ok(it) }
    }
    
    /**
     * Получить информацию о пользователе по ID
     * GET /api/users/{userId}
     * 
     * @param userId ID пользователя из пути
     * @return 200 OK с UserResponse
     * @throws BadRequestException если userId некорректный
     * @throws ResourceNotFoundException если пользователь не найден
     */
    @GetMapping("/{userId}")
    @Operation(
        summary = "Get user by ID",
        description = "Returns user profile by user ID",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "User found",
                content = [Content(schema = Schema(implementation = UserResponse::class))]
            ),
            ApiResponse(
                responseCode = "400", 
                description = "Invalid userId",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "404", 
                description = "User not found",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "500", 
                description = "Internal server error",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    fun getUserById(@PathVariable userId: Long): Mono<ResponseEntity<UserResponse>> {
        return userService.getUserById(userId)
            .map { ResponseEntity.ok(it) }
    }
    
    /**
     * Получить информацию о пользователе по username
     * GET /api/users/username/{username}
     * 
     * @param username Username пользователя
     * @return 200 OK с UserResponse
     * @throws BadRequestException если username пустой
     * @throws ResourceNotFoundException если пользователь не найден
     */
    @GetMapping("/username/{username}")
    @Operation(
        summary = "Get user by username",
        description = "Returns user profile by username",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "User found",
                content = [Content(schema = Schema(implementation = UserResponse::class))]
            ),
            ApiResponse(
                responseCode = "400", 
                description = "Invalid username",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "404", 
                description = "User not found",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "500", 
                description = "Internal server error",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    fun getUserByUsername(@PathVariable username: String): Mono<ResponseEntity<UserResponse>> {
        return userService.getUserByUsername(username)
            .map { ResponseEntity.ok(it) }
    }
    
    /**
     * Обновить профиль пользователя
     * PUT /api/users/me
     *
     * @param userId ID пользователя из заголовка X-User-Id
     * @param request UpdateProfileRequest с опциональными email, firstName, lastName
     * @return 200 OK с обновленным UserResponse
     * @throws BadRequestException если userId некорректный или некорректные данные
     * @throws ResourceNotFoundException если пользователь не найден
     * @throws ConflictException если email уже используется
     */
    @PutMapping("/me")
    @Operation(
        summary = "Update user profile",
        description = "Updates the profile of the authenticated user (email, firstName, lastName are optional)",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Profile updated successfully",
                content = [Content(schema = Schema(implementation = UserResponse::class))]
            ),
            ApiResponse(responseCode = "400", description = "Invalid userId or input data"),
            ApiResponse(responseCode = "404", description = "User not found"),
            ApiResponse(responseCode = "409", description = "Email already registered"),
            ApiResponse(responseCode = "500", description = "Internal server error")
        ]
    )
    fun updateProfile(
        @RequestHeader("X-User-Id")
        @Parameter(hidden = true)
        userId: Long,
        @Valid @RequestBody request: UpdateProfileRequest
    ): Mono<ResponseEntity<UserResponse>> {
        return userService.updateProfile(userId, request)
            .map { ResponseEntity.ok(it) }
    }
    
    /**
     * Удалить пользователя
     * DELETE /api/users/{userId}
     * 
     * @param userId ID пользователя из пути
     * @return 204 No Content
     * @throws BadRequestException если userId некорректный
     * @throws ResourceNotFoundException если пользователь не найден
     */
    @DeleteMapping("/{userId}")
    @Operation(
        summary = "Delete user",
        description = "Deletes the user and all associated roles",
        responses = [
            ApiResponse(responseCode = "204", description = "User deleted successfully"),
            ApiResponse(responseCode = "400", description = "Invalid userId"),
            ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            ApiResponse(responseCode = "404", description = "User not found"),
            ApiResponse(responseCode = "500", description = "Internal server error")
        ]
    )
    fun deleteUser(
        @PathVariable userId: Long,
        @RequestHeader("X-User-Id")
        @Parameter(hidden = true)
        currentUserId: Long
    ): Mono<ResponseEntity<Void>> {
        return userService.deleteUser(userId, currentUserId)
            .then(Mono.just(ResponseEntity.noContent().build<Void>()))
    }
}
