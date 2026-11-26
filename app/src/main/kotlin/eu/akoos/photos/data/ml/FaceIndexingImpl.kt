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

package eu.akoos.photos.data.ml

import eu.akoos.photos.data.face.FaceIndexingProgress as DataProgress
import eu.akoos.photos.data.face.FaceIndexingScheduler
import eu.akoos.photos.data.face.FaceIndexingState as DataState
import eu.akoos.photos.data.face.FaceSweepEvent as DataSweepEvent
import eu.akoos.photos.data.face.SweepStop as DataSweepStop
import eu.akoos.photos.domain.entity.GalleryItem
import eu.akoos.photos.domain.ml.FaceIndexing
import eu.akoos.photos.domain.ml.FaceIndexingProgress
import eu.akoos.photos.domain.ml.FaceIndexingState
import eu.akoos.photos.domain.ml.FaceSweepEvent
import eu.akoos.photos.domain.ml.SweepStop
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import eu.akoos.photos.di.AppScope
import kotlinx.coroutines.CoroutineScope
import me.proton.core.domain.entity.UserId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FaceIndexingImpl @Inject constructor(
    private val scheduler: FaceIndexingScheduler,
    @AppScope private val appScope: CoroutineScope,
) : FaceIndexing {
    override val progress: StateFlow<FaceIndexingProgress> = scheduler.progress
        .map { it.toDomain() }
        .stateIn(appScope, SharingStarted.Eagerly, scheduler.progress.value.toDomain())

    override suspend fun indexAll(userId: UserId?) = scheduler.indexAll(userId)
    override suspend fun awaitIdle() = scheduler.awaitIdle()
    override suspend fun <T> withImportFence(block: suspend () -> T): T = scheduler.withImportFence(block)
    override suspend fun setPaused(paused: Boolean) = scheduler.setPaused(paused)
    override fun requestIndex(userId: UserId?) = scheduler.requestIndex(userId)
    override fun requestRecluster(userId: UserId?) = scheduler.requestRecluster(userId)
    override suspend fun regroupNow(userId: UserId?): Boolean = scheduler.regroupNow(userId)
    override suspend fun indexPhotoOnDemand(item: GalleryItem, userId: UserId?, force: Boolean): Boolean =
        scheduler.indexPhotoOnDemand(item, userId, force)
    override suspend fun reset() = scheduler.reset()
    override suspend fun rescan(userId: UserId?) = scheduler.rescan(userId)
    override suspend fun wipeFaceData(userId: UserId?, thenIndex: Boolean) =
        scheduler.wipeFaceData(userId, thenIndex)

    override fun sweepFacelessForPerson(
        userId: UserId?,
        centroid: FloatArray,
        matchThreshold: Float,
    ): Flow<FaceSweepEvent> =
        scheduler.sweepFacelessForPerson(userId, centroid, matchThreshold).map { it.toDomain() }

    private fun DataSweepEvent.toDomain(): FaceSweepEvent = when (this) {
        is DataSweepEvent.Progress -> FaceSweepEvent.Progress(done, total)
        is DataSweepEvent.Match -> FaceSweepEvent.Match(
            photoKey, index, box, landmarks, embedding, score, blur,
        )
        is DataSweepEvent.Stopped -> FaceSweepEvent.Stopped(
            reason = when (reason) {
                DataSweepStop.COMPLETED -> SweepStop.COMPLETED
                DataSweepStop.MODEL_STALLED -> SweepStop.MODEL_STALLED
                DataSweepStop.CANCELLED -> SweepStop.CANCELLED
                DataSweepStop.FAILED -> SweepStop.FAILED
            },
            done = done,
            total = total,
        )
    }

    private fun DataProgress.toDomain() = FaceIndexingProgress(
        state = when (state) {
            DataState.Idle -> FaceIndexingState.Idle
            DataState.WaitingModel -> FaceIndexingState.WaitingModel
            DataState.Running -> FaceIndexingState.Running
            DataState.Paused -> FaceIndexingState.Paused
            DataState.Done -> FaceIndexingState.Done
        },
        indexed = indexed,
        total = total,
        heartbeat = heartbeat,
        grouping = grouping,
        waitingForWifi = waitingForWifi,
    )
}
