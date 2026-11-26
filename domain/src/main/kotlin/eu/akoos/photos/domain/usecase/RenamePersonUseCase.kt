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

/**
 * Renames a person in place, or takes the name away with a null or blank [newName]. A new name re-keys the
 * old name's curation (manual photos, confirmations, "not this person" marks, cover, curation times) and
 * confirms the person's current faces under it, so a rebuild keeps them together. Taking the name away
 * clears that name's confirmation from every face of the account and the person's own faces' confirmations,
 * so the removed name cannot pull faces back together on a rebuild. One transaction, counted as one edit.
 *
 * Renaming onto a name another person holds is a merge, which [AssignPersonNameUseCase] does; the caller
 * routes that case there. Returns the name the person now has.
 */
interface RenamePersonUseCase {
    suspend operator fun invoke(account: String, personId: Long, newName: String?): String?
}
