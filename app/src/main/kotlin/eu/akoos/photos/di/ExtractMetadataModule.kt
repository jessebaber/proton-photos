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
import eu.akoos.photos.data.usecase.CloudCompressQueueImpl
import eu.akoos.photos.data.usecase.CloudMetadataSaveControllerImpl
import eu.akoos.photos.data.usecase.CloudPhotoExifRewriter
import eu.akoos.photos.data.usecase.CloudPhotoExifRewriterImpl
import eu.akoos.photos.data.usecase.WriteCloudPhotoMetadataUseCaseImpl
import eu.akoos.photos.domain.usecase.CloudCompressQueue
import eu.akoos.photos.domain.usecase.CloudMetadataSaveController
import eu.akoos.photos.domain.usecase.WriteCloudPhotoMetadataUseCase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ExtractMetadataModule {

    @Binds
    @Singleton
    abstract fun bindCloudCompressQueue(impl: CloudCompressQueueImpl): CloudCompressQueue

    @Binds
    @Singleton
    abstract fun bindCloudMetadataSaveController(impl: CloudMetadataSaveControllerImpl): CloudMetadataSaveController

    @Binds
    @Singleton
    abstract fun bindWriteCloudPhotoMetadataUseCase(impl: WriteCloudPhotoMetadataUseCaseImpl): WriteCloudPhotoMetadataUseCase

    @Binds
    @Singleton
    abstract fun bindCloudPhotoExifRewriter(impl: CloudPhotoExifRewriterImpl): CloudPhotoExifRewriter
}
