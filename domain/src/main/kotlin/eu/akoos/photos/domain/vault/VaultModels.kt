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

package eu.akoos.photos.domain.vault

/** Progress of a multi-photo hide or restore into/out of the vault. */
data class HiddenFolderProgress(
    val done: Int,
    val total: Int,
    val restoring: Boolean,
)

/** What the user picks on sign-out for vaulted photos. */
enum class SignOutVaultChoice {
    DELETE,
    MOVE_OUT,
}

/** Constants naming the on-device rescue folder for vault leftovers. */
object HiddenRescueNames {
    const val FOLDER = "Photos for Proton Rescue"
    const val SIGN_OUT_FOLDER = "Photos for Proton Sign-out"
}
