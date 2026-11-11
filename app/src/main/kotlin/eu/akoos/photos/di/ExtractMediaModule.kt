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

package eu.akoos.photos.di

import android.app.PendingIntent
import android.content.IntentSender
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import eu.akoos.photos.data.usecase.DeletePhotoUseCaseImpl
import eu.akoos.photos.data.usecase.DownloadPhotosUseCaseImpl
import eu.akoos.photos.data.usecase.FreeUpSpaceUseCaseImpl
import eu.akoos.photos.data.usecase.MoveToFolderUseCaseImpl
import eu.akoos.photos.data.usecase.ResolveCoverGifUseCaseImpl
import eu.akoos.photos.data.usecase.WriteLocalPhotoMetadataUseCaseImpl
import eu.akoos.photos.domain.usecase.DeletePhotoUseCase
import eu.akoos.photos.domain.usecase.DownloadPhotosUseCase
import eu.akoos.photos.domain.usecase.FreeUpSpaceUseCase
import eu.akoos.photos.domain.usecase.MoveToFolderUseCase
import eu.akoos.photos.domain.usecase.ResolveCoverGifUseCase
import eu.akoos.photos.domain.usecase.WriteLocalPhotoMetadataUseCase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ExtractMediaModule {

    @Binds
    @Singleton
    abstract fun bindDeletePhotoUseCase(
        impl: DeletePhotoUseCaseImpl,
    ): DeletePhotoUseCase<@JvmSuppressWildcards PendingIntent>

    @Binds
    @Singleton
    abstract fun bindMoveToFolderUseCase(
        impl: MoveToFolderUseCaseImpl,
    ): MoveToFolderUseCase<@JvmSuppressWildcards IntentSender>

    @Binds
    @Singleton
    abstract fun bindFreeUpSpaceUseCase(
        impl: FreeUpSpaceUseCaseImpl,
    ): FreeUpSpaceUseCase<@JvmSuppressWildcards PendingIntent>

    @Binds
    @Singleton
    abstract fun bindWriteLocalPhotoMetadataUseCase(
        impl: WriteLocalPhotoMetadataUseCaseImpl,
    ): WriteLocalPhotoMetadataUseCase<@JvmSuppressWildcards IntentSender>

    @Binds
    @Singleton
    abstract fun bindDownloadPhotosUseCase(impl: DownloadPhotosUseCaseImpl): DownloadPhotosUseCase

    @Binds
    @Singleton
    abstract fun bindResolveCoverGifUseCase(impl: ResolveCoverGifUseCaseImpl): ResolveCoverGifUseCase
}
