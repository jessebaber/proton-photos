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

package eu.akoos.photos.domain.usecase

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import eu.akoos.photos.data.api.dto.BatchLinkDto
import eu.akoos.photos.data.api.dto.LinkCoreDto
import eu.akoos.photos.data.db.dao.FreeUpVerifiedDao
import eu.akoos.photos.data.db.entity.SyncStateEntity
import eu.akoos.photos.data.repository.drive.LinkDetailHelpers
import eu.akoos.photos.domain.entity.SyncState
import eu.akoos.photos.domain.entity.SyncStatus
import eu.akoos.photos.domain.repository.DrivePhotoRepository
import eu.akoos.photos.domain.repository.SyncStateRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Coverage for what the Free up space screen shows against what the sweep actually does.
 *
 * The screen lists photos and then permanently deletes their device copies, so the two have to be
 * the same set: a list drawn from a second, similar-looking filter could name a photo the sweep
 * spares, or spare one it takes, and the list would be a promise about a different set than the one
 * that runs. [FreeUpSpaceUseCase.candidates] is that shared query, and these tests pin that the
 * sweep deletes exactly what it returns, in that order.
 *
 * The progress contract is pinned here too, because a screen that reports a count the user watches
 * for minutes must not stall short of the total or run past it.
 */
class FreeUpSpaceProgressTest {

    private val userId = UserId("u1")
    private val volumeId = "vol-1"
    private lateinit var repo: SyncStateRepository
    private lateinit var context: Context
    private lateinit var resolver: ContentResolver
    private lateinit var cloudRepo: DrivePhotoRepository
    private lateinit var linkDetailHelpers: LinkDetailHelpers
    private lateinit var freeUpVerifiedDao: FreeUpVerifiedDao
    private lateinit var useCase: FreeUpSpaceUseCase

    /** Eligible by [FreeUpSpaceUseCase.isEligibleForReclamation]: SYNCED with a real stamp. */
    private fun eligible(i: Int) = SyncState(
        localUri = "content://media/external/images/media/$i",
        cloudFileId = "cloud-$i",
        localHash = "hash-$i",
        cloudHash = null,
        status = SyncStatus.SYNCED,
        lastSyncAttemptMs = 0L,
        lastSyncSuccessMs = 1L,
        backedUpAtMs = 1L,
        sizeBytes = 1024L,
    )

    private fun entityFor(row: SyncState) = SyncStateEntity(
        localUri = row.localUri,
        userId = userId.id,
        cloudFileId = row.cloudFileId,
        localHash = row.localHash,
        cloudHash = null,
        status = SyncStatus.SYNCED,
        lastSyncAttemptMs = 0L,
        lastSyncSuccessMs = 1L,
        backedUpAtMs = 1L,
        sizeBytes = row.sizeBytes,
    )

    private fun linkAt(linkId: String) =
        BatchLinkDto(link = LinkCoreDto(linkId = linkId, type = 2, state = 1))

    /** Parks [rows] as ready and stubs live re-check so [finishReadyForReclaim] reaches reclaim. */
    private fun stubFinishPath(rows: List<SyncState>) {
        coEvery { freeUpVerifiedDao.readyToReclaim(userId.id, any()) } returns rows.map { entityFor(it) }
        coEvery { cloudRepo.getVolumeId(userId) } returns volumeId
        coEvery {
            linkDetailHelpers.batchFetchLinkDetailsStrict(userId, volumeId, any())
        } answers {
            @Suppress("UNCHECKED_CAST")
            val ids = args[2] as List<String>
            ids.associateWith { linkAt(it) }
        }
    }

    @Before
    fun setUp() {
        mockkStatic(Uri::class)
        every { Uri.parse(any()) } returns mockk(relaxed = true)
        resolver = mockk(relaxed = true)
        context = mockk(relaxed = true)
        every { context.contentResolver } returns resolver
        repo = mockk(relaxed = true)
        cloudRepo = mockk(relaxed = true)
        linkDetailHelpers = mockk(relaxed = true)
        freeUpVerifiedDao = mockk(relaxed = true)
        useCase = FreeUpSpaceUseCase(
            context,
            repo,
            cloudRepo,
            linkDetailHelpers,
            freeUpVerifiedDao,
        )
    }

