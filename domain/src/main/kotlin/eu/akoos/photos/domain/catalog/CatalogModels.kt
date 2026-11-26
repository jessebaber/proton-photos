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

/** Person cluster row for presentation reads. */
data class PersonRecord(
    val id: Long,
    val userId: String,
    val displayName: String? = null,
    val coverFaceId: String? = null,
    val faceCount: Int = 0,
    val updatedAt: Long = 0,
    val isOther: Boolean = false,
)

/** One detected face without Room types. */
data class FaceRecord(
    val id: String,
    val userId: String,
    val photoKey: String,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val landmarks: String,
    val embedding: ByteArray,
    val personId: Long?,
    val score: Float,
    val blur: Float? = null,
    val rejected: Boolean = false,
    val manualName: String? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FaceRecord) return false
        return id == other.id &&
            userId == other.userId &&
            photoKey == other.photoKey &&
            left == other.left &&
            top == other.top &&
            right == other.right &&
            bottom == other.bottom &&
            landmarks == other.landmarks &&
            embedding.contentEquals(other.embedding) &&
            personId == other.personId &&
            score == other.score &&
            blur == other.blur &&
            rejected == other.rejected &&
            manualName == other.manualName
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + userId.hashCode()
        result = 31 * result + photoKey.hashCode()
        result = 31 * result + left.hashCode()
        result = 31 * result + top.hashCode()
        result = 31 * result + right.hashCode()
        result = 31 * result + bottom.hashCode()
        result = 31 * result + landmarks.hashCode()
        result = 31 * result + embedding.contentHashCode()
        result = 31 * result + (personId?.hashCode() ?: 0)
        result = 31 * result + score.hashCode()
        result = 31 * result + (blur?.hashCode() ?: 0)
        result = 31 * result + rejected.hashCode()
        result = 31 * result + (manualName?.hashCode() ?: 0)
        return result
    }
}

data class FaceBoxRecord(
    val faceId: String,
    val photoKey: String,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val personId: Long?,
)

data class NotPersonRecord(
    val userId: String,
    val personName: String,
    val faceId: String,
)

data class PhotoLocationRecord(
    val id: String,
    val userId: String,
    val latitude: Double,
    val longitude: Double,
)

data class DayMetaRecord(
    val date: String,
    val userId: String,
    val coverPhotoUri: String? = null,
    val locationText: String? = null,
    val description: String? = null,
    val updatedAt: Long = 0L,
)

data class CloudAlbumRecord(
    val linkId: String,
    val name: String,
    val photoCount: Int,
    val coverLinkId: String? = null,
    val lastActivityTimeMs: Long? = null,
    val sharingShareId: String? = null,
    val volumeId: String? = null,
    val sharedByEmail: String? = null,
)

data class ImportHistoryRecord(
    val id: Long,
    val zipId: String,
    val fileName: String,
    val importedAt: Long,
    val total: Int,
    val uploaded: Int,
    val skipped: Int,
    val failed: Int,
    val runId: String? = null,
)

data class ImportStagedRecord(
    val zipId: String,
    val entryName: String,
    val title: String? = null,
    val dateMs: Long? = null,
    val sizeBytes: Long = 0,
    val thumbPath: String? = null,
    val excluded: Boolean = false,
    val alreadyInDrive: Boolean = false,
    val albumName: String? = null,
)

data class ImportUploadedRecord(
    val runId: String,
    val linkId: String,
    val name: String? = null,
    val alreadyInDrive: Boolean = false,
)

data class ImportAlbumCountRecord(
    val albumName: String,
    val count: Int,
)

data class SyncStateLiteRecord(
    val localUri: String,
    val cloudFileId: String?,
    val status: String,
)

data class PerceptualHashLiteRecord(
    val key: String,
    val h0: Long,
    val h1: Long,
    val h2: Long,
    val h3: Long,
    val quality: Int,
    val freshness: String,
)

data class PhotoPickerRowRecord(
    val linkId: String,
    val thumbnailUrl: String?,
)

data class AlbumMembershipRecord(
    val albumLinkId: String,
    val photoLinkId: String,
)

