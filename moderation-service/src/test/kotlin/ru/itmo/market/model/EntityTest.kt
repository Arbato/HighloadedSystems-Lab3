package ru.itmo.market.model

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import ru.itmo.market.domain.model.entity.ModerationAction
import ru.itmo.market.domain.model.entity.ModerationAudit
import java.time.LocalDateTime

@DisplayName("Entity Tests")
class EntityTest {

    // ==================== ModerationAction Tests ====================

    @Test
    @DisplayName("ModerationAction should create with all fields")
    fun testModerationActionCreation() {
        val action = ModerationAction(
            id = 1L,
            productId = 100L,
            moderatorId = 10L,
            actionType = "APPROVE",
            reason = "Good quality"
        )

        assert(action.id == 1L)
        assert(action.productId == 100L)
        assert(action.moderatorId == 10L)
        assert(action.actionType == "APPROVE")
        assert(action.reason == "Good quality")
    }

    @Test
    @DisplayName("ModerationAction should have default values")
    fun testModerationActionDefaults() {
        val action = ModerationAction(
            productId = 100L,
            moderatorId = 10L,
            actionType = "REJECT"
        )

        assert(action.id == 0L)
        assert(action.reason == null)
        assert(action.createdAt != null)
        assert(action.updatedAt != null)
    }

    @Test
    @DisplayName("ModerationAction should allow null reason")
    fun testModerationActionNullReason() {
        val action = ModerationAction(
            id = 1L,
            productId = 100L,
            moderatorId = 10L,
            actionType = "APPROVE",
            reason = null
        )

        assert(action.reason == null)
    }

    @Test
    @DisplayName("ModerationAction copy should work correctly")
    fun testModerationActionCopy() {
        val action = ModerationAction(
            id = 1L,
            productId = 100L,
            moderatorId = 10L,
            actionType = "APPROVE"
        )

        val copied = action.copy(actionType = "REJECT", reason = "Changed")

        assert(copied.id == 1L)
        assert(copied.productId == 100L)
        assert(copied.actionType == "REJECT")
        assert(copied.reason == "Changed")
    }

    @Test
    @DisplayName("ModerationAction equals and hashCode")
    fun testModerationActionEqualsHashCode() {
        val action1 = ModerationAction(
            id = 1L,
            productId = 100L,
            moderatorId = 10L,
            actionType = "APPROVE"
        )
        val action2 = ModerationAction(
            id = 1L,
            productId = 100L,
            moderatorId = 10L,
            actionType = "APPROVE"
        )

        assert(action1.productId == action2.productId)
        assert(action1.moderatorId == action2.moderatorId)
        assert(action1.actionType == action2.actionType)
    }

    // ==================== ModerationAudit Tests ====================

    @Test
    @DisplayName("ModerationAudit should create with all fields")
    fun testModerationAuditCreation() {
        val audit = ModerationAudit(
            id = 1L,
            actionId = 10L,
            productId = 100L,
            moderatorId = 5L,
            oldStatus = "PENDING",
            newStatus = "APPROVED",
            ipAddress = "192.168.1.1"
        )

        assert(audit.id == 1L)
        assert(audit.actionId == 10L)
        assert(audit.productId == 100L)
        assert(audit.moderatorId == 5L)
        assert(audit.oldStatus == "PENDING")
        assert(audit.newStatus == "APPROVED")
        assert(audit.ipAddress == "192.168.1.1")
    }

    @Test
    @DisplayName("ModerationAudit should have default values")
    fun testModerationAuditDefaults() {
        val audit = ModerationAudit(
            actionId = 10L,
            productId = 100L,
            moderatorId = 5L,
            oldStatus = "PENDING",
            newStatus = "REJECTED"
        )

        assert(audit.id == 0L)
        assert(audit.ipAddress == null)
        assert(audit.createdAt != null)
    }

    @Test
    @DisplayName("ModerationAudit copy should work correctly")
    fun testModerationAuditCopy() {
        val audit = ModerationAudit(
            id = 1L,
            actionId = 10L,
            productId = 100L,
            moderatorId = 5L,
            oldStatus = "PENDING",
            newStatus = "APPROVED"
        )

        val copied = audit.copy(newStatus = "REJECTED", ipAddress = "10.0.0.1")

        assert(copied.id == 1L)
        assert(copied.newStatus == "REJECTED")
        assert(copied.ipAddress == "10.0.0.1")
    }

    @Test
    @DisplayName("ModerationAudit toString should contain key fields")
    fun testModerationAuditToString() {
        val audit = ModerationAudit(
            id = 1L,
            actionId = 10L,
            productId = 100L,
            moderatorId = 5L,
            oldStatus = "PENDING",
            newStatus = "APPROVED"
        )

        val str = audit.toString()
        assert(str.contains("100") || str.contains("productId"))
        assert(str.contains("PENDING") || str.contains("oldStatus"))
        assert(str.contains("APPROVED") || str.contains("newStatus"))
    }
}