    @Test
    fun `the sweep reclaims exactly the rows the screen was shown`() = runTest {
        val rows = (1..5).map { eligible(it) }
        coEvery { repo.getSyncedBefore(userId, any()) } returns rows
        every { resolver.delete(any(), any(), any()) } returns 1

        val listed = useCase.candidates(userId, Long.MAX_VALUE)
        val result = useCase(userId, Long.MAX_VALUE)

        assertEquals(rows.map { it.localUri }, listed.map { it.localUri })
        assertEquals(rows.size, (result as FreeUpSpaceUseCase.FreeUpResult.Done).freed)
        // Every listed photo, and nothing else, had its row moved off the device.
        for (row in listed) {
            coVerify(exactly = 1) {
                repo.updateStatusAndDeleteLocal(row.localUri, SyncStatus.CLOUD_ONLY)
            }
        }
        coVerify(exactly = listed.size) {
            repo.updateStatusAndDeleteLocal(any<String>(), any())
        }
    }

    @Test
    fun `progress starts at zero, never goes backwards, and ends on the total`() = runTest {
        val rows = (1..45).map { eligible(it) }
        coEvery { repo.getSyncedBefore(userId, any()) } returns rows
        every { resolver.delete(any(), any(), any()) } returns 1

        val reports = mutableListOf<Pair<Int, Int>>()
        useCase(userId, Long.MAX_VALUE) { done, total -> reports += done to total }

        assertEquals(0 to rows.size, reports.first())
        assertEquals(rows.size to rows.size, reports.last())
        assertTrue("every report carries the same total", reports.all { it.second == rows.size })
        assertTrue(
            "progress must not go backwards: $reports",
            reports.zipWithNext().all { (a, b) -> b.first >= a.first },
        )
        assertTrue("no report may exceed the total", reports.all { it.first <= rows.size })
    }

    @Test
    fun `a photo the system refuses is left for the consent dialog rather than counted as freed`() = runTest {
        val rows = (1..3).map { eligible(it) }
        coEvery { repo.getSyncedBefore(userId, any()) } returns rows
        // delete() answering 0 is how the platform reports "not yours to delete" on API 30+.
        every { resolver.delete(any(), any(), any()) } returns 0

        val result = useCase(userId, Long.MAX_VALUE)

        // Nothing was reclaimed, so no row may claim its device copy is gone.
        coVerify(exactly = 0) { repo.updateStatusAndDeleteLocal(any<String>(), any()) }
        coVerify(exactly = 0) { repo.updateStatusAndDeleteLocal(any<List<String>>(), any()) }
        // On a JVM unit test Build.VERSION.SDK_INT reads 0, so the consent branch is skipped and the
        // run reports Done with zero freed. The invariant that matters either way is that a refused
        // photo is never counted.
        if (result is FreeUpSpaceUseCase.FreeUpResult.Done) assertEquals(0, result.freed)
    }

    @Test
    fun `finish reclaim progress deltas union to successful deletes and never overlap`() = runTest {
        val rows = (1..45).map { eligible(it) }
        stubFinishPath(rows)
        every { resolver.delete(any(), any(), any()) } returns 1

        val deltas = mutableListOf<List<String>>()
        val started = mutableListOf<List<SyncState>>()
        useCase.finishReadyForReclaim(
            userId = userId,
            onRecheckStarted = { started += listOf(it) },
            onReclaimProgress = { _, _, _, newlyCompleted ->
                if (newlyCompleted.isNotEmpty()) deltas += newlyCompleted
            },
        )

        assertEquals(rows.map { it.localUri }, started.single().map { it.localUri })
        val union = deltas.flatten()
        assertEquals(rows.map { it.localUri }, union)
        assertEquals("no URI appears twice across deltas", union.size, union.toSet().size)
        for (i in deltas.indices) {
            for (j in i + 1 until deltas.size) {
                assertTrue(
                    "delta $i and $j overlap: ${deltas[i]} / ${deltas[j]}",
                    deltas[i].toSet().intersect(deltas[j].toSet()).isEmpty(),
                )
            }
        }
    }

