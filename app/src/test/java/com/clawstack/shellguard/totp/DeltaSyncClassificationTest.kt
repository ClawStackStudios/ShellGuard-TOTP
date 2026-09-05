package com.clawstack.shellguard.totp

import com.clawstack.shellguard.totp.data.remote.models.PearlDto
import com.clawstack.shellguard.totp.data.repository.TotpRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression tests for the One-Way Sync delta classification in
 * [TotpRepository.classifyDeltaPearls].
 *
 * Guards against the v0.0.1.2 regression where a null server `updated_at`
 * stamp compared equal to a missing local mirror row (null == null), causing
 * brand-new pearls to be classified as "unchanged" — they were never
 * decrypted, never upserted, and sync still reported success.
 */
class DeltaSyncClassificationTest {

    private fun pearl(id: String, updatedAt: String? = null) = PearlDto(
        id = id,
        owner_uuid = "user-remote-123",
        title = "Service $id",
        totp_secret = "envelope-$id",
        updated_at = updatedAt
    )

    @Test
    fun freshInstallWithNullServerStampMustSync() {
        // THE REPRO: fresh install (no local rows) + server omits `updated_at`.
        // Null-to-null must NOT classify as unchanged.
        val result = TotpRepository.classifyDeltaPearls(
            localSnapshotById = emptyMap(),
            pearls = listOf(pearl("pearl-a", updatedAt = null))
        )
        assertEquals(emptyList<String>(), result.first) // nothing skipped
        assertEquals(listOf("pearl-a"), result.second.map { it.id }) // must be decrypted + upserted
    }

    @Test
    fun freshInstallWithNonNullStampMustSync() {
        val result = TotpRepository.classifyDeltaPearls(
            localSnapshotById = emptyMap(),
            pearls = listOf(pearl("pearl-a", updatedAt = "2026-09-04T10:00:00Z"))
        )
        assertTrue(result.first.isEmpty())
        assertEquals(listOf("pearl-a"), result.second.map { it.id })
    }

    @Test
    fun matchingNonNullStampsAreUnchanged() {
        val result = TotpRepository.classifyDeltaPearls(
            localSnapshotById = mapOf("pearl-a" to "2026-09-04T10:00:00Z"),
            pearls = listOf(pearl("pearl-a", updatedAt = "2026-09-04T10:00:00Z"))
        )
        assertEquals(listOf("pearl-a"), result.first)
        assertTrue(result.second.isEmpty())
    }

    @Test
    fun changedStampMustReSync() {
        val result = TotpRepository.classifyDeltaPearls(
            localSnapshotById = mapOf("pearl-a" to "2026-09-04T10:00:00Z"),
            pearls = listOf(pearl("pearl-a", updatedAt = "2026-09-04T11:30:00Z"))
        )
        assertTrue(result.first.isEmpty())
        assertEquals(listOf("pearl-a"), result.second.map { it.id })
    }

    @Test
    fun existingMirrorWithNullRemoteStampSelfHeals() {
        // Devices stuck on the broken build hold mirror rows with a null stamp;
        // a null-stamped remote pearl must re-sync rather than be skipped.
        val result = TotpRepository.classifyDeltaPearls(
            localSnapshotById = mapOf("pearl-a" to null),
            pearls = listOf(pearl("pearl-a", updatedAt = null))
        )
        assertTrue(result.first.isEmpty())
        assertEquals(listOf("pearl-a"), result.second.map { it.id })
    }

    @Test
    fun mixedBatchIsClassifiedCorrectly() {
        val result = TotpRepository.classifyDeltaPearls(
            localSnapshotById = mapOf(
                "unchanged" to "2026-09-04T10:00:00Z",
                "stale" to "2026-09-04T10:00:00Z",
                "null-stamp" to null
            ),
            pearls = listOf(
                pearl("unchanged", updatedAt = "2026-09-04T10:00:00Z"),
                pearl("stale", updatedAt = "2026-09-04T12:00:00Z"),
                pearl("null-stamp", updatedAt = null),
                pearl("brand-new", updatedAt = "2026-09-04T13:00:00Z")
            )
        )
        assertEquals(listOf("unchanged"), result.first)
        assertEquals(
            listOf("stale", "null-stamp", "brand-new"),
            result.second.map { it.id }
        )
    }
}