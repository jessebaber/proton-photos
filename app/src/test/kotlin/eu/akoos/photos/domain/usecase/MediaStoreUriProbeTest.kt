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

package eu.akoos.photos.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure coverage for the MediaStore URI classification used before createDeleteRequest /
 * createTrashRequest: missing or non-numeric ids must leave the consent batch so one stale URI
 * cannot fail the whole request.
 */
class MediaStoreUriProbeTest {

    @Test
    fun `mediaStoreRowId reads the trailing numeric segment`() {
        assertEquals(42L, mediaStoreRowId("content://media/external/images/media/42"))
        assertEquals(7L, mediaStoreRowId("content://media/external/video/media/7"))
    }

    @Test
    fun `mediaStoreRowId returns null when there is no numeric id`() {
        assertNull(mediaStoreRowId("content://media/external/images/media"))
        assertNull(mediaStoreRowId("file:///sdcard/photo.jpg"))
        assertNull(mediaStoreRowId(""))
    }

    @Test
    fun `urisPresentInMediaStore keeps only ids that still exist`() {
        val uris = listOf(
            "content://media/external/images/media/1",
            "content://media/external/images/media/2",
            "content://media/external/images/media/3",
            "content://other/no-id",
        )
        assertEquals(
            listOf(
                "content://media/external/images/media/1",
                "content://media/external/images/media/3",
            ),
            urisPresentInMediaStore(uris, existingIds = setOf(1L, 3L, 99L)),
        )
    }

    @Test
    fun `urisPresentInMediaStore drops everything when the probe finds nothing`() {
        val uris = listOf(
            "content://media/external/images/media/1",
            "content://media/external/images/media/2",
        )
        assertTrue(urisPresentInMediaStore(uris, existingIds = emptySet()).isEmpty())
    }
}
