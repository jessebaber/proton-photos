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

package eu.akoos.photos.data.usecase

import eu.akoos.photos.data.usecase.CloudLinkCheck
import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.akoos.photos.domain.entity.CloudPhoto
import eu.akoos.photos.domain.entity.LocalMediaItem
import eu.akoos.photos.domain.repository.DrivePhotoRepository
import eu.akoos.photos.domain.usecase.CloudLinkCheck
import eu.akoos.photos.domain.usecase.CloudSavePhase
import eu.akoos.photos.domain.usecase.LocationEdit
import eu.akoos.photos.domain.usecase.WriteCloudPhotoMetadataUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import me.proton.core.accountmanager.domain.AccountManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Changes a single CLOUD photo's capture date and/or GPS place. Proton Drive has no metadata-edit or
 * revision API, so the only way to change a cloud photo's metadata is to replace it with a corrected
 * copy: download the original bytes, rewrite only the changed EXIF tags on a working copy (losslessly,
 * never decoding or recompressing pixels), upload the corrected copy as a new link, re-add that new
 * link to every album the original belonged to, and only then trash the original.
 *
 * The ordering mirrors the editor's proven cloud-replacement path. The trash step is the one that can
 * lose data, so it is fenced hard: the original is trashed ONLY after the upload has returned a verified
 * new link AND that new link has joined every album the original was in. If any album re-add fails the
 * original is left in place, which leaves a recoverable duplicate rather than a photo silently dropped
 * from an album.
 *
 * The in-file EXIF write targets EXIF-writable images; the capture date still reaches Drive through the
 * upload item for any container, since Drive stores it in its own capture-time field.
 */
@Singleton
class WriteCloudPhotoMetadataUseCaseImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cloudRepo: DrivePhotoRepository,
    private val accountManager: AccountManager,
    private val exifRewriter: CloudPhotoExifRewriter,
    private val carryOver: CarryOverToReplacement,
    private val linkCheck: CloudLinkCheck,
) : WriteCloudPhotoMetadataUseCase {
    private val replacement = CloudPhotoReplacement(context, cloudRepo, carryOver, linkCheck::isLive)

    override suspend operator fun invoke(
        photo: CloudPhoto,
        newCaptureMs: Long?,
        location: LocationEdit,
        description: String?,
        artist: String?,
        copyright: String?,
        onPhase: ((CloudSavePhase) -> Unit)?,
        resumeUploadedLinkId: String?,
        onUploaded: (suspend (String) -> Unit)?,
    ): WriteCloudPhotoMetadataUseCase.Result = withContext(Dispatchers.IO) {
        if (newCaptureMs == null && location is LocationEdit.Unchanged &&
            description == null && artist == null && copyright == null
        ) {
            return@withContext WriteCloudPhotoMetadataUseCase.Result.NothingToDo
        }
        val userId = accountManager.getPrimaryUserId().first()
            ?: return@withContext WriteCloudPhotoMetadataUseCase.Result.Failed("not signed in")
        onPhase?.invoke(CloudSavePhase.PREPARING)

        // Only the changed EXIF tags are rewritten on the working copy, losslessly; the pixels are never
        // decoded. A place-only change keeps the original capture time, so the copy sorts where it did.
        val prepare = CloudPhotoReplacement.Prepare { file ->
            val effectiveCaptureMs = newCaptureMs ?: photo.captureTimeMs
            val xAttr = exifRewriter.rewrite(
                file = file,
                mimeType = photo.mimeType,
                writeCaptureMs = newCaptureMs,
                xAttrCaptureMs = effectiveCaptureMs,
                location = location,
                description = description,
                artist = artist,
                copyright = copyright,
            )
            // Same displayName: this is a correction, not a copy. Dims and duration come from the xAttr the
            // rewriter derived from the corrected file; the known cloud duration is the fallback.
            CloudPhotoReplacement.Prepared(
                file = file,
                item = LocalMediaItem(
                    uri = Uri.fromFile(file).toString(),
                    dateTaken = effectiveCaptureMs,
                    displayName = photo.displayName,
                    mimeType = photo.mimeType,
                    sizeBytes = file.length(),
                    bucketName = null,
                    width = xAttr.displayWidth ?: 0,
                    height = xAttr.displayHeight ?: 0,
                    duration = xAttr.durationMillis ?: photo.durationMs ?: 0L,
                ),
                xAttr = xAttr,
            )
        }
        when (val r = replacement.replace(userId, photo, prepare, onPhase, resumeUploadedLinkId, onUploaded)) {
            is CloudPhotoReplacement.Result.Success -> WriteCloudPhotoMetadataUseCase.Result.Success
            CloudPhotoReplacement.Result.Skipped -> WriteCloudPhotoMetadataUseCase.Result.NothingToDo
            is CloudPhotoReplacement.Result.Failed -> WriteCloudPhotoMetadataUseCase.Result.Failed(r.reason, r.transient)
        }
    }
}
