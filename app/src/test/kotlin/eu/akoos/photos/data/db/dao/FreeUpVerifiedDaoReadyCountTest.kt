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

package eu.akoos.photos.data.db.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import eu.akoos.photos.data.db.AppDatabase
import eu.akoos.photos.data.db.entity.FreeUpVerifiedEntity
import eu.akoos.photos.data.db.entity.SyncStateEntity
import eu.akoos.photos.domain.entity.SyncStatus
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Pins [FreeUpVerifiedDao.readyToReclaimCount] against the same join as [FreeUpVerifiedDao.readyToReclaim].
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FreeUpVerifiedDaoReadyCountTest {

    private lateinit var db: AppDatabase
    private lateinit var freeUpVerifiedDao: FreeUpVerifiedDao
    private lateinit var syncStateDao: SyncStateDao

    private val userId = "u1"
    private val now = 1_000_000L
    private val sinceMs = now - 24L * 60L * 60L * 1000L

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor { it.run() }
            .build()
        freeUpVerifiedDao = db.freeUpVerifiedDao()
        syncStateDao = db.syncStateDao()
    }

    @After
    fun tearDown() = db.close()

    private fun sync(
        uri: String,
        cloudFileId: String?,
        status: SyncStatus = SyncStatus.SYNCED,
        user: String = userId,
    ) = SyncStateEntity(
        localUri = uri,
        userId = user,
        cloudFileId = cloudFileId,
        localHash = "hash",
        cloudHash = null,
        status = status,
        lastSyncAttemptMs = 0L,
        lastSyncSuccessMs = 1L,
        backedUpAtMs = 1L,
        sizeBytes = 1024L,
    )

    private fun verified(
        uri: String,
        cloudFileId: String,
        verifiedAtMs: Long = now,
        user: String = userId,
    ) = FreeUpVerifiedEntity(
        userId = user,
        localUri = uri,
        cloudFileId = cloudFileId,
        verifiedAtMs = verifiedAtMs,
    )

    @Test
    fun `readyToReclaimCount matches readyToReclaim size on fixture rows`() = runTest {
        syncStateDao.upsert(sync("content://1", "cloud-1"))
        syncStateDao.upsert(sync("content://2", "cloud-2"))
        syncStateDao.upsert(sync("content://3", "cloud-3", status = SyncStatus.CLOUD_ONLY))
        syncStateDao.upsert(sync("content://4", "cloud-old"))
        syncStateDao.upsert(sync("content://5", "cloud-5", user = "u2"))
        freeUpVerifiedDao.upsert(
            listOf(
                verified("content://1", "cloud-1"),
                verified("content://2", "cloud-2"),
                verified("content://3", "cloud-3"),
                verified("content://4", "cloud-new"),
                verified("content://5", "cloud-5", user = "u2"),
                verified("content://expired", "cloud-x", verifiedAtMs = sinceMs - 1L),
            ),
        )
        syncStateDao.upsert(sync("content://expired", "cloud-x"))

        val ready = freeUpVerifiedDao.readyToReclaim(userId, sinceMs)
        val count = freeUpVerifiedDao.readyToReclaimCount(userId, sinceMs)

        assertEquals(listOf("content://1", "content://2"), ready.map { it.localUri }.sorted())
        assertEquals(ready.size, count)
        assertEquals(2, count)
    }
}
