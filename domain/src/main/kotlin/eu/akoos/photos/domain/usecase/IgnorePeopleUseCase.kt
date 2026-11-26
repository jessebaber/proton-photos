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
 * "This is not a person" for whole groups (a statue, a poster, a repeated false detection): every face of
 * each of the people is rejected so it never groups again, the same thing's look-alikes elsewhere in the
 * library go with them ([HideSimilarFacesUseCase]), a named person's name-keyed curation is dropped, and the
 * people are removed. The people the look-alikes came from are brought up to date. The look-alikes are found
 * first, by reading alone; every write then lands in one transaction, counted as one edit, so a failure part
 * way leaves the people as they were. Shared by the person page and the review screen. Returns how many
 * look-alikes were removed besides the people's own faces.
 */
interface IgnorePeopleUseCase {
    suspend operator fun invoke(account: String, personIds: Collection<Long>): Int
}
