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

import eu.akoos.photos.domain.entity.CloudPhoto
import eu.akoos.photos.domain.entity.GalleryItem
import me.proton.core.domain.entity.UserId

/**
 * Downloads cloud photos onto the device MediaStore.
 */
interface DownloadPhotosUseCase {
    data class Progress(val done: Int, val total: Int, val failed: Int, val skipped: Int = 0)

    suspend fun downloadCloudPhotos(
        userId: UserId,
        photos: List<CloudPhoto>,
        folderName: String,
        folderByLinkId: Map<String, String> = emptyMap(),
        onSaved: (String) -> Unit = {},
        onProgress: suspend (Progress) -> Unit = {},
    ): Progress

    suspend fun downloadGalleryItems(
        userId: UserId,
        items: List<GalleryItem>,
        folderName: String = "",
        folderByLinkId: Map<String, String> = emptyMap(),
        onSaved: (String) -> Unit = {},
        onProgress: suspend (Progress) -> Unit = {},
    ): Progress

    companion object {
        fun downloadTimestampMs(captureTimeSeconds: Long, fileLastModifiedMs: Long): Long =
            if (captureTimeSeconds > 0L) captureTimeSeconds * 1000L else fileLastModifiedMs

        fun dateTakenColumnMs(captureTimeSeconds: Long): Long? =
            if (captureTimeSeconds > 0L) captureTimeSeconds * 1000L else null

        fun shouldStampExifDate(mimeType: String, captureTimeSeconds: Long): Boolean =
            captureTimeSeconds > 0L && WriteLocalPhotoMetadataUseCase.isExifWritableImageMime(mimeType)

        fun shouldWriteExifGps(mimeType: String, hasCoordinates: Boolean): Boolean =
            hasCoordinates && WriteLocalPhotoMetadataUseCase.isExifWritableImageMime(mimeType)
    }
}
