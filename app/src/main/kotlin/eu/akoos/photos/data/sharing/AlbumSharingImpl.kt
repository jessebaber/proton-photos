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

package eu.akoos.photos.data.sharing

import eu.akoos.photos.data.repository.drive.AlbumSharingService
import eu.akoos.photos.di.AppScope
import eu.akoos.photos.domain.entity.Album
import eu.akoos.photos.domain.entity.AlbumShareLink
import eu.akoos.photos.domain.entity.PendingInvitation
import eu.akoos.photos.domain.entity.ShareExternalInvitation
import eu.akoos.photos.domain.entity.ShareInvitation
import eu.akoos.photos.domain.entity.ShareMember
import eu.akoos.photos.domain.entity.SharedPhoto
import eu.akoos.photos.domain.sharing.AlbumSharing
import eu.akoos.photos.domain.sharing.SaveToLibraryState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import me.proton.core.domain.entity.UserId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlbumSharingImpl @Inject constructor(
    private val service: AlbumSharingService,
    @AppScope private val appScope: CoroutineScope,
) : AlbumSharing {
    override val saveToLibraryState: StateFlow<SaveToLibraryState> = service.saveToLibraryState
        .map { it.toDomain() }
        .stateIn(appScope, SharingStarted.Eagerly, SaveToLibraryState.Idle)

    override suspend fun createAlbumShareLink(userId: UserId, albumLinkId: String): AlbumShareLink =
        service.createAlbumShareLink(userId, albumLinkId)
    override suspend fun createPhotoShareLink(userId: UserId, photoLinkId: String): String =
        service.createPhotoShareLink(userId, photoLinkId)
    override suspend fun getPhotoShareLink(userId: UserId, photoLinkId: String): String? =
        service.getPhotoShareLink(userId, photoLinkId)
    override suspend fun revokePhotoShareLink(userId: UserId, photoLinkId: String) =
        service.revokePhotoShareLink(userId, photoLinkId)
    override suspend fun setPhotoLinkPassword(userId: UserId, photoLinkId: String, password: String?): String =
        service.setPhotoLinkPassword(userId, photoLinkId, password)
    override suspend fun revokeShareUrlOnly(userId: UserId, shareId: String) =
        service.revokeShareUrlOnly(userId, shareId)
    override suspend fun changeMemberPermission(userId: UserId, shareId: String, memberId: String, permissions: Int) =
        service.changeMemberPermission(userId, shareId, memberId, permissions)
    override suspend fun changeInvitationPermission(userId: UserId, shareId: String, invitationId: String, permissions: Int) =
        service.changeInvitationPermission(userId, shareId, invitationId, permissions)
    override suspend fun inviteToAlbum(userId: UserId, albumLinkId: String, email: String, permissions: Int): String =
        service.inviteToAlbum(userId, albumLinkId, email, permissions)
    override suspend fun deleteShare(userId: UserId, shareId: String) = service.deleteShare(userId, shareId)
    override suspend fun leaveSharedAlbum(userId: UserId, shareId: String, albumLinkId: String) =
        service.leaveSharedAlbum(userId, shareId, albumLinkId)
    override suspend fun loadSharedWithMeAlbums(userId: UserId): List<Album> = service.loadSharedWithMeAlbums(userId)
    override suspend fun prefetchSharedAlbumCovers(userId: UserId, albums: List<Album>) =
        service.prefetchSharedAlbumCovers(userId, albums)
    override suspend fun loadSharedByMePhotos(userId: UserId): List<SharedPhoto> = service.loadSharedByMePhotos(userId)
    override suspend fun loadShareInvitations(userId: UserId, shareId: String): List<ShareInvitation> =
        service.loadShareInvitations(userId, shareId)
    override suspend fun revokeShareInvitation(userId: UserId, shareId: String, invitationId: String) =
        service.revokeShareInvitation(userId, shareId, invitationId)
    override suspend fun listExternalInvitations(userId: UserId, shareId: String): List<ShareExternalInvitation> =
        service.listExternalInvitations(userId, shareId)
    override suspend fun revokeExternalInvitation(userId: UserId, shareId: String, invitationId: String) =
        service.revokeExternalInvitation(userId, shareId, invitationId)
    override suspend fun loadShareMembers(userId: UserId, shareId: String): List<ShareMember> =
        service.loadShareMembers(userId, shareId)
    override suspend fun removeShareMember(userId: UserId, shareId: String, memberId: String) =
        service.removeShareMember(userId, shareId, memberId)
    override suspend fun loadPendingInvitations(userId: UserId): List<PendingInvitation> =
        service.loadPendingInvitations(userId)
    override suspend fun declineInvitation(userId: UserId, invitationId: String) =
        service.declineInvitation(userId, invitationId)
    override suspend fun acceptInvitation(userId: UserId, invitationId: String) =
        service.acceptInvitation(userId, invitationId)
    override fun startSaveSharedAlbumToOwnLibrary(
        userId: UserId,
        sharingShareId: String,
        sourceAlbumLinkId: String,
        sourceAlbumDecryptedName: String,
        sourceVolumeId: String,
    ) = service.startSaveSharedAlbumToOwnLibrary(
        userId, sharingShareId, sourceAlbumLinkId, sourceAlbumDecryptedName, sourceVolumeId,
    )
    override fun cancelSaveToLibrary() = service.cancelSaveToLibrary()
    override fun acknowledgeSaveToLibraryResult() = service.acknowledgeSaveToLibraryResult()

    private fun AlbumSharingService.SaveToLibraryState.toDomain(): SaveToLibraryState = when (this) {
        AlbumSharingService.SaveToLibraryState.Idle -> SaveToLibraryState.Idle
        is AlbumSharingService.SaveToLibraryState.Running -> SaveToLibraryState.Running(
            sourceAlbumLinkId, copied, total,
        )
        is AlbumSharingService.SaveToLibraryState.Done -> SaveToLibraryState.Done(
            sourceAlbumLinkId, newAlbumLinkId, copiedCount, failedCount, totalRequested,
        )
        is AlbumSharingService.SaveToLibraryState.Failed -> SaveToLibraryState.Failed(
            sourceAlbumLinkId, reason,
        )
        is AlbumSharingService.SaveToLibraryState.Cancelled -> SaveToLibraryState.Cancelled(
            sourceAlbumLinkId, copied, total,
        )
    }
}
