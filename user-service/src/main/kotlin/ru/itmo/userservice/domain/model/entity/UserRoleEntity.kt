package ru.itmo.userservice.domain.model.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Table("user_roles")
data class UserRoleEntity(
    @Id
    @Column("id")
    val id: Long? = null,
    
    @Column("user_id")
    val userId: Long,
    
    @Column("role")
    val role: String
)
