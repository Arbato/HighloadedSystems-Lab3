package ru.itmo.userservice.domain.repository

import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.r2dbc.repository.R2dbcRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import ru.itmo.userservice.domain.model.entity.User

@Repository
interface UserRepository : R2dbcRepository<User, Long> {
    
    /**
     * Поиск пользователя по username
     */
    fun findByUsername(username: String): Mono<User>
    
    /**
     * Поиск пользователя по email
     */
    fun findByEmail(email: String): Mono<User>
    
    /**
     * Проверка существования пользователя по username
     */
    fun existsByUsername(username: String): Mono<Boolean>
    
    /**
     * Проверка существования пользователя по email
     */
    fun existsByEmail(email: String): Mono<Boolean>
    
    /**
     * Поиск всех пользователей (с пагинацией через limit/offset)
     */
    @Query(
        """
        SELECT * FROM users 
        LIMIT :limit OFFSET :offset
        """
    )
    fun findAllUsers(@Param("limit") limit: Int, @Param("offset") offset: Int): Flux<User>
    
    /**
     * Подсчет всех пользователей
     */
    @Query("SELECT COUNT(*) FROM users")
    fun countAllUsers(): Mono<Long>
}
