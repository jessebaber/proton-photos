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

package eu.akoos.photos.domain.thumbnails

import me.proton.core.domain.entity.UserId

/**
 * Presentation-facing thumbnail decrypt scheduling.
 * Accepts link ids rather than Room entities so the domain stays free of data.db types.
 */
interface ThumbnailDecrypt {
    fun pinCovers(userId: UserId, linkIds: Collection<String>)
    fun requestPinnedCover(userId: UserId, linkId: String)
    fun cancel(linkId: String)
    fun clear()

    /** Decrypt a trash/grid thumbnail into a local file URL; null on failure. */
    suspend fun decryptThumbnailToFileBounded(
        userId: UserId,
        linkId: String,
        volumeId: String,
        serverUrl: String,
        serverToken: String?,
        contentKeyPacketBase64: String,
        encNodeKey: String,
        encNodePass: String,
        parentLinkId: String,
    ): String?
}
