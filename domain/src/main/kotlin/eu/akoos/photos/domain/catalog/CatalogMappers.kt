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

/**
 * Pure mapping helpers between catalog field bags and domain records.
 * Data-layer Impls call these so entity → record conversion stays testable without Room.
 */
object CatalogMappers {
    fun personRecord(
        id: Long,
        userId: String,
        displayName: String?,
        coverFaceId: String?,
        faceCount: Int,
        updatedAt: Long,
        isOther: Boolean,
    ) = PersonRecord(id, userId, displayName, coverFaceId, faceCount, updatedAt, isOther)

    fun faceRecord(
        id: String,
        userId: String,
        photoKey: String,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        landmarks: String,
        embedding: ByteArray,
        personId: Long?,
        score: Float,
        blur: Float? = null,
        rejected: Boolean = false,
        manualName: String? = null,
    ) = FaceRecord(
        id, userId, photoKey, left, top, right, bottom, landmarks, embedding,
        personId, score, blur, rejected, manualName,
    )

    fun dayMetaRecord(
        date: String,
        userId: String,
        coverPhotoUri: String?,
        locationText: String?,
        description: String?,
        updatedAt: Long,
    ) = DayMetaRecord(date, userId, coverPhotoUri, locationText, description, updatedAt)

    /**
     * Blank [date] is refused. Blank [locationText] / [description] collapse to null so
     * empty form fields do not persist whitespace-only values.
     */
    fun dayMetaRecordOrNull(
        date: String,
        userId: String,
        coverPhotoUri: String?,
        locationText: String?,
        description: String?,
        updatedAt: Long,
    ): DayMetaRecord? {
        if (date.isBlank()) return null
        return DayMetaRecord(
            date = date,
            userId = userId,
            coverPhotoUri = coverPhotoUri?.takeIf { it.isNotBlank() },
            locationText = locationText?.takeIf { it.isNotBlank() },
            description = description?.takeIf { it.isNotBlank() },
            updatedAt = updatedAt,
        )
    }

    fun importStagedRecord(
        zipId: String,
        entryName: String,
        title: String?,
        dateMs: Long?,
        sizeBytes: Long,
        thumbPath: String?,
        excluded: Boolean,
        alreadyInDrive: Boolean,
        albumName: String?,
    ) = ImportStagedRecord(
        zipId, entryName, title, dateMs, sizeBytes, thumbPath, excluded, alreadyInDrive, albumName,
    )
}
