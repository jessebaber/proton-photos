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

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure decision coverage for [shouldPromptMediaStoreConsent]: trash growth re-prompt,
 * permanent-delete deny stop, and in-flight gating.
 */
class MediaStoreConsentPromptTest {

    @Test
    fun trash_prompts_when_queue_grows_past_high_water() {
        assertTrue(
            shouldPromptMediaStoreConsent(
                mode = MediaStoreConsentMode.Trash,
                pendingSize = 3,
                lastPromptedSize = 2,
                inFlight = false,
                denyStopped = false,
            ),
        )
    }

    @Test
    fun trash_does_not_prompt_at_or_below_high_water_after_deny() {
        // Deny leaves lastPromptedSize at the denied batch size; same size must not re-fire.
        assertFalse(
            shouldPromptMediaStoreConsent(
                mode = MediaStoreConsentMode.Trash,
                pendingSize = 5,
                lastPromptedSize = 5,
                inFlight = false,
                denyStopped = true, // ignored for Trash
            ),
        )
        assertFalse(
            shouldPromptMediaStoreConsent(
                mode = MediaStoreConsentMode.Trash,
                pendingSize = 4,
                lastPromptedSize = 5,
                inFlight = false,
                denyStopped = false,
            ),
        )
    }

    @Test
    fun trash_denyStopped_does_not_block_growth_reprompt() {
        assertTrue(
            shouldPromptMediaStoreConsent(
                mode = MediaStoreConsentMode.Trash,
                pendingSize = 6,
                lastPromptedSize = 5,
                inFlight = false,
                denyStopped = true,
            ),
        )
    }

    @Test
    fun permanentDelete_stops_after_deny() {
        assertFalse(
            shouldPromptMediaStoreConsent(
                mode = MediaStoreConsentMode.PermanentDelete,
                pendingSize = 10,
                lastPromptedSize = 0,
                inFlight = false,
                denyStopped = true,
            ),
        )
    }

    @Test
    fun permanentDelete_prompts_while_pending_and_not_denied() {
        assertTrue(
            shouldPromptMediaStoreConsent(
                mode = MediaStoreConsentMode.PermanentDelete,
                pendingSize = 10,
                lastPromptedSize = 0,
                inFlight = false,
                denyStopped = false,
            ),
        )
    }

    @Test
    fun inFlight_blocks_both_modes() {
        assertFalse(
            shouldPromptMediaStoreConsent(
                mode = MediaStoreConsentMode.Trash,
                pendingSize = 9,
                lastPromptedSize = 1,
                inFlight = true,
                denyStopped = false,
            ),
        )
        assertFalse(
            shouldPromptMediaStoreConsent(
                mode = MediaStoreConsentMode.PermanentDelete,
                pendingSize = 9,
                lastPromptedSize = 0,
                inFlight = true,
                denyStopped = false,
            ),
        )
    }

    @Test
    fun empty_pending_never_prompts() {
        assertFalse(
            shouldPromptMediaStoreConsent(
                mode = MediaStoreConsentMode.Trash,
                pendingSize = 0,
                lastPromptedSize = 0,
                inFlight = false,
                denyStopped = false,
            ),
        )
        assertFalse(
            shouldPromptMediaStoreConsent(
                mode = MediaStoreConsentMode.PermanentDelete,
                pendingSize = 0,
                lastPromptedSize = 0,
                inFlight = false,
                denyStopped = false,
            ),
        )
    }
}
