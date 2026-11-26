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

import eu.akoos.photos.data.semantic.SemanticIndexingProgress as DataProgress
import eu.akoos.photos.data.semantic.SemanticIndexingScheduler
import eu.akoos.photos.data.semantic.SemanticIndexingState as DataState
import eu.akoos.photos.di.AppScope
import eu.akoos.photos.domain.ml.SemanticIndexing
import eu.akoos.photos.domain.ml.SemanticIndexingProgress
import eu.akoos.photos.domain.ml.SemanticIndexingState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import me.proton.core.domain.entity.UserId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SemanticIndexingImpl @Inject constructor(
    private val scheduler: SemanticIndexingScheduler,
    @AppScope private val appScope: CoroutineScope,
) : SemanticIndexing {
    override val progress: StateFlow<SemanticIndexingProgress> = scheduler.progress
        .map { it.toDomain() }
        .stateIn(appScope, SharingStarted.Eagerly, scheduler.progress.value.toDomain())

    override suspend fun indexAll(userId: UserId?) = scheduler.indexAll(userId)
    override fun setPaused(paused: Boolean) = scheduler.setPaused(paused)
    override fun requestIndex(userId: UserId?) = scheduler.requestIndex(userId)
    override suspend fun reset(userId: UserId?) = scheduler.reset(userId)

    private fun DataProgress.toDomain() = SemanticIndexingProgress(
        state = when (state) {
            DataState.Idle -> SemanticIndexingState.Idle
            DataState.WaitingModel -> SemanticIndexingState.WaitingModel
            DataState.Running -> SemanticIndexingState.Running
            DataState.Paused -> SemanticIndexingState.Paused
            DataState.Done -> SemanticIndexingState.Done
        },
        indexed = indexed,
        total = total,
    )
}
