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

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pins Free-up screen phase transitions for the background-verify + foreground-finish flow.
 * Red until [deriveFreeUpSpaceState] implements the real reducer (Phase 10).
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
                freeing = false,
                doneMessage = false,
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
                freeing = false,
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
                freeing = false,
                doneMessage = false,
            ),
        )
    }

    @Test
    fun `freeing while the foreground finisher runs`() {
        assertEquals(
            FreeUpSpacePhase.Freeing,
            deriveFreeUpSpaceState(
                workRunning = false,
                workSucceeded = true,
                workFailed = false,
                readyCount = 3,
                freeing = true,
                doneMessage = false,
            ),
        )
    }

    @Test
    fun `rechecking is distinct from local deletion`() {
        assertEquals(
            FreeUpSpacePhase.Rechecking,
            deriveFreeUpSpaceState(
                workRunning = false,
                workSucceeded = true,
                workFailed = false,
                readyCount = 3,
                freeing = false,
                doneMessage = false,
                rechecking = true,
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
                freeing = false,
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
                freeing = false,
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
                freeing = false,
                doneMessage = false,
            ),
        )
    }
}
