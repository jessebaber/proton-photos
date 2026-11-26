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

package eu.akoos.photos.data.catalog

import eu.akoos.photos.data.db.dao.ImportAlbumMemberDao
import eu.akoos.photos.data.importer.ImportScanCache
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import eu.akoos.photos.data.db.dao.ImportHistoryDao
import eu.akoos.photos.data.db.dao.ImportStagedDao
import eu.akoos.photos.data.db.dao.ImportUploadedDao
import eu.akoos.photos.domain.catalog.CatalogMappers
import eu.akoos.photos.domain.catalog.ImportAlbumCountRecord
import eu.akoos.photos.domain.catalog.ImportCatalog
import eu.akoos.photos.domain.catalog.ImportHistoryRecord
import eu.akoos.photos.domain.catalog.ImportStagedRecord
import eu.akoos.photos.domain.catalog.ImportUploadedRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImportCatalogImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val importHistoryDao: ImportHistoryDao,
    private val importStagedDao: ImportStagedDao,
    private val importUploadedDao: ImportUploadedDao,
    private val importAlbumMemberDao: ImportAlbumMemberDao,
) : ImportCatalog {
    override fun observeHistory(): Flow<List<ImportHistoryRecord>> =
        importHistoryDao.observeAll().map { list ->
            list.map {
                ImportHistoryRecord(
                    it.id, it.zipId, it.fileName, it.importedAt,
                    it.total, it.uploaded, it.skipped, it.failed, it.runId,
                )
            }
        }

    override suspend fun deleteHistoryById(id: Long) = importHistoryDao.deleteById(id)

    override fun observeStagedForZip(zipUri: String): Flow<List<ImportStagedRecord>> =
        importStagedDao.observeForZip(zipUri).map { list ->
            list.map {
                CatalogMappers.importStagedRecord(
                    it.zipId, it.entryName, it.title, it.dateMs, it.sizeBytes,
                    it.thumbPath, it.excluded, it.alreadyInDrive, it.albumName,
                    lat = it.lat, lng = it.lng, description = it.description,
                )
            }
        }

    override fun observeNewCount(zipUri: String): Flow<Int> = importStagedDao.observeNewCount(zipUri)
    override fun observeHasAlbums(zipUri: String): Flow<Boolean> = importStagedDao.observeHasAlbums(zipUri)
    override fun observeAlreadyCount(zipUri: String): Flow<Int> = importStagedDao.observeAlreadyCount(zipUri)
    override fun observeAlbumSummary(zipUri: String): Flow<List<ImportAlbumCountRecord>> =
        importStagedDao.observeAlbumSummary(zipUri).map { list ->
            list.map { ImportAlbumCountRecord(it.name, it.count) }
        }
    override fun observeIncludedCount(zipUri: String): Flow<Int> = importStagedDao.observeIncludedCount(zipUri)
    override suspend fun setExcluded(zipUri: String, entryNames: List<String>, excluded: Boolean) =
        importStagedDao.setExcluded(zipUri, entryNames, excluded)
    override suspend fun thumbPaths(zipUri: String): List<String> =
        importStagedDao.thumbPaths(zipUri).filterNotNull()
    override suspend fun clearForZip(zipUri: String) = importStagedDao.clearForZip(zipUri)
    override suspend fun zipsWithPending(): List<String> = importStagedDao.zipsWithPending()

    override fun observeUploadedByRun(runId: String): Flow<List<ImportUploadedRecord>> =
        importUploadedDao.observeByRun(runId).map { list ->
            list.map { ImportUploadedRecord(it.runId, it.linkId, it.name, it.alreadyInDrive, it.undone) }
        }
    override suspend fun deleteUploadedRun(runId: String) = importUploadedDao.deleteRun(runId)
    override suspend fun albumCountsForRun(runId: String): List<ImportAlbumCountRecord> =
        importAlbumMemberDao.albumCountsForRun(runId).map { ImportAlbumCountRecord(it.name, it.count) }
    override suspend fun clearAlbumMembersForRun(runId: String) = importAlbumMemberDao.clearForRun(runId)

    override suspend fun clearScanCache(zipUri: String) {
        ImportScanCache(File(context.cacheDir, ImportScanCache.DIR_NAME)).delete(zipUri)
    }
}
