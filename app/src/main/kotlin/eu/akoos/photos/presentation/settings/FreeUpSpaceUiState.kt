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

package eu.akoos.photos.presentation.settings

import eu.akoos.photos.domain.entity.SyncState
import kotlin.math.max

/**
 * Pure UI phase for the Free up space screen, derived from WorkInfo + ready count so a JVM test can
 * pin checking → ready → freeing → done without Compose or WorkManager.
 */
enum class FreeUpSpacePhase {
    Idle,
    Checking,
    Ready,
    Freeing,
    Done,
    Error,
}

/**
 * Derives the screen phase from background-verify work state and the persisted ready set.
 * Freeing wins over Ready while the foreground finisher runs; a terminal message maps to Done.
 */
fun deriveFreeUpSpaceState(
    workRunning: Boolean,
    workSucceeded: Boolean,
    workFailed: Boolean,
    readyCount: Int,
    freeing: Boolean,
    doneMessage: Boolean,
): FreeUpSpacePhase = when {
    workFailed -> FreeUpSpacePhase.Error
    freeing -> FreeUpSpacePhase.Freeing
    doneMessage -> FreeUpSpacePhase.Done
    workRunning -> FreeUpSpacePhase.Checking
    // Ready set may come from manual verify SUCCEEDED or from the scheduled worker's persisted
    // chunks — do not require workSucceeded so auto free-up can finish when the screen opens.
    readyCount > 0 -> FreeUpSpacePhase.Ready
    workSucceeded -> FreeUpSpacePhase.Idle
    else -> FreeUpSpacePhase.Idle
}

/**
 * Settle the Free-up grid onto the live re-check-confirmed set. Bytes come from row [SyncState.sizeBytes]
 * only — never a mid-run MediaStore refresh.
 */
fun settleFreeUpCandidates(confirmed: List<SyncState>): Pair<List<SyncState>, Long> =
    confirmed to confirmed.sumOf { it.sizeBytes.coerceAtLeast(0L) }

/**
 * Drop successfully deleted URIs from the Free-up grid and subtract their [SyncState.sizeBytes]
 * from the summary (floored at 0). An empty [completedUris] is a no-op.
 */
fun pruneFreeUpCandidates(
    candidates: List<SyncState>,
    reclaimableBytes: Long,
    completedUris: List<String>,
): Pair<List<SyncState>, Long> {
    if (completedUris.isEmpty()) return candidates to reclaimableBytes
    val completed = completedUris.toHashSet()
    var removedBytes = 0L
    val remaining = ArrayList<SyncState>(candidates.size)
    for (row in candidates) {
        if (row.localUri in completed) {
            removedBytes += row.sizeBytes.coerceAtLeast(0L)
        } else {
            remaining += row
        }
    }
    return remaining to max(0L, reclaimableBytes - removedBytes)
}
