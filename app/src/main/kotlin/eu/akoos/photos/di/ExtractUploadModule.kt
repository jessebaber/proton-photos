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

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import eu.akoos.photos.data.usecase.FolderAlbumBackfillImpl
import eu.akoos.photos.data.usecase.ForceUploadLocalUrisUseCaseImpl
import eu.akoos.photos.data.usecase.InvalidateStrippedLocationsUseCaseImpl
import eu.akoos.photos.data.usecase.UploadPendingUseCaseImpl
import eu.akoos.photos.domain.usecase.FolderAlbumBackfill
import eu.akoos.photos.domain.usecase.ForceUploadLocalUrisUseCase
import eu.akoos.photos.domain.usecase.InvalidateStrippedLocationsUseCase
import eu.akoos.photos.domain.usecase.UploadPendingUseCase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ExtractUploadModule {

    @Binds
    @Singleton
    abstract fun bindUploadPendingUseCase(impl: UploadPendingUseCaseImpl): UploadPendingUseCase

    @Binds
    @Singleton
    abstract fun bindFolderAlbumBackfill(impl: FolderAlbumBackfillImpl): FolderAlbumBackfill

    @Binds
    @Singleton
    abstract fun bindForceUploadLocalUrisUseCase(
        impl: ForceUploadLocalUrisUseCaseImpl,
    ): ForceUploadLocalUrisUseCase

    @Binds
    @Singleton
    abstract fun bindInvalidateStrippedLocationsUseCase(
        impl: InvalidateStrippedLocationsUseCaseImpl,
    ): InvalidateStrippedLocationsUseCase
}
