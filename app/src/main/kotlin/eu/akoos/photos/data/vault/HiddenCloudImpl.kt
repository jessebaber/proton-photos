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

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.akoos.photos.data.hidden.HiddenCloudPhotos
import eu.akoos.photos.domain.vault.HiddenCloud
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HiddenCloudImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : HiddenCloud {
    override suspend fun hide(linkIds: Collection<String>, atMs: Long?) =
        if (atMs == null) HiddenCloudPhotos.hide(context, linkIds)
        else HiddenCloudPhotos.hide(context, linkIds, atMs)
    override suspend fun reveal(linkIds: Collection<String>) = HiddenCloudPhotos.reveal(context, linkIds)
    override suspend fun hideAlbums(albumLinkIds: Collection<String>) =
        HiddenCloudPhotos.hideAlbums(context, albumLinkIds)
    override suspend fun revealAlbums(albumLinkIds: Collection<String>) =
        HiddenCloudPhotos.revealAlbums(context, albumLinkIds)
}
