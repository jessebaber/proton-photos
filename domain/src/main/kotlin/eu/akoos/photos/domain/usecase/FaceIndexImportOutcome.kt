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
 * Why an import ended, so the caller can tell the user exactly what happened. Every refusal folds in
 * no data; only [Success] has touched the account.
 */
sealed interface FaceIndexImportOutcome {
    /** Faces and named people were merged into the account. */
    data class Success(val faces: Int, val people: Int) : FaceIndexImportOutcome

    /** The file was built with a different embedding model, so its vectors were not imported, but the
     *  names it carries were staged to be put back on this device's own faces as the library is scanned. */
    data class ReattachScheduled(val people: Int) : FaceIndexImportOutcome

    /** The file belongs to a different Proton account, whose link ids and key do not match here. */
    data object WrongAccount : FaceIndexImportOutcome

    /** Not a face index, an unsupported newer version, or a body that would not decrypt or parse. */
    data object Unreadable : FaceIndexImportOutcome

    /** Storage is too full to import the file; nothing was written. */
    data object NotEnoughSpace : FaceIndexImportOutcome
}
