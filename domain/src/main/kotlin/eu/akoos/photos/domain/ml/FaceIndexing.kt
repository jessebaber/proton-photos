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

package eu.akoos.photos.domain.ml

import eu.akoos.photos.domain.entity.GalleryItem
import kotlinx.coroutines.flow.StateFlow
import me.proton.core.domain.entity.UserId

enum class FaceIndexingState { Idle, WaitingModel, Running, Paused, Done }

data class FaceIndexingProgress(
    val state: FaceIndexingState,
    val indexed: Int,
    val total: Int,
    val heartbeat: Long = 0L,
    val grouping: Boolean = false,
    val waitingForWifi: Boolean = false,
)

/** Schedules and controls on-device face indexing. */
interface FaceIndexing {
    val progress: StateFlow<FaceIndexingProgress>
    suspend fun indexAll(userId: UserId?)
    suspend fun awaitIdle()
    suspend fun <T> withImportFence(block: suspend () -> T): T
    suspend fun setPaused(paused: Boolean)
    fun requestIndex(userId: UserId?)
    fun requestRecluster(userId: UserId?)
    suspend fun regroupNow(userId: UserId?): Boolean
    suspend fun indexPhotoOnDemand(item: GalleryItem, userId: UserId?, force: Boolean = false): Boolean
    suspend fun reset()
    suspend fun rescan(userId: UserId?)
    suspend fun wipeFaceData(userId: UserId?, thenIndex: Boolean = true)
}
