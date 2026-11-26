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

/** Photo listing, tags, perceptual hash, embeddings, face-scan reads. */
interface PhotoCatalog {
    fun observeTagsCsv(linkId: String): Flow<String?>
    suspend fun getTagsCsv(linkId: String): String?
    fun observeOwnStreamPickerRows(userId: String): Flow<List<PhotoPickerRowRecord>>

    suspend fun setUserTagsCsv(uri: String, csv: String)
    suspend fun getUserTagsCsv(uri: String): String?

    fun observePerceptualHashes(algo: Int): Flow<List<PerceptualHashLiteRecord>>
    suspend fun embeddingCount(userId: String): Int
    fun observeEmbeddingCount(userId: String): Flow<Int>
    fun observeOwnStreamTotals(userId: String): Flow<OwnStreamTotalsRecord>
    suspend fun hasFaceScans(userId: String): Boolean
    suspend fun clearFaceScans(userId: String)
    suspend fun clearEmbeddings(userId: String)

    suspend fun getThumbnailUrlSeed(userId: String): List<PhotoPickerRowRecord>
    suspend fun clearCachedThumbnailUrls()
}
