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

package eu.akoos.photos.domain.label

import eu.akoos.photos.domain.entity.GalleryItem
import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.entity.UserId

data class LabelUsage(val labelKey: String, val label: String, val count: Int)

data class LabelBulkResult(val changedPhotos: Int, val cappedPhotos: Int)

sealed interface LabelCreation {
    data class Created(val label: String) : LabelCreation
    data class Exists(val label: String) : LabelCreation
    data object Invalid : LabelCreation
}

/** User photo labels repository presentation reads and writes. */
interface PhotoLabels {
    suspend fun currentUserKey(): String
    fun observeUserKey(): Flow<String>
    fun observeLabelsByPhoto(): Flow<Map<String, List<String>>>
    fun observeLabelUsage(): Flow<List<LabelUsage>>
    fun observeLocked(): Flow<Boolean>
    suspend fun createLabel(text: String): LabelCreation
    suspend fun photoKeysOfLabel(label: String): Set<String>
    fun observeLabelsOf(photoKey: String): Flow<List<String>>
    suspend fun labelsFor(item: GalleryItem): List<String>
    suspend fun setLabels(item: GalleryItem, labels: List<String>)
    suspend fun setLabels(photoKey: String, labels: List<String>)
    suspend fun addLabels(photoKeys: Collection<String>, labels: List<String>): LabelBulkResult
    suspend fun removeLabel(photoKey: String, label: String): Boolean
    suspend fun renameLabel(from: String, to: String): Boolean
    suspend fun deleteLabels(labels: Collection<String>): Int
    suspend fun deleteLabel(label: String): Int
    suspend fun rekey(oldKey: String, newKey: String): Int
    suspend fun deleteForPhotos(photoKeys: Collection<String>): Int
    suspend fun clearForUser(userId: UserId): Int
    suspend fun adoptGuestLabels(accountId: String): Int
}
