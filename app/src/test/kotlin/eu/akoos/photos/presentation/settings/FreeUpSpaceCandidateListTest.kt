/*
 * Photos for Proton
 * Copyright (C) 2026 Akoos <https://akoos.eu>
 *
 * Source:  https://github.com/gitakoos/proton-photos
 * Website: https://www.photosforproton.eu
 *
 * This file is part of Photos for Proton.
 *
 * Photos for Proton is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package eu.akoos.photos.presentation.settings

import eu.akoos.photos.domain.entity.SyncState
import eu.akoos.photos.domain.entity.SyncStatus
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pins settle/prune helpers used by the Free-up grid during reclaim: confirm replaces the list
 * (merging known per-URI sizes); progress deltas remove only named URIs; empty delta is a no-op;
 * bytes never go negative.
 */
class FreeUpSpaceCandidateListTest {

    private fun row(i: Int, sizeBytes: Long = 100L * i) = SyncState(
        localUri = "content://media/external/images/media/$i",
        cloudFileId = "cloud-$i",
        localHash = "hash-$i",
        cloudHash = null,
        status = SyncStatus.SYNCED,
        lastSyncAttemptMs = 0L,
        lastSyncSuccessMs = 1L,
        backedUpAtMs = 1L,
        sizeBytes = sizeBytes,
    )

    @Test
    fun `settle replaces candidates and sums sizeBytes`() {
        val confirmed = listOf(row(1, 10), row(2, 20), row(3, 30))
        val (candidates, bytes) = settleFreeUpCandidates(confirmed)
        assertEquals(confirmed, candidates)
        assertEquals(60L, bytes)
    }

    @Test
    fun `settle merges known bytes when row sizeBytes is zero`() {
        val confirmed = listOf(row(1, 0), row(2, 0), row(3, 5))
        val known = mapOf(
            row(1).localUri to 10L,
            row(2).localUri to 20L,
            row(3).localUri to 1L, // row wins via max(5, 1)
        )
        val (candidates, bytes) = settleFreeUpCandidates(confirmed, known)
        assertEquals(listOf(row(1, 10), row(2, 20), row(3, 5)), candidates)
        assertEquals(35L, bytes)
    }

    @Test
    fun `sequential settle then drops matches final confirmed set`() {
        val preview = listOf(row(1, 10), row(2, 20), row(3, 30), row(4, 40))
        val ready = listOf(row(1, 0), row(2, 0), row(3, 0), row(4, 0))
        val known = preview.associate { it.localUri to it.sizeBytes }
        val (settled, settleBytes) = settleFreeUpCandidates(ready, known)
        assertEquals(preview, settled)
        assertEquals(100L, settleBytes)

        val (afterFirst, bytesAfterFirst) = pruneFreeUpCandidates(
            settled,
            settleBytes,
            completedUris = listOf(row(2).localUri),
        )
        val (afterSecond, bytesAfterSecond) = pruneFreeUpCandidates(
            afterFirst,
            bytesAfterFirst,
            completedUris = listOf(row(4).localUri),
        )
        assertEquals(listOf(row(1, 10), row(3, 30)), afterSecond)
        assertEquals(40L, bytesAfterSecond)
    }

    @Test
    fun `prune removes only named URIs and reduces bytes`() {
        val candidates = listOf(row(1, 10), row(2, 20), row(3, 30))
        val (pruned, bytes) = pruneFreeUpCandidates(
            candidates,
            reclaimableBytes = 60L,
            completedUris = listOf(row(2).localUri),
        )
        assertEquals(listOf(row(1, 10), row(3, 30)), pruned)
        assertEquals(40L, bytes)
    }

    @Test
    fun `empty delta is a no-op`() {
        val candidates = listOf(row(1, 10), row(2, 20))
        val (pruned, bytes) = pruneFreeUpCandidates(
            candidates,
            reclaimableBytes = 30L,
            completedUris = emptyList(),
        )
        assertEquals(candidates, pruned)
        assertEquals(30L, bytes)
    }

    @Test
    fun `bytes floor at zero when sizeBytes exceed reclaimableBytes`() {
        val candidates = listOf(row(1, 50), row(2, 50))
        val (pruned, bytes) = pruneFreeUpCandidates(
            candidates,
            reclaimableBytes = 30L,
            completedUris = listOf(row(1).localUri, row(2).localUri),
        )
        assertEquals(emptyList<SyncState>(), pruned)
        assertEquals(0L, bytes)
    }
}
