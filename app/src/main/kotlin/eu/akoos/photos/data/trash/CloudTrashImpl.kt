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

package eu.akoos.photos.data.trash

import eu.akoos.photos.data.repository.drive.CloudTrashService
import eu.akoos.photos.domain.entity.CloudPhoto
import eu.akoos.photos.domain.entity.CloudTrashItem
import eu.akoos.photos.domain.trash.CloudDeleteOutcome
import eu.akoos.photos.domain.trash.CloudRestoreOutcome
import eu.akoos.photos.domain.trash.CloudTrash
import eu.akoos.photos.domain.trash.CloudTrashOutcome
import kotlinx.coroutines.flow.SharedFlow
import me.proton.core.domain.entity.UserId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudTrashImpl @Inject constructor(
    private val service: CloudTrashService,
) : CloudTrash {
    override val trashCountChanges: SharedFlow<Int> = service.trashCountChanges

    override suspend fun deleteFiles(userId: UserId, linkIds: List<String>): CloudTrashOutcome {
        val o = service.deleteFiles(userId, linkIds)
        return CloudTrashOutcome(o.trashedLinkIds, o.failedLinkIds)
    }

    override suspend fun setCloudFavorite(userId: UserId, photo: CloudPhoto, favorite: Boolean): Boolean =
        service.setCloudFavorite(userId, photo, favorite)

    override suspend fun setCloudTag(userId: UserId, photo: CloudPhoto, tagId: Int, add: Boolean): Boolean =
        service.setCloudTag(userId, photo, tagId, add)

    override suspend fun getCloudTrash(userId: UserId): List<CloudTrashItem> = service.getCloudTrash(userId)

    override suspend fun restoreFromCloudTrash(userId: UserId, linkIds: List<String>): CloudRestoreOutcome {
        val o = service.restoreFromCloudTrash(userId, linkIds)
        return CloudRestoreOutcome(o.restoredLinkIds, o.failedLinkIds, o.galleryRefreshFailed)
    }

    override suspend fun deleteFromCloudForever(userId: UserId, linkIds: List<String>): CloudDeleteOutcome {
        val o = service.deleteFromCloudForever(userId, linkIds)
        return CloudDeleteOutcome(o.deletedLinkIds, o.failedLinkIds)
    }

    override suspend fun renameCloudPhoto(userId: UserId, photo: CloudPhoto, newName: String) =
        service.renameCloudPhoto(userId, photo, newName)

    override suspend fun copyCloudPhotoAs(userId: UserId, photo: CloudPhoto, newName: String): String =
        service.copyCloudPhotoAs(userId, photo, newName)
}
