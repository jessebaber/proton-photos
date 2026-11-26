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

package eu.akoos.photos.data.downloads

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.akoos.photos.data.repository.drive.PhotoDownloadService
import eu.akoos.photos.domain.downloads.PhotoDownloads
import eu.akoos.photos.domain.entity.CloudPhoto
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhotoDownloadsImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : PhotoDownloads {
    override fun pruneStaleFullResCache(networkAvailable: Boolean) =
        PhotoDownloadService.pruneStaleFullResCache(context, networkAvailable)

    override fun isFullResCached(photo: CloudPhoto): Boolean =
        PhotoDownloadService.isFullResCached(context, photo)

    override fun fullResFile(photo: CloudPhoto): File? =
        PhotoDownloadService.fullResFile(context, photo)
}
