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

package eu.akoos.photos.data.thumbnails

import eu.akoos.photos.data.repository.drive.ThumbnailDecryptScheduler
import eu.akoos.photos.domain.thumbnails.ThumbnailDecrypt
import me.proton.core.domain.entity.UserId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThumbnailDecryptImpl @Inject constructor(
    private val scheduler: ThumbnailDecryptScheduler,
) : ThumbnailDecrypt {
    override fun pinCovers(userId: UserId, linkIds: Collection<String>) = scheduler.pinCovers(userId, linkIds)
    override fun requestPinnedCover(userId: UserId, linkId: String) = scheduler.requestPinnedCover(userId, linkId)
    override fun cancel(linkId: String) = scheduler.cancel(linkId)
    override fun clear() = scheduler.clear()

    override suspend fun decryptThumbnailToFileBounded(
        userId: UserId,
        linkId: String,
        volumeId: String,
        serverUrl: String,
        serverToken: String?,
        contentKeyPacketBase64: String,
        encNodeKey: String,
        encNodePass: String,
        parentLinkId: String,
    ): String? = scheduler.decryptThumbnailToFileBounded(
        userId = userId,
        linkId = linkId,
        volumeId = volumeId,
        serverUrl = serverUrl,
        serverToken = serverToken,
        contentKeyPacketBase64 = contentKeyPacketBase64,
        encNodeKey = encNodeKey,
        encNodePass = encNodePass,
        parentLinkId = parentLinkId,
    )
}
