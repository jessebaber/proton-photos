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
import eu.akoos.photos.worker.FreeUpPipelineWorker
import kotlin.math.max

/**
 * Pure UI phase for the Free up space screen, derived from WorkInfo phase/counts + ready count so a
 * JVM test can pin checking → rechecking → freeing → ready/done without Compose or WorkManager.
 */
enum class FreeUpSpacePhase {
    Idle,
    Checking,
    Rechecking,
    Ready,
    Freeing,
    Stopped,
    Done,
    Error,
}

/**
 * Derives the screen phase from pipeline WorkInfo and the persisted ready set.
 *
 * [workerPhase] is the WorkInfo progress/output [FreeUpPipelineWorker.KEY_PHASE] wire name
 * (`verifying` / `rechecking` / `deleting` / `awaiting_consent`).
 */
fun deriveFreeUpSpaceState(
    workRunning: Boolean,
    workSucceeded: Boolean,
    workFailed: Boolean,
    readyCount: Int,
    doneMessage: Boolean,
    workerPhase: String? = null,
    stopped: Boolean = false,
    workCancelled: Boolean = false,
): FreeUpSpacePhase = when {
    workFailed -> FreeUpSpacePhase.Error
    stopped -> FreeUpSpacePhase.Stopped
    doneMessage -> FreeUpSpacePhase.Done
    workRunning -> phaseWhileRunning(workerPhase)
    workerPhase == FreeUpPipelineWorker.PHASE_AWAITING_CONSENT -> FreeUpSpacePhase.Ready
    // Ready set may come from manual SUCCEEDED (awaiting consent) or from the scheduled worker's
    // persisted markers — do not require workSucceeded so auto free-up can surface Ready on open.
    readyCount > 0 -> FreeUpSpacePhase.Ready
    workCancelled -> FreeUpSpacePhase.Stopped
    workSucceeded -> FreeUpSpacePhase.Idle
    else -> FreeUpSpacePhase.Idle
}

private fun phaseWhileRunning(workerPhase: String?): FreeUpSpacePhase = when (workerPhase) {
    FreeUpPipelineWorker.PHASE_RECHECKING -> FreeUpSpacePhase.Rechecking
    FreeUpPipelineWorker.PHASE_DELETING -> FreeUpSpacePhase.Freeing
    FreeUpPipelineWorker.PHASE_AWAITING_CONSENT -> FreeUpSpacePhase.Ready
    else -> FreeUpSpacePhase.Checking // verifying, or enqueued with no progress yet
}

/**
 * Settle the Free-up grid onto the live re-check-confirmed set. Per-URI bytes use
 * `max(row.sizeBytes, knownBytesByUri[uri])` so a zero [SyncState.sizeBytes] does not collapse the
 * header when the grid already knew a size — never a mid-run MediaStore refresh.
 */
fun settleFreeUpCandidates(
    confirmed: List<SyncState>,
    knownBytesByUri: Map<String, Long> = emptyMap(),
): Pair<List<SyncState>, Long> {
    val settled = confirmed.map { row ->
        val size = max(row.sizeBytes.coerceAtLeast(0L), knownBytesByUri[row.localUri] ?: 0L)
        if (size == row.sizeBytes) row else row.copy(sizeBytes = size)
    }
    return settled to settled.sumOf { it.sizeBytes.coerceAtLeast(0L) }
}

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
