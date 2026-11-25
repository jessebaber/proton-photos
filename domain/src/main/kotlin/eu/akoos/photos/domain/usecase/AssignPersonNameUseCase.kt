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

/**
 * Assigns a person cluster a name: folds [fromPersonId] into the existing person named [rawName]
 * (that person absorbs its faces, relabelled so the choice survives a rebuild, and its name-keyed
 * curation re-keys across), then removes the now-empty source. If no person of that name exists, the
 * source is simply renamed to it. Returns the surviving person's id, or -1 on an empty name.
 *
 * This is the one place the fold is spelled out, shared by the person page's merge, its merge
 * suggestion, and the viewer's tap-a-face-to-name, so all three keep the exact same bookkeeping.
 */
interface AssignPersonNameUseCase {
    suspend operator fun invoke(account: String, fromPersonId: Long, rawName: String, confirm: Boolean = true): Long
}
