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

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import eu.akoos.photos.data.db.entity.FreeUpVerifiedEntity
import eu.akoos.photos.data.db.entity.SyncStateEntity

/**
 * Persisted Drive-verify markers for free-up. Stub DAO for Phase 6 contract tests; wired into
 * AppDatabase in Phase 7.
 */
@Dao
interface FreeUpVerifiedDao {

    @Upsert
    suspend fun upsert(row: FreeUpVerifiedEntity)

    @Upsert
    suspend fun upsert(rows: List<FreeUpVerifiedEntity>)

    /** Local URIs already verified at or after [sinceMs] for [userId], so a resume can skip them. */
    @Query(
        "SELECT localUri FROM free_up_verified WHERE userId = :userId AND verifiedAtMs >= :sinceMs",
    )
    suspend fun freshLocalUris(userId: String, sinceMs: Long): List<String>

    @Query("DELETE FROM free_up_verified WHERE userId = :userId AND localUri IN (:uris)")
    suspend fun deleteForUris(userId: String, uris: List<String>)

    @Query("DELETE FROM free_up_verified WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)

    /**
     * SYNCED sync_state rows whose verified marker still matches cloudFileId and is fresh.
     * The join encodes [matchesFreeUpVerifiedJoin].
     */
    @Query(
        """
        SELECT s.* FROM sync_state s
        INNER JOIN free_up_verified v
          ON v.localUri = s.localUri AND v.userId = s.userId
        WHERE s.userId = :userId
          AND s.status = 'SYNCED'
          AND s.cloudFileId IS NOT NULL
          AND s.cloudFileId = v.cloudFileId
          AND v.verifiedAtMs >= :sinceMs
        """,
    )
    suspend fun readyToReclaim(userId: String, sinceMs: Long): List<SyncStateEntity>

    /**
     * Same join predicates as [readyToReclaim], returning only the row count (WorkInfo observer).
     */
    @Query(
        """
        SELECT COUNT(*) FROM sync_state s
        INNER JOIN free_up_verified v
          ON v.localUri = s.localUri AND v.userId = s.userId
        WHERE s.userId = :userId
          AND s.status = 'SYNCED'
          AND s.cloudFileId IS NOT NULL
          AND s.cloudFileId = v.cloudFileId
          AND v.verifiedAtMs >= :sinceMs
        """,
    )
    suspend fun readyToReclaimCount(userId: String, sinceMs: Long): Int
}
