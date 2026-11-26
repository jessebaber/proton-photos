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

package eu.akoos.photos.data.work

import android.content.Context
import androidx.lifecycle.asFlow
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.akoos.photos.domain.importer.ImportAlbumMode
import eu.akoos.photos.domain.work.BackgroundWork
import eu.akoos.photos.domain.work.ModelDownloadKind
import eu.akoos.photos.service.BackgroundSyncService
import eu.akoos.photos.service.GifExportService
import eu.akoos.photos.service.ModelDownloadService
import eu.akoos.photos.service.ScreenshotOverlayService
import eu.akoos.photos.util.ModelDownloadKind as UtilModelDownloadKind
import eu.akoos.photos.worker.AlbumDownloadWorker
import eu.akoos.photos.worker.FreeUpSpaceWorker
import eu.akoos.photos.worker.ImportWorker
import eu.akoos.photos.worker.MetadataEditWorker
import eu.akoos.photos.worker.StageImportWorker
import eu.akoos.photos.worker.SyncWorker
import eu.akoos.photos.worker.UpdateCheckWorker
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackgroundWorkImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : BackgroundWork {
    private val workManager get() = WorkManager.getInstance(context)

    override fun runSyncNow(wifiOnly: Boolean, allowLowBattery: Boolean) =
        SyncWorker.runNow(context, wifiOnly, allowLowBattery)

    override suspend fun reconcileSyncBackgroundWork() = SyncWorker.reconcileBackgroundWork(context)

    override fun cancelSync() {
        SyncWorker.cancel(workManager)
    }

    override fun runFreeUpSpaceNow(intervalMs: Long) {
        FreeUpSpaceWorker.runNow(workManager, intervalMs)
    }

    override fun cancelFreeUpSpace() {
        FreeUpSpaceWorker.cancel(workManager)
    }

    override suspend fun reconcileUpdateCheck() = UpdateCheckWorker.reconcile(context)

    override fun enqueueAlbumDownload(
        albumLinkId: String,
        albumName: String,
        photoLinkIds: List<String>,
        userId: String,
    ) = AlbumDownloadWorker.enqueue(context, albumLinkId, albumName, photoLinkIds, userId)

    override fun albumDownloadUniqueName(albumLinkId: String): String =
        AlbumDownloadWorker.uniqueName(albumLinkId)

    override fun enqueueImport(zipUri: String, runId: String, albumMode: String) {
        val mode = runCatching { ImportAlbumMode.valueOf(albumMode) }.getOrDefault(ImportAlbumMode.NONE)
        ImportWorker.enqueue(context, zipUri, runId, mode)
    }

    override fun importUniqueName(zipUri: String): String = ImportWorker.uniqueName(zipUri)

    override fun cancelImport(zipUri: String) {
        workManager.cancelUniqueWork(ImportWorker.uniqueName(zipUri))
    }

    override fun enqueueStageImport(zipUri: String) {
        StageImportWorker.enqueue(context, zipUri)
    }

    override fun stageImportUniqueName(zipUri: String): String = StageImportWorker.uniqueName(zipUri)

    override fun cancelStageImport(zipUri: String) {
        workManager.cancelUniqueWork(StageImportWorker.uniqueName(zipUri))
    }

    override fun startBackgroundSync() = BackgroundSyncService.start(context)
    override fun stopBackgroundSync() = BackgroundSyncService.stop(context)

    override fun cancelAccountSessionWork() {
        SyncWorker.cancel(workManager)
        workManager.cancelUniqueWork(SyncWorker.NAME_ONESHOT)
        workManager.cancelUniqueWork(SyncWorker.NAME_CONTENT_OBSERVER)
        workManager.cancelAllWorkByTag(AlbumDownloadWorker.TAG)
        workManager.cancelAllWorkByTag(ImportWorker.TAG)
        workManager.cancelUniqueWork(MetadataEditWorker.UNIQUE_NAME)
        BackgroundSyncService.stop(context)
    }

    override fun startGifExport(
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
    ) {
        GifExportService.start(
            context = context,
            sourceUri = sourceUri,
            startMs = startMs,
            endMs = endMs,
            aspectName = aspect.ifBlank { "ORIGINAL" },
            zoom = zoom,
            panX = panX,
            panY = panY,
            maxEdgePx = maxEdgePx,
            isCloud = isCloud,
            dateTakenMs = dateTakenMs,
            displayName = displayName,
        )
    }

    override fun startModelDownload(kind: ModelDownloadKind) {
        val utilKind = when (kind) {
            ModelDownloadKind.FACE -> UtilModelDownloadKind.FACE
            ModelDownloadKind.OCR -> UtilModelDownloadKind.OCR
            ModelDownloadKind.SEMANTIC -> UtilModelDownloadKind.SEMANTIC
        }
        ModelDownloadService.start(context, utilKind)
    }

    override fun startScreenshotOverlay() = ScreenshotOverlayService.start(context)
    override fun stopScreenshotOverlay() = ScreenshotOverlayService.stop(context)

    override fun scheduleSync(wifiOnly: Boolean, intervalMinutes: Long) {
        SyncWorker.schedule(workManager, wifiOnly, intervalMinutes)
    }

    override fun scheduleFreeUpSpace(intervalMs: Long) {
        FreeUpSpaceWorker.schedule(workManager, intervalMs)
    }

    override fun observeAlbumDownloads(): Flow<List<BackgroundWork.AlbumDownloadProgress>> =
        workManager.getWorkInfosByTagLiveData(AlbumDownloadWorker.TAG).asFlow().map { infos ->
            infos.filter { it.state == WorkInfo.State.RUNNING }.map { wi ->
                BackgroundWork.AlbumDownloadProgress(
                    id = wi.id.toString(),
                    albumName = wi.progress.getString(AlbumDownloadWorker.KEY_ALBUM_NAME).orEmpty(),
                    done = wi.progress.getInt(AlbumDownloadWorker.KEY_PROGRESS_DONE, 0),
                    total = wi.progress.getInt(AlbumDownloadWorker.KEY_PROGRESS_TOTAL, 0),
                    coverUri = wi.progress.getString(BackgroundWork.AlbumDownloadKeys.KEY_COVER_URI)
                        ?.takeIf { it.isNotBlank() },
                )
            }.filter { it.total > 0 }
        }

    override fun cancelWorkById(id: String) {
        runCatching { workManager.cancelWorkById(UUID.fromString(id)) }
    }

}

