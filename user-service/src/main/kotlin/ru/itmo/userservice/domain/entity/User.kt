package ru.itmo.userservice.domain.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("users")
data class User(
    @Id
    @Column("id")
    val id: Long? = null,
    
    @Column("username")
    val username: String,
    
    @Column("email")
    val email: String,
    
    @Column("password")
    val password: String,
    
    @Column("first_name")
    val firstName: String,
    
    @Column("last_name")
    val lastName: String,
    
    @Column("created_at")
    val createdAt: LocalDateTime? = LocalDateTime.now(),
    
    @Column("updated_at")
    val updatedAt: LocalDateTime? = LocalDateTime.now()
)
