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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins empty/busy helpers for the Free-up screen after settle/prune/ready-grid machinery was
 * removed. Grid coherence during Checking is owned by the ViewModel live-grid guard.
 */
class FreeUpSpaceCandidateListTest {

    @Test
    fun `isEmpty is false while loading even with no candidates`() {
        val state = FreeUpSpaceViewModel.UiState(loading = true, candidates = emptyList())
        assertFalse(state.isEmpty)
    }

    @Test
    fun `isEmpty is true only when loaded with no candidates`() {
        val state = FreeUpSpaceViewModel.UiState(loading = false, candidates = emptyList())
        assertTrue(state.isEmpty)
    }

    @Test
    fun `phase enum is Idle Checking Freeing Stopped Done Error`() {
        assertEquals(
            listOf(
                FreeUpSpacePhase.Idle,
                FreeUpSpacePhase.Checking,
                FreeUpSpacePhase.Freeing,
                FreeUpSpacePhase.Stopped,
                FreeUpSpacePhase.Done,
                FreeUpSpacePhase.Error,
            ),
            FreeUpSpacePhase.entries,
        )
    }
}
