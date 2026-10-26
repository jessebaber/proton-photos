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
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pins Free-up screen phase transitions for the observe-only WorkInfo pipeline flow.
 */
class FreeUpSpaceDeriveStateTest {

    @Test
    fun `checking while verify work is running`() {
        assertEquals(
            FreeUpSpacePhase.Checking,
            deriveFreeUpSpaceState(
                workRunning = true,
                workSucceeded = false,
                workFailed = false,
                readyCount = 0,
                doneMessage = false,
                workerPhase = FreeUpPipelineWorker.PHASE_VERIFYING,
            ),
        )
    }

    @Test
    fun `checking when enqueued with no phase yet`() {
        assertEquals(
            FreeUpSpacePhase.Checking,
            deriveFreeUpSpaceState(
                workRunning = true,
                workSucceeded = false,
                workFailed = false,
                readyCount = 0,
                doneMessage = false,
                workerPhase = null,
            ),
        )
    }

    @Test
    fun `ready when verify succeeded with a non-empty ready set`() {
        assertEquals(
            FreeUpSpacePhase.Ready,
            deriveFreeUpSpaceState(
                workRunning = false,
                workSucceeded = true,
                workFailed = false,
                readyCount = 3,
                doneMessage = false,
            ),
        )
    }

    @Test
    fun `ready from scheduled verify without manual work SUCCEEDED`() {
        assertEquals(
            FreeUpSpacePhase.Ready,
            deriveFreeUpSpaceState(
                workRunning = false,
                workSucceeded = false,
                workFailed = false,
                readyCount = 2,
                doneMessage = false,
            ),
        )
    }

    @Test
    fun `ready from awaiting_consent worker output`() {
        assertEquals(
            FreeUpSpacePhase.Ready,
            deriveFreeUpSpaceState(
                workRunning = false,
                workSucceeded = true,
                workFailed = false,
                readyCount = 0,
                doneMessage = false,
                workerPhase = FreeUpPipelineWorker.PHASE_AWAITING_CONSENT,
            ),
        )
    }

    @Test
    fun `freeing while worker phase is deleting`() {
        assertEquals(
            FreeUpSpacePhase.Freeing,
            deriveFreeUpSpaceState(
                workRunning = true,
                workSucceeded = false,
                workFailed = false,
                readyCount = 3,
                doneMessage = false,
                workerPhase = FreeUpPipelineWorker.PHASE_DELETING,
            ),
        )
    }

    @Test
    fun `rechecking is distinct from local deletion`() {
        assertEquals(
            FreeUpSpacePhase.Rechecking,
            deriveFreeUpSpaceState(
                workRunning = true,
                workSucceeded = false,
                workFailed = false,
                readyCount = 3,
                doneMessage = false,
                workerPhase = FreeUpPipelineWorker.PHASE_RECHECKING,
            ),
        )
    }

    @Test
    fun `stopped stays visible even when work info is terminal`() {
        assertEquals(
            FreeUpSpacePhase.Stopped,
            deriveFreeUpSpaceState(
                workRunning = false,
                workSucceeded = true,
                workFailed = false,
                readyCount = 2,
                doneMessage = false,
                stopped = true,
            ),
        )
    }

    @Test
    fun `done after a terminal message`() {
        assertEquals(
            FreeUpSpacePhase.Done,
            deriveFreeUpSpaceState(
                workRunning = false,
                workSucceeded = true,
                workFailed = false,
                readyCount = 0,
                doneMessage = true,
            ),
        )
    }

    @Test
    fun `error when verify work failed`() {
        assertEquals(
            FreeUpSpacePhase.Error,
            deriveFreeUpSpaceState(
                workRunning = false,
                workSucceeded = false,
                workFailed = true,
                readyCount = 0,
                doneMessage = false,
            ),
        )
    }

    @Test
    fun `cancelled with no ready set maps to stopped`() {
        assertEquals(
            FreeUpSpacePhase.Stopped,
            deriveFreeUpSpaceState(
                workRunning = false,
                workSucceeded = false,
                workFailed = false,
                readyCount = 0,
                doneMessage = false,
                workCancelled = true,
            ),
        )
    }
}
