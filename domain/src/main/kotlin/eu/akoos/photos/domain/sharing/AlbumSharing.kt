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

package eu.akoos.photos.domain.sharing

import eu.akoos.photos.domain.entity.Album
import eu.akoos.photos.domain.entity.AlbumShareLink
import eu.akoos.photos.domain.entity.PendingInvitation
import eu.akoos.photos.domain.entity.ShareExternalInvitation
import eu.akoos.photos.domain.entity.ShareInvitation
import eu.akoos.photos.domain.entity.ShareMember
import eu.akoos.photos.domain.entity.SharedPhoto
import kotlinx.coroutines.flow.StateFlow
import me.proton.core.domain.entity.UserId

sealed interface SaveToLibraryState {
    data object Idle : SaveToLibraryState
    data class Running(
        val sourceAlbumLinkId: String,
        val copied: Int,
        val total: Int,
    ) : SaveToLibraryState
    data class Done(
        val sourceAlbumLinkId: String,
        val newAlbumLinkId: String,
        val copiedCount: Int,
        val failedCount: Int,
        val totalRequested: Int,
    ) : SaveToLibraryState
    data class Failed(
        val sourceAlbumLinkId: String,
        val reason: String?,
    ) : SaveToLibraryState
    data class Cancelled(
        val sourceAlbumLinkId: String,
        val copied: Int,
        val total: Int,
    ) : SaveToLibraryState
}

/** Album and photo sharing operations presentation drives. */
interface AlbumSharing {
    val saveToLibraryState: StateFlow<SaveToLibraryState>

    suspend fun createAlbumShareLink(userId: UserId, albumLinkId: String): AlbumShareLink
    suspend fun createPhotoShareLink(userId: UserId, photoLinkId: String): String
    suspend fun getPhotoShareLink(userId: UserId, photoLinkId: String): String?
    suspend fun revokePhotoShareLink(userId: UserId, photoLinkId: String)
    suspend fun setPhotoLinkPassword(userId: UserId, photoLinkId: String, password: String?): String
    suspend fun revokeShareUrlOnly(userId: UserId, shareId: String)
    suspend fun changeMemberPermission(userId: UserId, shareId: String, memberId: String, permissions: Int)
    suspend fun changeInvitationPermission(userId: UserId, shareId: String, invitationId: String, permissions: Int)
    suspend fun inviteToAlbum(
        userId: UserId,
        albumLinkId: String,
        email: String,
        permissions: Int,
    ): String
    suspend fun deleteShare(userId: UserId, shareId: String)
    suspend fun leaveSharedAlbum(userId: UserId, shareId: String, albumLinkId: String)
    suspend fun loadSharedWithMeAlbums(userId: UserId): List<Album>
    suspend fun prefetchSharedAlbumCovers(userId: UserId, albums: List<Album>)
    suspend fun loadSharedByMePhotos(userId: UserId): List<SharedPhoto>
    suspend fun loadShareInvitations(userId: UserId, shareId: String): List<ShareInvitation>
    suspend fun revokeShareInvitation(userId: UserId, shareId: String, invitationId: String)
    suspend fun listExternalInvitations(userId: UserId, shareId: String): List<ShareExternalInvitation>
    suspend fun revokeExternalInvitation(userId: UserId, shareId: String, invitationId: String)
    suspend fun loadShareMembers(userId: UserId, shareId: String): List<ShareMember>
    suspend fun removeShareMember(userId: UserId, shareId: String, memberId: String)
    suspend fun loadPendingInvitations(userId: UserId): List<PendingInvitation>
    suspend fun declineInvitation(userId: UserId, invitationId: String)
    suspend fun acceptInvitation(userId: UserId, invitationId: String)
    fun startSaveSharedAlbumToOwnLibrary(
        userId: UserId,
        sharingShareId: String,
        sourceAlbumLinkId: String,
        sourceAlbumDecryptedName: String,
        sourceVolumeId: String,
    )
    fun cancelSaveToLibrary()
    fun acknowledgeSaveToLibraryResult()
}
