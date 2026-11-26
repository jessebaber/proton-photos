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

import me.proton.core.domain.entity.UserId

/**
 * Forces a set of local-only photo URIs to back up, even when their source folder is outside the
 * backup selection. For each URI it stamps an explicit upload intent on the sync_state row and kicks
 * an upload pass.
 */
interface ForceUploadLocalUrisUseCase {
    /** Force [uris] to back up with no album to join. Returns the number of URIs queued. */
    suspend fun forceUpload(userId: UserId, uris: List<String>): Int

    /** Force [uris] to back up and join [albumLinkId] once uploaded. Returns the number queued. */
    suspend fun queueForAlbum(userId: UserId, albumLinkId: String, uris: List<String>): Int
}
