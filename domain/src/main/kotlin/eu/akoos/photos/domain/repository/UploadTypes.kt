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

package eu.akoos.photos.domain.repository

/**
 * Phase reported via uploadFile's onProgress callback. Each phase emits its own 0 →
 * totalBytes curve with a guaranteed final 100% tick so the bar settles cleanly.
 */
enum class UploadPhase { Encrypting, Uploading }

/**
 * Source-derived metadata for the photo xAttr Camera/Location blocks, resolved BEFORE any strip
 * pass and already gated against the user's strip config so the encrypted xAttr can never re-leak
 * a field the file itself had erased. All fields are nullable — a null block is simply omitted
 * from the xAttr JSON.
 *
 *  - [latitude]/[longitude]: present only when GPS is NOT stripped.
 *  - [cameraOrientation]: raw EXIF orientation int (1..8), always present when known (never stripped).
 *  - [cameraCaptureTimeIso]: ISO_INSTANT capture time — the floored upload time when timestamps
 *    are stripped, else the real EXIF/MediaStore capture time.
 *  - [cameraDevice]: EXIF model, present only when camera info is NOT stripped.
 *  - [subjectCoordinates]: [Top,Left,Bottom,Right] from EXIF SubjectArea, present only when camera
 *    info is NOT stripped.
 *  - [displayWidth]/[displayHeight]: rotation-corrected dimensions for xAttr Media — already
 *    W/H-swapped when the EXIF orientation or video rotation is 90/270. Null falls back to the
 *    MediaStore values on the local media item.
 *  - [durationMillis]: video length for xAttr Media, read from the source container. Null falls back
 *    to the local media item duration.
 */
data class UploadXAttrMetadata(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val cameraOrientation: Int? = null,
    val cameraCaptureTimeIso: String? = null,
    val cameraDevice: String? = null,
    val subjectCoordinates: IntArray? = null,
    val displayWidth: Int? = null,
    val displayHeight: Int? = null,
    val durationMillis: Long? = null,
)

/** Per-link result of a restore-from-trash batch. [galleryRefreshFailed] is true when the restore
 *  itself succeeded but the follow-up stream refresh couldn't run. */
data class CloudRestoreOutcome(
    val restoredLinkIds: Set<String>,
    val failedLinkIds: Set<String>,
    val galleryRefreshFailed: Boolean,
)

/** Per-link result of a permanent-delete batch. */
data class CloudDeleteOutcome(
    val deletedLinkIds: Set<String>,
    val failedLinkIds: Set<String>,
)

/** Per-link result of a move-to-trash batch. */
data class CloudTrashOutcome(
    val trashedLinkIds: Set<String>,
    val failedLinkIds: Set<String>,
)
