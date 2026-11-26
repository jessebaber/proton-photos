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

package eu.akoos.photos.domain.catalog

import kotlinx.coroutines.flow.Flow

/** Takeout import staging, history, and uploaded-row catalog. */
interface ImportCatalog {
    fun observeHistory(): Flow<List<ImportHistoryRecord>>
    suspend fun deleteHistoryById(id: Long)

    fun observeStagedForZip(zipUri: String): Flow<List<ImportStagedRecord>>
    fun observeNewCount(zipUri: String): Flow<Int>
    fun observeHasAlbums(zipUri: String): Flow<Boolean>
    fun observeAlreadyCount(zipUri: String): Flow<Int>
    fun observeAlbumSummary(zipUri: String): Flow<List<ImportAlbumCountRecord>>
    fun observeIncludedCount(zipUri: String): Flow<Int>
    suspend fun setExcluded(zipUri: String, entryNames: List<String>, excluded: Boolean)
    suspend fun thumbPaths(zipUri: String): List<String>
    suspend fun clearForZip(zipUri: String)
    suspend fun zipsWithPending(): List<String>

    fun observeUploadedByRun(runId: String): Flow<List<ImportUploadedRecord>>
    suspend fun deleteUploadedRun(runId: String)
    suspend fun albumCountsForRun(runId: String): List<ImportAlbumCountRecord>
    suspend fun clearAlbumMembersForRun(runId: String)

    /** Drop the on-disk takeout scan cache for [zipUri], if any. */
    suspend fun clearScanCache(zipUri: String)
}
