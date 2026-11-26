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

import kotlinx.coroutines.flow.StateFlow
import me.proton.core.domain.entity.UserId

enum class SemanticIndexingState { Idle, WaitingModel, Running, Paused, Done }

data class SemanticIndexingProgress(
    val state: SemanticIndexingState,
    val indexed: Int,
    val total: Int,
)

enum class SemanticStatusLabel { WaitingModel, Indexing, Paused, Ready, NotIndexed }

const val SEMANTIC_INDEX_FOREGROUND_THRESHOLD = 50

fun semanticStatusLabel(
    state: SemanticIndexingState,
    indexedCount: Int,
    pending: Int,
): SemanticStatusLabel {
    val bulkIndex = pending >= SEMANTIC_INDEX_FOREGROUND_THRESHOLD
    return when (state) {
        SemanticIndexingState.WaitingModel -> SemanticStatusLabel.WaitingModel
        SemanticIndexingState.Running ->
            if (bulkIndex) SemanticStatusLabel.Indexing
            else if (indexedCount > 0) SemanticStatusLabel.Ready
            else SemanticStatusLabel.NotIndexed
        SemanticIndexingState.Paused ->
            if (bulkIndex) SemanticStatusLabel.Paused
            else if (indexedCount > 0) SemanticStatusLabel.Ready
            else SemanticStatusLabel.NotIndexed
        SemanticIndexingState.Done -> SemanticStatusLabel.Ready
        SemanticIndexingState.Idle ->
            if (indexedCount > 0) SemanticStatusLabel.Ready else SemanticStatusLabel.NotIndexed
    }
}

/** Schedules and controls on-device semantic embedding indexing. */
interface SemanticIndexing {
    val progress: StateFlow<SemanticIndexingProgress>
    suspend fun indexAll(userId: UserId?)
    fun setPaused(paused: Boolean)
    fun requestIndex(userId: UserId?)
    suspend fun reset(userId: UserId?)
}
