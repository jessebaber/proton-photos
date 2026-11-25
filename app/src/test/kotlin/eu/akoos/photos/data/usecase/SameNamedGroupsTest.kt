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

package eu.akoos.photos.data.usecase

import eu.akoos.photos.data.db.entity.PersonEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class SameNamedGroupsTest {
    @Test
    fun same_named_people_group_under_the_one_with_most_faces() {
        val people = listOf(
            PersonEntity(id = 1, userId = "a", displayName = "adam", faceCount = 3),
            PersonEntity(id = 2, userId = "a", displayName = "Adam", faceCount = 9),
            PersonEntity(id = 3, userId = "a", displayName = "Ádám", faceCount = 4),
            PersonEntity(id = 4, userId = "a", displayName = null, faceCount = 50),
        )
        val groups = sameNamedGroups(people)
        assertEquals(1, groups.size)
        assertEquals(listOf(2L, 1L), groups[0].map { it.id })
    }
}
