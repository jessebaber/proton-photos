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

import android.content.Context
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
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.UnknownHostException

/**
 * Pins chunked [FreeUpSpaceUseCase.verifyForReclaim] / [FreeUpSpaceUseCase.readyToReclaim]:
 * successful chunks persist and appear in the ready set; resume skips fresh verified rows; a failed
 * or offline chunk never marks those URIs ready.
 */
class FreeUpSpaceVerifyTest {

    private val userId = UserId("u1")
    private val volumeId = "vol-1"
    private lateinit var cloudRepo: DrivePhotoRepository
    private lateinit var linkDetailHelpers: LinkDetailHelpers
    private lateinit var freeUpVerifiedDao: FreeUpVerifiedDao
    private lateinit var useCase: FreeUpSpaceUseCase

    private val persisted = mutableListOf<FreeUpVerifiedEntity>()

    private fun candidate(linkId: String?) = SyncState(
        localUri = "content://media/external/images/media/${linkId ?: "none"}",
        cloudFileId = linkId,
        localHash = "hash-${linkId ?: "none"}",
        cloudHash = null,
        status = SyncStatus.SYNCED,
        lastSyncAttemptMs = 0L,
        lastSyncSuccessMs = 1L,
        backedUpAtMs = 1L,
        sizeBytes = 1024L,
    )

    private fun linkAt(linkId: String, state: Int) =
        BatchLinkDto(link = LinkCoreDto(linkId = linkId, type = 2, state = state))

    private fun entityFor(row: FreeUpVerifiedEntity) = SyncStateEntity(
        localUri = row.localUri,
        userId = row.userId,
        cloudFileId = row.cloudFileId,
        localHash = "hash",
        cloudHash = null,
        status = SyncStatus.SYNCED,
        lastSyncAttemptMs = 0L,
        lastSyncSuccessMs = 1L,
        backedUpAtMs = 1L,
        sizeBytes = 1024L,
    )

    @Before
    fun setUp() {
        persisted.clear()
        cloudRepo = mockk(relaxed = true)
        linkDetailHelpers = mockk(relaxed = true)
        freeUpVerifiedDao = mockk(relaxed = true)
        val context = mockk<Context>(relaxed = true)
        val syncStateRepo = mockk<SyncStateRepository>(relaxed = true)
        useCase = FreeUpSpaceUseCase(
            context,
            syncStateRepo,
            cloudRepo,
            linkDetailHelpers,
            freeUpVerifiedDao,
        )
        coEvery { cloudRepo.getVolumeId(userId) } returns volumeId
        coEvery { freeUpVerifiedDao.upsert(any<FreeUpVerifiedEntity>()) } coAnswers {
            persisted += firstArg<FreeUpVerifiedEntity>()
        }
        coEvery { freeUpVerifiedDao.upsert(any<List<FreeUpVerifiedEntity>>()) } coAnswers {
            persisted += firstArg<List<FreeUpVerifiedEntity>>()
        }
        coEvery { freeUpVerifiedDao.freshLocalUris(any(), any()) } answers {
            val since = secondArg<Long>()
            persisted.filter { it.userId == userId.id && it.verifiedAtMs >= since }.map { it.localUri }
        }
        coEvery { freeUpVerifiedDao.readyToReclaim(userId.id, any()) } answers {
            val since = secondArg<Long>()
            persisted
                .filter { it.userId == userId.id && it.verifiedAtMs >= since }
                .map { entityFor(it) }
        }
    }

    @Test
    fun `after a successful chunk those URIs appear in readyToReclaim`() = runTest {
        val row = candidate("cloud-1")
        coEvery {
            linkDetailHelpers.batchFetchLinkDetailsStrict(userId, volumeId, any())
        } returns mapOf("cloud-1" to linkAt("cloud-1", state = 1))

        useCase.verifyForReclaim(userId, listOf(row))
        val ready = useCase.readyToReclaim(userId)

        assertEquals(listOf(row.localUri), ready.map { it.localUri })
        assertTrue("successful chunk must persist a verified row", persisted.isNotEmpty())
    }

    @Test
    fun `resume skips candidates already freshly verified`() = runTest {
        val row = candidate("cloud-1")
        persisted += FreeUpVerifiedEntity(
            userId = userId.id,
            localUri = row.localUri,
            cloudFileId = "cloud-1",
            verifiedAtMs = System.currentTimeMillis(),
        )

        useCase.verifyForReclaim(userId, listOf(row))

        coVerify(exactly = 0) {
            linkDetailHelpers.batchFetchLinkDetailsStrict(any(), any(), any())
        }
    }

    @Test
    fun `a failed chunk never marks those URIs ready`() = runTest {
        val rows = listOf(candidate("a"), candidate("b"))
        coEvery {
            linkDetailHelpers.batchFetchLinkDetailsStrict(userId, volumeId, any())
        } throws RuntimeException("chunk failed")

        runCatching { useCase.verifyForReclaim(userId, rows) }
        val ready = useCase.readyToReclaim(userId)

        assertTrue("failed chunk must not persist ready rows", ready.isEmpty())
        assertTrue(persisted.isEmpty())
    }

    @Test
    fun `offline lookup throws so the worker can retry`() = runTest {
        coEvery {
            linkDetailHelpers.batchFetchLinkDetailsStrict(userId, volumeId, any())
        } throws UnknownHostException("no route")

        val propagated: Throwable? = try {
            useCase.verifyForReclaim(userId, listOf(candidate("cloud-1")))
            null
        } catch (e: UnknownHostException) {
            e
        }

        assertNotNull("offline must propagate for WorkManager retry", propagated)
        assertTrue(persisted.isEmpty())
    }

    @Test
    fun `trashed links are not persisted as ready`() = runTest {
        coEvery {
            linkDetailHelpers.batchFetchLinkDetailsStrict(userId, volumeId, any())
        } returns mapOf("cloud-1" to linkAt("cloud-1", state = 2))

        useCase.verifyForReclaim(userId, listOf(candidate("cloud-1")))
        val ready = useCase.readyToReclaim(userId)

        assertTrue(ready.isEmpty())
        assertTrue(persisted.isEmpty())
    }
}
