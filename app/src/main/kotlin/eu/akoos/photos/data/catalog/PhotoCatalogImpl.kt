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

import eu.akoos.photos.data.db.dao.FaceScanDao
import eu.akoos.photos.data.db.dao.ImageEmbeddingDao
import eu.akoos.photos.data.db.dao.LocalTagDao
import eu.akoos.photos.data.db.dao.PerceptualHashDao
import eu.akoos.photos.data.db.dao.PhotoListingDao
import eu.akoos.photos.domain.catalog.PerceptualHashLiteRecord
import eu.akoos.photos.domain.catalog.PhotoCatalog
import eu.akoos.photos.domain.catalog.OwnStreamTotalsRecord
import eu.akoos.photos.domain.catalog.PhotoPickerRowRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhotoCatalogImpl @Inject constructor(
    private val photoListingDao: PhotoListingDao,
    private val localTagDao: LocalTagDao,
    private val perceptualHashDao: PerceptualHashDao,
    private val imageEmbeddingDao: ImageEmbeddingDao,
    private val faceScanDao: FaceScanDao,
) : PhotoCatalog {
    override fun observeTagsCsv(linkId: String): Flow<String?> = photoListingDao.observeTagsCsv(linkId)
    override suspend fun getTagsCsv(linkId: String): String? = photoListingDao.getTagsCsv(linkId)
    override fun observeOwnStreamPickerRows(userId: String): Flow<List<PhotoPickerRowRecord>> =
        photoListingDao.observeOwnStreamPickerRows(userId).map { rows ->
            rows.map { PhotoPickerRowRecord(it.linkId, it.thumbnailUrl) }
        }

    override suspend fun setUserTagsCsv(uri: String, csv: String) = localTagDao.setUserTagsCsv(uri, csv)
    override suspend fun getUserTagsCsv(uri: String): String? = localTagDao.getUserTagsCsv(uri)

    override fun observePerceptualHashes(algo: Int): Flow<List<PerceptualHashLiteRecord>> =
        perceptualHashDao.observeLite(algo).map { list ->
            list.map {
                PerceptualHashLiteRecord(it.key, it.h0, it.h1, it.h2, it.h3, it.quality, it.freshness)
            }
        }

    override suspend fun embeddingCount(userId: String): Int = imageEmbeddingDao.countForUser(userId)
    override fun observeEmbeddingCount(userId: String): Flow<Int> =
        imageEmbeddingDao.observeCountForUser(userId)
    override fun observeOwnStreamTotals(userId: String): Flow<OwnStreamTotalsRecord> =
        photoListingDao.observeOwnStreamTotals(userId).map {
            OwnStreamTotalsRecord(it.photos, it.videos, it.bytes)
        }
    override suspend fun hasFaceScans(userId: String): Boolean = faceScanDao.hasAnyForUser(userId)
    override suspend fun clearFaceScans(userId: String) = faceScanDao.clearForUser(userId)
    override suspend fun clearEmbeddings(userId: String) = imageEmbeddingDao.deleteAllForUser(userId)

    override suspend fun getThumbnailUrlSeed(userId: String): List<PhotoPickerRowRecord> =
        photoListingDao.getThumbnailUrlSeed(userId).map { PhotoPickerRowRecord(it.linkId, it.thumbnailUrl) }

    override suspend fun clearCachedThumbnailUrls() = photoListingDao.clearCachedThumbnailUrls()
}
