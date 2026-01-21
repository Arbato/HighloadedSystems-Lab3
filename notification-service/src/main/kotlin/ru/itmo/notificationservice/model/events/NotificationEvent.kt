package ru.itmo.notificationservice.model.events

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.LocalDateTime

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "event_type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = UserRegisteredEvent::class, name = "USER_REGISTERED"),
    JsonSubTypes.Type(value = UserProfileUpdatedEvent::class, name = "USER_PROFILE_UPDATED"),
    JsonSubTypes.Type(value = UserDeletedEvent::class, name = "USER_DELETED"),
    JsonSubTypes.Type(value = UserRoleAssignedEvent::class, name = "USER_ROLE_ASSIGNED")
)
abstract class NotificationEvent {
    abstract val userId: Long
    abstract val eventType: String
    
    @JsonProperty("timestamp")
    val timestamp: LocalDateTime = LocalDateTime.now()
}

data class UserRegisteredEvent(
    @JsonProperty("user_id")
    override val userId: Long,
    
    @JsonProperty("username")
    val username: String,
    
    @JsonProperty("email")
    val email: String,
    
    @JsonProperty("first_name")
    val firstName: String,
    
    @JsonProperty("last_name")
    val lastName: String,
    
    @JsonProperty("event_type")
    override val eventType: String = "USER_REGISTERED"
) : NotificationEvent()

data class UserProfileUpdatedEvent(
    @JsonProperty("user_id")
    override val userId: Long,
    
    @JsonProperty("email")
    val email: String? = null,
    
    @JsonProperty("first_name")
    val firstName: String? = null,
    
    @JsonProperty("last_name")
    val lastName: String? = null,
    
    @JsonProperty("event_type")
    override val eventType: String = "USER_PROFILE_UPDATED"
) : NotificationEvent()

data class UserDeletedEvent(
    @JsonProperty("user_id")
    override val userId: Long,
    
    @JsonProperty("username")
    val username: String,
    
    @JsonProperty("event_type")
    override val eventType: String = "USER_DELETED"
) : NotificationEvent()

data class UserRoleAssignedEvent(
    @JsonProperty("user_id")
    override val userId: Long,
    
    @JsonProperty("role")
    val role: String,
    
    @JsonProperty("username")
    val username: String,
    
    @JsonProperty("event_type")
    override val eventType: String = "USER_ROLE_ASSIGNED"
) : NotificationEvent()
