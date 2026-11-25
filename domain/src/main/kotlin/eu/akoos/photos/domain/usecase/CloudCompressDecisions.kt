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

/** Whether a copy of [compressedBytes] is worth replacing an original of [originalBytes] with: at least
 *  [MIN_SAVING_PERCENT] percent smaller, so an already compact file is not swapped for a few bytes. Pure. */
fun worthReplacing(originalBytes: Long, compressedBytes: Long): Boolean =
    compressedBytes > 0L && originalBytes > 0L && compressedBytes * 100 <= originalBytes * (100 - MIN_SAVING_PERCENT)

/** The least saving, in percent of the original, that makes a compressed copy replace it. */
const val MIN_SAVING_PERCENT = 10
