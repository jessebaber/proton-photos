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

package eu.akoos.photos.domain.offline

import java.io.File

/**
 * Pinned offline full-resolution blobs keyed by Drive linkId.
 * Paths are returned as [File] (JVM), never as Android Uri/Context.
 */
interface OfflineStorage {
    fun blobFile(linkId: String, ext: String): File
    fun findBlob(linkId: String): File?
    fun isOffline(linkId: String): Boolean
    suspend fun store(linkId: String, source: File): File
    fun delete(linkId: String)
    fun computeSizeBytes(): Long
    fun clearAll()
    suspend fun clearAllAndUnpin()
}
