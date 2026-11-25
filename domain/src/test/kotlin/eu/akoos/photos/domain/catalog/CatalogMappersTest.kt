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

package eu.akoos.photos.domain.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CatalogMappersTest {

    @Test
    fun dayMetaRecordOrNull_mapsPositiveFields() {
        val record = CatalogMappers.dayMetaRecordOrNull(
            date = "2024-06-01",
            userId = "user-1",
            coverPhotoUri = "content://photo/1",
            locationText = "Paris",
            description = "Trip",
            updatedAt = 100L,
        )
        assertEquals(
            DayMetaRecord("2024-06-01", "user-1", "content://photo/1", "Paris", "Trip", 100L),
            record,
        )
    }

    @Test
    fun dayMetaRecordOrNull_rejectsBlankDateAndCollapsesBlankText() {
        assertNull(
            CatalogMappers.dayMetaRecordOrNull(
                date = "  ",
                userId = "user-1",
                coverPhotoUri = null,
                locationText = "  ",
                description = "",
                updatedAt = 0L,
            ),
        )
        val record = CatalogMappers.dayMetaRecordOrNull(
            date = "2024-06-02",
            userId = "user-1",
            coverPhotoUri = "  ",
            locationText = "   ",
            description = "",
            updatedAt = 1L,
        )
        assertEquals(
            DayMetaRecord("2024-06-02", "user-1", null, null, null, 1L),
            record,
        )
    }

    @Test
    fun faceRecord_mapsEmbeddingByContent() {
        val embedding = byteArrayOf(1, 2, 3)
        val record = CatalogMappers.faceRecord(
            id = "f1",
            userId = "u",
            photoKey = "p",
            left = 0f,
            top = 0f,
            right = 1f,
            bottom = 1f,
            landmarks = "",
            embedding = embedding,
            personId = 9L,
            score = 0.9f,
        )
        assertEquals("f1", record.id)
        assertEquals(9L, record.personId)
        assertEquals(byteArrayOf(1, 2, 3).toList(), record.embedding.toList())
    }
}

