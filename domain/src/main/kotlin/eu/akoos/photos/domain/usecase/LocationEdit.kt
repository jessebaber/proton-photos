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

/** How the caller wants a cloud photo's GPS place changed. A capture-date-only edit passes
 *  [Unchanged] so the location block is left exactly as the file already holds it. */
sealed interface LocationEdit {

    /** The GPS block is not addressed and keeps whatever the file already holds. */
    data object Unchanged : LocationEdit

    /** The GPS block is removed. */
    data object Clear : LocationEdit

    /** The GPS block is set to [latitude] / [longitude]. */
    data class Set(val latitude: Double, val longitude: Double) : LocationEdit
}

/** The step a single cloud metadata save has reached, reported through the invoke callback so a
 *  progress surface can label where the replacement is (download and rewrite, upload, then album
 *  re-add and trash). */
enum class CloudSavePhase { PREPARING, UPLOADING, FINISHING }
