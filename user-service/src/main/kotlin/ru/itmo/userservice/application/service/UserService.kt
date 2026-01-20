package ru.itmo.userservice.application.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono
import ru.itmo.userservice.adapter.exception.BadRequestException
import ru.itmo.userservice.adapter.exception.ConflictException
import ru.itmo.userservice.adapter.exception.ForbiddenException
import ru.itmo.userservice.adapter.exception.ResourceNotFoundException
import ru.itmo.userservice.domain.model.dto.request.UpdateProfileRequest
import ru.itmo.userservice.domain.model.dto.response.UserResponse
import ru.itmo.userservice.domain.model.entity.UserRoleEntity
import ru.itmo.userservice.domain.model.enums.UserRole
import ru.itmo.userservice.domain.repository.UserRepository
import ru.itmo.userservice.domain.repository.UserRoleRepository
import java.time.LocalDateTime

@Service
class UserService(
    private val userRepository: UserRepository,
    private val userRoleRepository: UserRoleRepository
) {

    /**
     * Получить профиль текущего пользователя
     * THROWS: ResourceNotFoundException если пользователь не найден
     */
    fun getCurrentUser(userId: Long): Mono<UserResponse> {
        if (userId <= 0) {
            return Mono.error(BadRequestException("Invalid user ID: $userId"))
        }
        
        return userRepository.findById(userId)
            .switchIfEmpty(Mono.error(ResourceNotFoundException("User not found with ID: $userId")))
            .flatMap { user ->
                getRolesByUserId(userId)
                    .map { user to it }
            }
            .map { (user, roles) ->
                UserResponse(
                    id = user.id!!,
                    username = user.username,
                    email = user.email,
                    firstName = user.firstName,
                    lastName = user.lastName,
                    roles = roles.toSet(),
                    createdAt = user.createdAt!!,
                    updatedAt = user.updatedAt!!
                )
            }
    }
    
    /**
     * Получить пользователя по ID
     * THROWS: ResourceNotFoundException если пользователь не найден
     */
    fun getUserById(userId: Long): Mono<UserResponse> {
        if (userId <= 0) {
            return Mono.error(BadRequestException("Invalid user ID: $userId"))
        }
        
        return userRepository.findById(userId)
            .switchIfEmpty(Mono.error(ResourceNotFoundException("User not found with ID: $userId")))
            .flatMap { user ->
                getRolesByUserId(userId)
                    .map { user to it }
            }
            .map { (user, roles) ->
                UserResponse(
                    id = user.id!!,
                    username = user.username,
                    email = user.email,
                    firstName = user.firstName,
                    lastName = user.lastName,
                    roles = roles.toSet(),
                    createdAt = user.createdAt!!,
                    updatedAt = user.updatedAt!!
                )
            }
    }
    
    /**
     * Получить пользователя по username
     * THROWS: ResourceNotFoundException если пользователь не найден
     */
    fun getUserByUsername(username: String): Mono<UserResponse> {
        if (username.isBlank()) {
            return Mono.error(BadRequestException("Username cannot be empty"))
        }
        
        return userRepository.findByUsername(username)
            .switchIfEmpty(Mono.error(ResourceNotFoundException("User not found with username: $username")))
            .flatMap { user ->
                getRolesByUserId(user.id!!)
                    .map { user to it }
            }
            .map { (user, roles) ->
                UserResponse(
                    id = user.id!!,
                    username = user.username,
                    email = user.email,
                    firstName = user.firstName,
                    lastName = user.lastName,
                    roles = roles.toSet(),
                    createdAt = user.createdAt!!,
                    updatedAt = user.updatedAt!!
                )
            }
    }
    
    /**
     * Обновить профиль пользователя
     * THROWS: ResourceNotFoundException если пользователь не найден
     * THROWS: ConflictException если email уже используется
     * THROWS: BadRequestException если email некорректный
     */
    @Transactional
    fun updateProfile(
        userId: Long,
        request: UpdateProfileRequest
    ): Mono<UserResponse> {
        if (userId <= 0) {
            return Mono.error(BadRequestException("Invalid user ID: $userId"))
        }
        
        return userRepository.findById(userId)
            .switchIfEmpty(Mono.error(ResourceNotFoundException("User not found with ID: $userId")))
            .flatMap { currentUser ->
                // Если email не пустой и отличается от текущего, проверяем конфликт
                if (request.email != null && request.email.isNotBlank() && request.email != currentUser.email) {
                    if (!isValidEmail(request.email)) {
                        return@flatMap Mono.error(BadRequestException("Invalid email format"))
                    }
                    
                    userRepository.existsByEmail(request.email)
                        .flatMap { exists ->
                            if (exists) {
                                Mono.error(ConflictException("Email already registered: ${request.email}"))
                            } else {
                                Mono.just(currentUser)
                            }
                        }
                } else {
                    Mono.just(currentUser)
                }
            }
            .flatMap { user ->
                val updatedUser = user.copy(
                    email = request.email ?: user.email,
                    firstName = request.firstName ?: user.firstName,
                    lastName = request.lastName ?: user.lastName,
                    updatedAt = LocalDateTime.now()
                )
                
                userRepository.save(updatedUser)
            }
            .flatMap { user ->
                getRolesByUserId(userId)
                    .map { user to it }
            }
            .map { (user, roles) ->
                UserResponse(
                    id = user.id!!,
                    username = user.username,
                    email = user.email,
                    firstName = user.firstName,
                    lastName = user.lastName,
                    roles = roles.toSet(),
                    createdAt = user.createdAt!!,
                    updatedAt = user.updatedAt!!
                )
            }
    }
    
    /**
     * Удалить пользователя
     * THROWS: ResourceNotFoundException если пользователь не найден
     */
    @Transactional
    fun deleteUser(userId: Long, currentUserId: Long): Mono<Void> {
        if (userId <= 0) {
            return Mono.error(BadRequestException("Invalid user ID: $userId"))
        }

        return if (userId == currentUserId) {
            userRepository.findById(userId)
                .switchIfEmpty(Mono.error(ResourceNotFoundException("User not found with ID: $userId")))
                .flatMap {
                    userRoleRepository.deleteByUserId(userId)
                        .then(userRepository.deleteById(userId))
                }
        } else {
            hasRole(currentUserId, UserRole.ADMIN)
                .flatMap { isAdmin ->
                    if (!isAdmin) {
                        Mono.error(ForbiddenException("Only admins can delete other users"))
                    } else {
                        userRepository.findById(userId)
                            .switchIfEmpty(Mono.error(ResourceNotFoundException("User not found with ID: $userId")))
                            .flatMap {
                                userRoleRepository.deleteByUserId(userId)
                                    .then(userRepository.deleteById(userId))
                            }
                    }
                }
        }
    }
    
    /**
     * Проверить, имеет ли пользователь конкретную роль
     */
    fun hasRole(userId: Long, role: UserRole): Mono<Boolean> {
        if (userId <= 0) {
            return Mono.error(BadRequestException("Invalid user ID: $userId"))
        }
        
        return userRoleRepository.existsByUserIdAndRole(userId, role.name)
    }
    
    /**
     * Добавить роль пользователю
     * THROWS: ResourceNotFoundException если пользователь не найден
     */
    @Transactional
    fun addRole(userId: Long, role: UserRole): Mono<Void> {
        if (userId <= 0) {
            return Mono.error(BadRequestException("Invalid user ID: $userId"))
        }
        
        return userRepository.findById(userId)
            .switchIfEmpty(Mono.error(ResourceNotFoundException("User not found with ID: $userId")))
            .flatMap {
                val userRole = UserRoleEntity(
                    userId = userId,
                    role = role.name
                )
                userRoleRepository.save(userRole)
                    .then()
            }
    }
    
    /**
     * Вспомогательный метод для получения ролей пользователя
     */
    private fun getRolesByUserId(userId: Long): Mono<List<String>> {
        return userRoleRepository.findByUserId(userId)
            .map { it.role }
            .collectList()
            .defaultIfEmpty(listOf(UserRole.USER.name))
    }

    /**
     * Валидация email
     */
    private fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9+_.-]+@(.+)\$".toRegex()
        return emailRegex.matches(email)
    }
}
