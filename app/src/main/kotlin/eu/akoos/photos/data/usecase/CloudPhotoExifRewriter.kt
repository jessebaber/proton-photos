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

import eu.akoos.photos.data.repository.drive.UploadXAttrMetadata
import eu.akoos.photos.domain.usecase.LocationEdit
import java.io.File

/**
 * Rewrites the changed EXIF onto a downloaded original in place and reports the photo xAttr that
 * describes the result. The write is LOSSLESS: only the addressed tags change, every pixel and every
 * untouched tag stays byte-for-byte. Isolated behind an interface so the orchestration in
 * [WriteCloudPhotoMetadataUseCase] can be unit-tested with a stub, while the real Android EXIF work
 * lives in [CloudPhotoExifRewriterImpl].
 */
interface CloudPhotoExifRewriter {

    /**
     * Applies the edit to [file] in place and returns the xAttr for the corrected copy. [writeCaptureMs]
     * (when non-null) rewrites the datetime tags; [location] sets, clears, or leaves the GPS block; each
     * of [description] / [artist] / [copyright] (when non-null) writes that descriptive text tag, with an
     * empty string clearing it. [xAttrCaptureMs] is the capture time stamped into the returned xAttr (the
     * new time, or the original when only the place changed). The returned xAttr carries the ORIGINAL
     * orientation, dimensions, camera and subject-area so a correction never re-orients or re-sizes the
     * photo.
     */
    fun rewrite(
        file: File,
        mimeType: String,
        writeCaptureMs: Long?,
        xAttrCaptureMs: Long,
        location: LocationEdit,
        description: String?,
        artist: String?,
        copyright: String?,
    ): UploadXAttrMetadata

    /**
     * Builds the photo xAttr for [uri] WITHOUT touching a single byte: reads the source orientation,
     * dimensions, camera and subject-area and folds in [xAttrCaptureMs] and [location], exactly the
     * read-only half of [rewrite]. Serves the Synced path, where the device file was already
     * EXIF-edited in place by the local save, so only the xAttr for the replacement upload has to be
     * derived here. [uri] may be a content or a file uri.
     */
    fun xAttrFor(
        uri: String,
        mimeType: String,
        xAttrCaptureMs: Long,
        location: LocationEdit,
    ): UploadXAttrMetadata
}
