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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.proton.core.domain.entity.UserId

/**
 * Assigns one or more faces to a person: an existing one picked by id, or a typed name, which resolves
 * to the account's person of that name (ignoring case) or creates one. The faces move onto the person at
 * once and are confirmed under its name, so the choice holds across the next rebuild, and a "not this
 * person" mark for that name on them is lifted. Moving faces onto an unnamed person clears their
 * confirmation instead, since an old confirmed name would pull them back to it on a rebuild.
 *
 * The people the faces came from get a fresh count and cover, one left with no faces is removed, and
 * every touched person's cached centroid is dropped so no stale summary places new faces. A new person
 * is inserted already carrying its count and cover, so an empty-person sweep never removes it. All
 * writes land in one transaction, keyed to the account or, for a guest, the local partition.
 *
 * Returns the person's id, or -1 when nothing was assigned (a blank name, a person that is not the
 * account's, the Unsorted bucket, or no matching face). With `recluster` set, one regroup is requested
 * afterwards so the name pulls in the person's other faces; a caller assigning over several calls passes
 * false and requests it once itself.
 */
interface AssignFacesToPersonUseCase {
    sealed interface Target {
        /** An existing person, picked by id. */
        data class Existing(val personId: Long) : Target

        /** A typed name: the person already called that, ignoring case, else a new one. */
        data class Named(val name: String) : Target
    }

    suspend operator fun invoke(
        userId: UserId?,
        faceIds: Collection<String>,
        target: Target,
        recluster: Boolean = true,
    ): Long
}
