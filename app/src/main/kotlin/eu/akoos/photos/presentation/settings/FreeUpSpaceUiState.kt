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
    workSucceeded && readyCount > 0 -> FreeUpSpacePhase.Ready
    else -> FreeUpSpacePhase.Idle
}
