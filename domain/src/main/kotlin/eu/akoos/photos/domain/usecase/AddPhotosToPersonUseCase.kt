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

import kotlinx.coroutines.flow.first

/**
 * Attach photos to a person, shared by every surface that offers "add to person" (the person page's
 * picker, the timeline selection, an album's selection). The membership is stored against the person's
 * NAME, which survives a clustering rebuild, so the person must be named first; a call for an unnamed
 * person is a no-op. [photoKeys] are item stableIds, the same keyspace the face index and library use.
 */
interface AddPhotosToPersonUseCase {
    suspend operator fun invoke(personId: Long, photoKeys: Collection<String>)
}
