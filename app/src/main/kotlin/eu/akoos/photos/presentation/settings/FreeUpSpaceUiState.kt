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

import eu.akoos.photos.worker.FreeUpPipelineWorker

/**
 * Pure UI phase for the Free up space screen, derived from WorkInfo phase/counts so a JVM test can
 * pin checking → freeing → stopped/done without Compose or WorkManager.
 */
enum class FreeUpSpacePhase {
    Idle,
    Checking,
    Freeing,
    Stopped,
    Done,
    Error,
}

/**
 * Derives the screen phase from pipeline WorkInfo.
 *
 * [workerPhase] is the WorkInfo progress/output [FreeUpPipelineWorker.KEY_PHASE] wire name
 * (`verifying` / `deleting` / `awaiting_consent`). Awaiting consent maps to [FreeUpSpacePhase.Stopped]
 * so the screen keeps the candidate grid and shows Continue (no Ready/empty-grid flash).
 */
fun deriveFreeUpSpaceState(
    workRunning: Boolean,
    workSucceeded: Boolean,
    workFailed: Boolean,
    doneMessage: Boolean,
    workerPhase: String? = null,
    stopped: Boolean = false,
    workCancelled: Boolean = false,
): FreeUpSpacePhase = when {
    workFailed -> FreeUpSpacePhase.Error
    stopped -> FreeUpSpacePhase.Stopped
    doneMessage -> FreeUpSpacePhase.Done
    workRunning -> phaseWhileRunning(workerPhase)
    workerPhase == FreeUpPipelineWorker.PHASE_AWAITING_CONSENT -> FreeUpSpacePhase.Stopped
    workCancelled -> FreeUpSpacePhase.Stopped
    workSucceeded -> FreeUpSpacePhase.Idle
    else -> FreeUpSpacePhase.Idle
}

private fun phaseWhileRunning(workerPhase: String?): FreeUpSpacePhase = when (workerPhase) {
    FreeUpPipelineWorker.PHASE_DELETING -> FreeUpSpacePhase.Freeing
    // awaiting_consent mid-run is terminal in practice; treat as Checking until WorkInfo settles.
    else -> FreeUpSpacePhase.Checking // verifying, or enqueued with no progress yet
}