    @Test
    fun `recheck started equals ready and drop deltas plus survivors equal that set`() = runTest {
        val rows = (1..5).map { eligible(it) }
        coEvery { freeUpVerifiedDao.readyToReclaim(userId.id, any()) } returns rows.map { entityFor(it) }
        coEvery { cloudRepo.getVolumeId(userId) } returns volumeId
        // Odd indices stay active; even indices are inactive (trashed).
        coEvery {
            linkDetailHelpers.batchFetchLinkDetailsStrict(userId, volumeId, any())
        } answers {
            @Suppress("UNCHECKED_CAST")
            val ids = args[2] as List<String>
            ids.associateWith { id ->
                val n = id.removePrefix("cloud-").toInt()
                linkAt(id).let { dto ->
                    if (n % 2 == 0) {
                        BatchLinkDto(link = LinkCoreDto(linkId = id, type = 2, state = 2))
                    } else {
                        dto
                    }
                }
            }
        }
        every { resolver.delete(any(), any(), any()) } returns 1

        val started = mutableListOf<SyncState>()
        val dropDeltas = mutableListOf<List<String>>()
        var recheckReports = 0
        useCase.finishReadyForReclaim(
            userId = userId,
            onRecheckStarted = { started += it },
            onRecheckProgress = { _, _, newlyDropped ->
                recheckReports++
                if (newlyDropped.isNotEmpty()) dropDeltas += newlyDropped
            },
        )

        assertEquals(rows.map { it.localUri }, started.map { it.localUri })
        assertTrue("progress(0,total,empty) then per-chunk", recheckReports >= 2)
        val dropped = dropDeltas.flatten()
        assertEquals("no URI appears twice across drop deltas", dropped.size, dropped.toSet().size)
        for (i in dropDeltas.indices) {
            for (j in i + 1 until dropDeltas.size) {
                assertTrue(
                    "drop delta $i and $j overlap",
                    dropDeltas[i].toSet().intersect(dropDeltas[j].toSet()).isEmpty(),
                )
            }
        }
        val expectedDropped = rows.filterIndexed { index, _ -> (index + 1) % 2 == 0 }.map { it.localUri }
        val expectedSurvivors = rows.filterIndexed { index, _ -> (index + 1) % 2 == 1 }.map { it.localUri }
        assertEquals(expectedDropped.toSet(), dropped.toSet())
        assertTrue("drops are only inactive URIs", dropped.none { it in expectedSurvivors })
        assertEquals(
            "settle + all drops == started ready set",
            started.map { it.localUri }.toSet(),
            (expectedSurvivors + dropped).toSet(),
        )
    }

    @Test
    fun `finish reclaim deltas omit delete-zero URIs while counters stay monotonic`() = runTest {
        val rows = (1..25).map { eligible(it) }
        stubFinishPath(rows)
        // Alternating success / refusal in reclaim order.
        var call = 0
        every { resolver.delete(any(), any(), any()) } answers {
            call++
            if (call % 2 == 1) 1 else 0
        }

        val reports = mutableListOf<Triple<Int, Int, Int>>()
        val deltaUris = mutableListOf<String>()
        useCase.finishReadyForReclaim(
            userId = userId,
            onReclaimProgress = { processed, removed, total, newlyCompleted ->
                reports += Triple(processed, removed, total)
                deltaUris += newlyCompleted
            },
        )

        val expectedSuccess = rows.filterIndexed { index, _ -> index % 2 == 0 }.map { it.localUri }
        val expectedFail = rows.filterIndexed { index, _ -> index % 2 == 1 }.map { it.localUri }
        assertEquals(expectedSuccess, deltaUris)
        assertTrue(expectedFail.none { it in deltaUris })
        assertEquals(0 to rows.size, reports.first().first to reports.first().third)
        assertEquals(rows.size, reports.last().first)
        assertTrue(
            "processed must not go backwards: $reports",
            reports.zipWithNext().all { (a, b) -> b.first >= a.first },
        )
        assertTrue(
            "removed must not go backwards: $reports",
            reports.zipWithNext().all { (a, b) -> b.second >= a.second },
        )
        assertEquals(expectedSuccess.size, reports.last().second)
    }
}
