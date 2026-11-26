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

import kotlinx.coroutines.flow.SharedFlow
import me.proton.core.domain.entity.UserId
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Uploads pending local photos to Drive. Presentation observes [progress] and can ask for a manual
 * run or a graceful stop; the worker drives [invoke].
 */
interface UploadPendingUseCase {
    /**
     * Hot stream of per-file upload events. Buffered so a slow collector never throttles the
     * upload loop. Replay = 1 so a UI that connects mid-batch sees the latest event immediately.
     */
    val progress: SharedFlow<UploadProgress>

    /**
     * Mark the next batch as user-requested. Called by Sync now before it hands the run to the
     * worker, so a one-off backup still works while auto-backup is switched off.
     */
    fun requestManualRun()

    /**
     * Request a graceful stop of the current upload batch. Prevents any not-yet-started queued item
     * from beginning; the item currently in transit finishes and is backed up.
     */
    fun requestStop()

    /**
     * Joins the photos [bucketName] already backed up ([onDrive], device uri to Drive link) to the
     * folder's Drive album, for when its album mirror is turned on. Returns how many photos were
     * queued.
     */
    suspend fun backfillFolderAlbum(
        userId: UserId,
        bucketName: String,
        onDrive: suspend () -> Map<String, String>,
    ): Int

    /**
     * Run the pending-upload batch. Returned to the worker so it can distinguish "0 to upload"
     * (success) from "tried N, 0 succeeded".
     */
    suspend operator fun invoke(userId: UserId): Result

    /**
     * Returned to the worker so it can distinguish "0 to upload" (success) from "tried N,
     * 0 succeeded" (the latter must not silently mark sync as done when every item failed).
     */
    data class Result(val attempted: Int, val successCount: Int) {
        val allFailed: Boolean get() = attempted > 0 && successCount == 0
    }

    /** What the delete-after-backup step does with the device copy, decided by [resolveDeleteAfterBackupRemoval]. */
    enum class Removal {
        /** Move the device copy to the system trash, recoverable for the OS retention window. */
        TRASH,
        /** Remove the device copy outright, with no recovery. */
        PERMANENT,
        /** Leave the device copy alone. */
        NONE,
    }

    companion object {
        /** Android R (API 30), where the system trash arrives. Kept numeric so this stays pure JVM. */
        const val API_R = 30

        /**
         * Whether an upload's device copy is trashed, permanently removed, or kept, the single
         * safety decision of the delete-after-backup path. Pure and side-effect-free so it can be
         * pinned by a plain JVM test.
         *
         * The device copy is touched only when the user opted into [deleteLocalAfterBackup] AND the
         * Drive copy is committed ([uploadSucceeded]) — a failed upload must never cost the user the
         * only copy of a photo. From R the removal goes through the system trash so it stays
         * recoverable; below R no system trash exists, so the copy can only be removed outright.
         */
        fun resolveDeleteAfterBackupRemoval(
            sdkInt: Int,
            deleteLocalAfterBackup: Boolean,
            uploadSucceeded: Boolean,
        ): Removal = when {
            !deleteLocalAfterBackup || !uploadSucceeded -> Removal.NONE
            sdkInt >= API_R -> Removal.TRASH
            else -> Removal.PERMANENT
        }

        /**
         * The capture time an upload stamps onto its Drive copy, chosen from the sources the caller
         * already holds. Pure and side-effect-free so the choice can be pinned by a plain JVM test.
         *
         * Precedence: strip-timestamp floors the time to [nowMs]; otherwise an explicit MediaStore
         * DATE_TAKEN ([dateTakenIsExplicit]) wins; otherwise a non-explicit MediaStore date falls
         * back to a positive [exifDateTimeOriginalMs]; with none of those the MediaStore date is kept.
         */
        fun resolveUploadCaptureTimeMs(
            mediaStoreDateTakenMs: Long,
            dateTakenIsExplicit: Boolean,
            exifDateTimeOriginalMs: Long?,
            stripTimestamp: Boolean,
            nowMs: Long,
        ): Long {
            if (stripTimestamp) return nowMs
            if (!dateTakenIsExplicit && exifDateTimeOriginalMs != null && exifDateTimeOriginalMs > 0L) {
                return exifDateTimeOriginalMs
            }
            return mediaStoreDateTakenMs
        }

        /**
         * Whether an upload's mirror step actually rewrote the on-device file GPS-free, which is what
         * makes that file's stored `photo_location` fix stale. All three have to hold.
         */
        fun mirrorRemovedDeviceGps(
            stripGpsOnUpload: Boolean,
            mirrorToLocal: Boolean,
            deviceRewriteSucceeded: Boolean,
        ): Boolean = stripGpsOnUpload && mirrorToLocal && deviceRewriteSucceeded

        /**
         * The cloud displayName a rename-on-upload derives from a source's capture timestamp,
         * formatted `yyyy-MM-dd_HH-mm-ss` and keeping the original extension. A non-positive
         * [dateTakenMs] (absent MediaStore DATE_TAKEN) falls back to [nowMs].
         */
        fun uploadRenamedName(displayName: String, dateTakenMs: Long, nowMs: Long): String {
            val ext = displayName.substringAfterLast('.', "")
            val captureMs = dateTakenMs.takeIf { it > 0L } ?: nowMs
            val base = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date(captureMs))
            return if (ext.isNotEmpty()) "$base.$ext" else base
        }
    }
}
