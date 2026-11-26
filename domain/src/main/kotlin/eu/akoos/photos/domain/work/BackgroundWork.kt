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

package eu.akoos.photos.domain.work

/** Model kinds the model-download foreground service can fetch. */
enum class ModelDownloadKind { FACE, OCR, SEMANTIC }

/**
 * WorkManager / foreground-service starters presentation uses.
 * Implementations hold Context; methods take only domain parameters.
 */
interface BackgroundWork {
    fun runSyncNow(wifiOnly: Boolean = true, allowLowBattery: Boolean = false)
    suspend fun reconcileSyncBackgroundWork()
    fun cancelSync()

    fun runFreeUpSpaceNow(intervalMs: Long)
    fun cancelFreeUpSpace()

    suspend fun reconcileUpdateCheck()

    fun enqueueAlbumDownload(
        albumLinkId: String,
        albumName: String,
        photoLinkIds: List<String>,
        userId: String,
    )
    fun albumDownloadUniqueName(albumLinkId: String): String

    fun enqueueImport(zipUri: String, runId: String, albumMode: String)
    fun importUniqueName(zipUri: String): String
    fun cancelImport(zipUri: String)

    fun enqueueStageImport(zipUri: String)
    fun stageImportUniqueName(zipUri: String): String
    fun cancelStageImport(zipUri: String)

    fun startBackgroundSync()
    fun stopBackgroundSync()

    /** Cancel sync/import/album-download/metadata-edit work and stop the sync service. */
    fun cancelAccountSessionWork()

    fun startGifExport(
        sourceUri: String,
        startMs: Long,
        endMs: Long,
        aspect: String,
        zoom: Float,
        panX: Float,
        panY: Float,
        maxEdgePx: Int,
        isCloud: Boolean,
        dateTakenMs: Long,
        displayName: String,
    )

    fun startModelDownload(kind: ModelDownloadKind)

    fun startScreenshotOverlay()
    fun stopScreenshotOverlay()

    /** Progress / result Data keys for workers, so presentation need not import worker classes. */
    object ImportKeys {
        const val KEY_DONE = "done"
        const val KEY_TOTAL = "total"
        const val KEY_CURRENT_NAME = "currentName"
        const val KEY_PHASE = "phase"
        const val KEY_PHASE_DONE_BYTES = "phaseDoneBytes"
        const val KEY_PHASE_TOTAL_BYTES = "phaseTotalBytes"
        const val KEY_RESULT_TOTAL = "resultTotal"
        const val KEY_RESULT_IMPORTED = "resultImported"
        const val KEY_RESULT_ALREADY = "resultAlready"
        const val KEY_RESULT_SKIPPED = "resultSkipped"
        const val KEY_RESULT_FAILED = "resultFailed"
        const val KEY_RESULT_ALBUMS = "resultAlbums"
        const val KEY_RESULT_ADDED = "resultAdded"
        const val KEY_RESULT_RUN_ID = "resultRunId"
        const val KEY_FAILURE_KIND = "failureKind"
    }

    object StageImportKeys {
        const val KEY_DONE = "done"
        const val KEY_TOTAL = "total"
        const val KEY_PHASE = "phase"
        const val KEY_BYTES_DONE = "bytesDone"
        const val KEY_BYTES_TOTAL = "bytesTotal"
        const val KEY_RESULT_STAGED = "resultStaged"
        const val KEY_RESULT_ENDED_EARLY = "resultEndedEarly"
        const val KEY_RESULT_READ_PERCENT = "resultReadPercent"
        const val KEY_FAILURE_KIND = "failureKind"
    }

    object AlbumDownloadKeys {
        const val KEY_PROGRESS_DONE = "progressDone"
        const val KEY_PROGRESS_TOTAL = "progressTotal"
        const val KEY_COVER_URI = "coverUri"
        const val KEY_RESULT_SAVED = "resultSaved"
        const val KEY_RESULT_FAILED = "resultFailed"
    }
}
