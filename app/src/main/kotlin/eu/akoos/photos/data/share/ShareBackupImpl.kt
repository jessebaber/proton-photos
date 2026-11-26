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

package eu.akoos.photos.data.share

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.akoos.photos.di.AppScope
import eu.akoos.photos.domain.repository.SyncStateRepository
import eu.akoos.photos.domain.share.ShareBackup
import eu.akoos.photos.domain.usecase.ForceUploadLocalUrisUseCase
import kotlinx.coroutines.CoroutineScope
import me.proton.core.domain.entity.UserId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShareBackupImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val syncStateRepo: SyncStateRepository,
    private val forceUpload: ForceUploadLocalUrisUseCase,
    @AppScope private val appScope: CoroutineScope,
) : ShareBackup {
    override suspend fun backUpSharedMedia(
        userId: UserId,
        sharedUris: List<String>,
        typeHint: String?,
    ): String = backUpSharedMedia(
        context = context,
        userId = userId,
        shared = sharedUris.map(Uri::parse),
        typeHint = typeHint,
        syncStateRepo = syncStateRepo,
        forceUpload = forceUpload,
        appScope = appScope,
    )
}
