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

package eu.akoos.photos.data.catalog

import eu.akoos.photos.data.db.dao.AlbumPhotoMembershipDao
import eu.akoos.photos.data.db.dao.CloudAlbumDao
import eu.akoos.photos.domain.catalog.AlbumCatalog
import eu.akoos.photos.domain.catalog.CloudAlbumRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlbumCatalogImpl @Inject constructor(
    private val cloudAlbumDao: CloudAlbumDao,
    private val albumPhotoMembershipDao: AlbumPhotoMembershipDao,
) : AlbumCatalog {
    override fun observeOwnedAlbums(): Flow<List<CloudAlbumRecord>> =
        cloudAlbumDao.observeOwned().map { list ->
            list.map {
                CloudAlbumRecord(
                    linkId = it.linkId,
                    name = it.name,
                    photoCount = it.photoCount,
                    coverLinkId = it.coverLinkId,
                    lastActivityTimeMs = it.lastActivityTimeMs,
                    sharingShareId = it.sharingShareId,
                    volumeId = it.volumeId,
                    sharedByEmail = it.sharedByEmail,
                )
            }
        }

    override fun observeAssociatedPhotoLinkIdsForAlbums(albumIds: Set<String>): Flow<List<String>> =
        albumPhotoMembershipDao.observeAssociatedPhotoLinkIdsForAlbums(albumIds)

    override suspend fun membershipLinkIdsForAlbum(albumLinkId: String): List<String> =
        albumPhotoMembershipDao.getPhotoLinkIds(albumLinkId)
}
