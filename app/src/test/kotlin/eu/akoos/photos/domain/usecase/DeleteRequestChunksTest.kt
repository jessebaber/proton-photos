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
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins [deleteRequestChunks]: MediaStore delete/trash request builders reject (or Binder-blow)
 * oversized URI lists, so reclaim must slice at the platform max before building a request.
 */
class DeleteRequestChunksTest {

    @Test
    fun `an empty list yields no chunks`() {
        assertEquals(emptyList<List<String>>(), deleteRequestChunks(emptyList()))
    }

    @Test
    fun `a list at the max stays one chunk`() {
        val uris = (1..DELETE_REQUEST_MAX_URIS).map { "content://media/external/images/media/$it" }
        val chunks = deleteRequestChunks(uris)
        assertEquals(1, chunks.size)
        assertEquals(DELETE_REQUEST_MAX_URIS, chunks.single().size)
        assertEquals(uris, chunks.single())
    }

    @Test
    fun `two thousand and one uris split into max then remainder`() {
        val uris = (1..2_001).map { "content://media/external/images/media/$it" }
        val chunks = deleteRequestChunks(uris)
        assertEquals(listOf(2_000, 1), chunks.map { it.size })
        assertEquals(uris, chunks.flatten())
    }

    @Test
    fun `no chunk ever exceeds the max`() {
        val uris = (1..5_001).map { "uri-$it" }
        val chunks = deleteRequestChunks(uris, max = DELETE_REQUEST_MAX_URIS)
        assertTrue(chunks.isNotEmpty())
        assertTrue(
            "every chunk must stay at or under $DELETE_REQUEST_MAX_URIS: ${chunks.map { it.size }}",
            chunks.all { it.size <= DELETE_REQUEST_MAX_URIS },
        )
        assertEquals(uris.size, chunks.sumOf { it.size })
    }

    @Test
    fun `a custom max is respected`() {
        val uris = (1..5).map { "uri-$it" }
        val chunks = deleteRequestChunks(uris, max = 2)
        assertEquals(listOf(2, 2, 1), chunks.map { it.size })
        assertEquals(uris, chunks.flatten())
    }
}
