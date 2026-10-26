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

import eu.akoos.photos.domain.usecase.DELETE_REQUEST_MAX_URIS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the hard separation of trash vs permanent-delete consent sinks, and the
 * [DELETE_REQUEST_MAX_URIS] chunk selection used by [MediaStoreConsentHandler].
 */
class MediaStoreConsentModeTest {

    @Test
    fun `trash and permanent delete are distinct sinks`() {
        assertNotEquals(MediaStoreConsentMode.Trash, MediaStoreConsentMode.PermanentDelete)
        assertEquals(2, MediaStoreConsentMode.entries.size)
    }

    @Test
    fun nextBatch_empty_pending_is_empty() {
        assertTrue(nextMediaStoreConsentBatch(emptySet()).isEmpty())
    }

    @Test
    fun nextBatch_under_ceiling_returns_all() {
        val pending = (1..10).map { "content://$it" }.toSet()
        assertEquals(pending, nextMediaStoreConsentBatch(pending))
    }

    @Test
    fun nextBatch_over_ceiling_caps_at_DELETE_REQUEST_MAX_URIS() {
        val pending = (1..(DELETE_REQUEST_MAX_URIS + 50)).map { "content://$it" }.toSet()
        val batch = nextMediaStoreConsentBatch(pending)
        assertEquals(DELETE_REQUEST_MAX_URIS, batch.size)
        assertTrue(pending.containsAll(batch))
    }

    @Test
    fun nextBatch_after_removing_first_chunk_advances_to_remainder() {
        val all = (1..(DELETE_REQUEST_MAX_URIS + 3)).map { "content://$it" }
        val pending = all.toSet()
        val first = nextMediaStoreConsentBatch(pending)
        val remaining = pending - first
        val second = nextMediaStoreConsentBatch(remaining)
        assertEquals(3, second.size)
        assertTrue(first.intersect(second).isEmpty())
        assertEquals(remaining, second)
    }
}
