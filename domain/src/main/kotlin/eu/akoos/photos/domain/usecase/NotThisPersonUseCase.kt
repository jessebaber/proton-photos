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

import eu.akoos.photos.domain.usecase.personPhotoCount
import kotlinx.coroutines.flow.first

/**
 * "This isn't them" for the given faces of a person. The exact faces are recorded as rejected for the
 * person's name (so they never rejoin), then unassigned WITHOUT rejecting, so they recluster and can be
 * suggested for the right person rather than vanishing from People. Any manual membership of
 * [photoKeys] is detached, the cached cover and count are refreshed, all in one transaction counted as one
 * edit, and a regroup is requested.
 *
 * Shared by the person page's "Remove from person" and the viewer's face tag, so both keep the same
 * bookkeeping. A nameless person records no rejection, as the mark is keyed by name.
 */
interface NotThisPersonUseCase {
    suspend operator fun invoke(account: String, personId: Long, faceIds: List<String>, photoKeys: Collection<String>)
}
