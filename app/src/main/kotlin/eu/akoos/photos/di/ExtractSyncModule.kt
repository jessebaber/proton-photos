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
import eu.akoos.photos.data.usecase.GetGalleryItemsUseCaseImpl
import eu.akoos.photos.data.usecase.ReconcileSyncStateUseCaseImpl
import eu.akoos.photos.domain.usecase.GetGalleryItemsUseCase
import eu.akoos.photos.domain.usecase.ReconcileSyncStateUseCase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ExtractSyncModule {

    @Binds
    @Singleton
    abstract fun bindGetGalleryItemsUseCase(impl: GetGalleryItemsUseCaseImpl): GetGalleryItemsUseCase

    @Binds
    @Singleton
    abstract fun bindReconcileSyncStateUseCase(impl: ReconcileSyncStateUseCaseImpl): ReconcileSyncStateUseCase
}
