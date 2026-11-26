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

package eu.akoos.photos.data.vault

import eu.akoos.photos.data.hidden.HiddenVaultEditor
import eu.akoos.photos.domain.vault.HiddenVaultEdit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HiddenVaultEditImpl @Inject constructor(
    private val editor: HiddenVaultEditor,
) : HiddenVaultEdit {
    override suspend fun rename(uri: String, newName: String): String? = editor.rename(uri, newName)
    override suspend fun copy(uri: String, newName: String): String? = editor.copy(uri, newName)
    override suspend fun adoptCopy(sourceUri: String, copyUri: String, displayName: String) =
        editor.adoptCopy(sourceUri, copyUri, displayName)
    override suspend fun restampCaptureTime(uri: String, captureTimeMs: Long): String? =
        editor.restampCaptureTime(uri, captureTimeMs)
}
