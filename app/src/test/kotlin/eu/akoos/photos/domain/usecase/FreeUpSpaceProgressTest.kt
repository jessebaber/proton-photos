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
import eu.akoos.photos.data.db.entity.FreeUpVerifiedEntity
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
import io.mockk.slot
import kotlinx.coroutines.CancellationException
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
 *
 * [FreeUpSpaceUseCase.runPipeline] adds scoped automatic vs manual eligibility, missing-URI repair,
 * URI-only [NeedsPermission], batched status/marker writes, and cancel-between-chunk behaviour.
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
    private fun eligible(i: Int, localHash: String = "hash-$i") = SyncState(
        localUri = "content://media/external/images/media/$i",
        cloudFileId = "cloud-$i",
        localHash = localHash,
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

    /** Stub MediaStore _ID probe so [uris] are treated as still present (permission path). */
    private fun stubMediaStorePresent(uris: List<String>) {
        val presence = uris.mapNotNull { uri ->
            val id = mediaStoreRowId(uri) ?: return@mapNotNull null
            id to MediaStoreRowPresence(id = id, isTrashed = false)
        }.toMap()
        every { queryExistingMediaStoreRows(any(), any()) } returns presence
    }

    /** Stub MediaStore _ID probe so no rows exist (missing-URI repair path). */
    private fun stubMediaStoreMissing() {
        every { queryExistingMediaStoreRows(any(), any()) } returns emptyMap()
    }

    @Before
    fun setUp() {
        mockkStatic(Uri::class)
        mockkStatic("eu.akoos.photos.domain.usecase.MediaStoreUriProbeKt")
        every { Uri.parse(any()) } returns mockk(relaxed = true)
        every { queryExistingMediaStoreRows(any(), any()) } answers { callOriginal() }
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
        // Batched status write covers every listed photo, and nothing else.
        val batchSlot = slot<List<String>>()
        coVerify(exactly = 1) {
            repo.updateStatusAndDeleteLocal(capture(batchSlot), SyncStatus.CLOUD_ONLY)
        }
        assertEquals(listed.map { it.localUri }.toSet(), batchSlot.captured.toSet())
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
    fun `a photo the system refuses stays NeedsPermission when the MediaStore row still exists`() =
        runTest {
            val rows = (1..3).map { eligible(it) }
            coEvery { repo.getSyncedBefore(userId, any()) } returns rows
            every { resolver.delete(any(), any(), any()) } returns 0
            stubMediaStorePresent(rows.map { it.localUri })

            val result = useCase(userId, Long.MAX_VALUE)

            assertTrue(
                "expected NeedsPermission, got $result",
                result is FreeUpSpaceUseCase.FreeUpResult.NeedsPermission,
            )
            val needs = result as FreeUpSpaceUseCase.FreeUpResult.NeedsPermission
            assertEquals(rows.map { it.localUri }, needs.localUris)
            assertTrue(needs.completedUris.isEmpty())
            // Permission survivors must not be marked CLOUD_ONLY yet.
            coVerify(exactly = 0) { repo.updateStatusAndDeleteLocal(any<List<String>>(), any()) }
            coVerify(exactly = 0) { repo.updateStatusAndDeleteLocal(any<String>(), any()) }
        }

    @Test
    fun `missing local URIs are repaired to CLOUD_ONLY without NeedsPermission`() = runTest {
        val rows = (1..3).map { eligible(it) }
        coEvery { repo.getSyncedBefore(userId, any()) } returns rows
        every { resolver.delete(any(), any(), any()) } returns 0
        stubMediaStoreMissing()

        val result = useCase(userId, Long.MAX_VALUE)

        assertEquals(3, (result as FreeUpSpaceUseCase.FreeUpResult.Done).freed)
        assertEquals(rows.map { it.localUri }, result.completedUris)
        coVerify(exactly = 1) {
            repo.updateStatusAndDeleteLocal(rows.map { it.localUri }, SyncStatus.CLOUD_ONLY)
        }
        coVerify {
            freeUpVerifiedDao.deleteForUris(userId.id, rows.map { it.localUri })
        }
    }

    @Test
    fun `direct deletes batch status and clearVerified on the progress step cadence`() = runTest {
        val rows = (1..45).map { eligible(it) }
        stubFinishPath(rows)
        every { resolver.delete(any(), any(), any()) } returns 1
        val batches = mutableListOf<List<String>>()
        coEvery { repo.updateStatusAndDeleteLocal(any<List<String>>(), any()) } coAnswers {
            batches += firstArg<List<String>>()
        }
        val cleared = mutableListOf<List<String>>()
        coEvery { freeUpVerifiedDao.deleteForUris(userId.id, any()) } coAnswers {
            cleared += secondArg<List<String>>()
        }

        useCase.finishReadyForReclaim(userId = userId)

        assertEquals(listOf(20, 20, 5), batches.map { it.size })
        assertEquals(rows.map { it.localUri }, batches.flatten())
        assertEquals(listOf(20, 20, 5), cleared.map { it.size })
        assertEquals(rows.map { it.localUri }.toSet(), cleared.flatten().toSet())
    }

    @Test
    fun `runPipeline automatic scope skips protected downloads while manual reclaims them`() =
        runTest {
            val uploaded = eligible(1, localHash = "hash-1")
            val downloaded = eligible(2, localHash = "")
            val rows = listOf(uploaded, downloaded)
            coEvery { repo.getSyncedBefore(userId, any()) } returns rows
            every { resolver.delete(any(), any(), any()) } returns 1
            coEvery { cloudRepo.getVolumeId(userId) } returns volumeId
            coEvery {
                linkDetailHelpers.batchFetchLinkDetailsStrict(userId, volumeId, any())
            } answers {
                @Suppress("UNCHECKED_CAST")
                val ids = args[2] as List<String>
                ids.associateWith { linkAt(it) }
            }
            val verified = mutableListOf<FreeUpVerifiedEntity>()
            coEvery { freeUpVerifiedDao.upsert(any<List<FreeUpVerifiedEntity>>()) } coAnswers {
                verified += firstArg<List<FreeUpVerifiedEntity>>()
            }
            coEvery { freeUpVerifiedDao.freshLocalUris(any(), any()) } returns emptyList()
            coEvery { freeUpVerifiedDao.readyToReclaim(userId.id, any()) } answers {
                verified.map { entityFor(eligible(it.localUri.substringAfterLast('/').toInt())) }
            }

            useCase.runPipeline(userId, Long.MAX_VALUE, protectDownloaded = true)

            assertEquals(listOf(uploaded.localUri), verified.map { it.localUri })
            coVerify {
                repo.updateStatusAndDeleteLocal(listOf(uploaded.localUri), SyncStatus.CLOUD_ONLY)
            }
            coVerify(exactly = 0) {
                repo.updateStatusAndDeleteLocal(
                    match<List<String>> { downloaded.localUri in it },
                    any(),
                )
            }

            verified.clear()
            useCase.runPipeline(userId, Long.MAX_VALUE, protectDownloaded = false)
            assertEquals(
                setOf(uploaded.localUri, downloaded.localUri),
                verified.map { it.localUri }.toSet(),
            )
        }

    @Test
    fun `runPipeline intersects ready markers with recomputed eligibility so auto skips manual-only`() =
        runTest {
            val uploaded = eligible(1, localHash = "hash-1")
            val downloaded = eligible(2, localHash = "")
            coEvery { repo.getSyncedBefore(userId, any()) } returns listOf(uploaded, downloaded)
            every { resolver.delete(any(), any(), any()) } returns 1
            coEvery { cloudRepo.getVolumeId(userId) } returns volumeId
            coEvery {
                linkDetailHelpers.batchFetchLinkDetailsStrict(userId, volumeId, any())
            } answers {
                @Suppress("UNCHECKED_CAST")
                val ids = args[2] as List<String>
                ids.associateWith { linkAt(it) }
            }
            // Manual previously verified both; automatic must not reclaim the protected download.
            coEvery { freeUpVerifiedDao.freshLocalUris(any(), any()) } returns listOf(
                uploaded.localUri,
                downloaded.localUri,
            )
            coEvery { freeUpVerifiedDao.readyToReclaim(userId.id, any()) } returns listOf(
                entityFor(uploaded),
                entityFor(downloaded),
            )

            useCase.runPipeline(userId, Long.MAX_VALUE, protectDownloaded = true)

            coVerify {
                repo.updateStatusAndDeleteLocal(listOf(uploaded.localUri), SyncStatus.CLOUD_ONLY)
            }
            coVerify(exactly = 0) {
                repo.updateStatusAndDeleteLocal(
                    match<List<String>> { downloaded.localUri in it },
                    any(),
                )
            }
            coVerify(exactly = 0) {
                freeUpVerifiedDao.deleteForUris(
                    userId.id,
                    match { downloaded.localUri in it },
                )
            }
        }

    @Test
    fun `cancel between reclaim progress steps flushes the batch and skips the rest`() = runTest {
        val rows = (1..45).map { eligible(it) }
        stubFinishPath(rows)
        var deletes = 0
        every { resolver.delete(any(), any(), any()) } answers {
            deletes++
            1
        }
        val batches = mutableListOf<List<String>>()
        coEvery { repo.updateStatusAndDeleteLocal(any<List<String>>(), any()) } coAnswers {
            batches += firstArg<List<String>>()
        }

        try {
            useCase.finishReadyForReclaim(
                userId = userId,
                onReclaimProgress = { processed, _, _, _ ->
                    if (processed >= FreeUpSpaceUseCase.PROGRESS_STEP) {
                        throw CancellationException("cancel after first progress batch")
                    }
                },
            )
        } catch (_: CancellationException) {
            // expected — cooperative cancel between progress chunks
        }

        assertEquals(FreeUpSpaceUseCase.PROGRESS_STEP, deletes)
        assertEquals(listOf(FreeUpSpaceUseCase.PROGRESS_STEP), batches.map { it.size })
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
        val failUris = rows.filterIndexed { index, _ -> index % 2 == 1 }.map { it.localUri }
        stubMediaStorePresent(failUris)

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
