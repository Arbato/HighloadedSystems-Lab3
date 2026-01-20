package ru.itmo.userservice.domain.repository

import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.r2dbc.repository.R2dbcRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import ru.itmo.userservice.domain.model.entity.UserRoleEntity

@Repository
interface UserRoleRepository : R2dbcRepository<UserRoleEntity, Long> {
    
    /**
     * Получить все роли пользователя
     */
    fun findByUserId(userId: Long): Flux<UserRoleEntity>
    
    /**
     * Удалить все роли пользователя
     */
    fun deleteByUserId(userId: Long): Mono<Void>
    
    /**
     * Проверить наличие конкретной роли у пользователя
     */
    @Query(
        """
        SELECT EXISTS(SELECT 1 FROM user_roles WHERE user_id = :userId AND role = :role)
        """
    )
    fun existsByUserIdAndRole(@Param("userId") userId: Long, @Param("role") role: String): Mono<Boolean>
}
