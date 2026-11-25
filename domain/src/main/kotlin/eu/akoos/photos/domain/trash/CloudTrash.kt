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

package eu.akoos.photos.domain.trash

import eu.akoos.photos.domain.entity.CloudPhoto
import eu.akoos.photos.domain.entity.CloudTrashItem
import kotlinx.coroutines.flow.SharedFlow
import me.proton.core.domain.entity.UserId

data class CloudRestoreOutcome(
    val restoredLinkIds: Set<String>,
    val failedLinkIds: Set<String>,
    val galleryRefreshFailed: Boolean,
)

data class CloudDeleteOutcome(
    val deletedLinkIds: Set<String>,
    val failedLinkIds: Set<String>,
)

data class CloudTrashOutcome(
    val trashedLinkIds: Set<String>,
    val failedLinkIds: Set<String>,
)

/** Drive trash, favorites, tags, rename and copy operations presentation drives. */
interface CloudTrash {
    val trashCountChanges: SharedFlow<Int>

    suspend fun deleteFiles(userId: UserId, linkIds: List<String>): CloudTrashOutcome
    suspend fun setCloudFavorite(userId: UserId, photo: CloudPhoto, favorite: Boolean): Boolean
    suspend fun setCloudTag(userId: UserId, photo: CloudPhoto, tagId: Int, add: Boolean): Boolean
    suspend fun getCloudTrash(userId: UserId): List<CloudTrashItem>
    suspend fun restoreFromCloudTrash(userId: UserId, linkIds: List<String>): CloudRestoreOutcome
    suspend fun deleteFromCloudForever(userId: UserId, linkIds: List<String>): CloudDeleteOutcome
    suspend fun renameCloudPhoto(userId: UserId, photo: CloudPhoto, newName: String)
    suspend fun copyCloudPhotoAs(
        userId: UserId,
        photo: CloudPhoto,
        newName: String,
    ): String
}
