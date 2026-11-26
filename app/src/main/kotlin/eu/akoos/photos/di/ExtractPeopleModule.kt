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
import eu.akoos.photos.data.usecase.AddPhotosToPersonUseCaseImpl
import eu.akoos.photos.domain.usecase.AddPhotosToPersonUseCase
import eu.akoos.photos.data.usecase.AssignFacesToPersonUseCaseImpl
import eu.akoos.photos.domain.usecase.AssignFacesToPersonUseCase
import eu.akoos.photos.data.usecase.AssignPersonNameUseCaseImpl
import eu.akoos.photos.domain.usecase.AssignPersonNameUseCase
import eu.akoos.photos.data.usecase.IgnorePeopleUseCaseImpl
import eu.akoos.photos.domain.usecase.IgnorePeopleUseCase
import eu.akoos.photos.data.usecase.NotThisPersonUseCaseImpl
import eu.akoos.photos.domain.usecase.NotThisPersonUseCase
import eu.akoos.photos.data.usecase.ObservePeopleUseCaseImpl
import eu.akoos.photos.domain.usecase.ObservePeopleUseCase
import eu.akoos.photos.data.usecase.PersonCentroidsImpl
import eu.akoos.photos.domain.usecase.PersonCentroids
import eu.akoos.photos.data.usecase.RenamePersonUseCaseImpl
import eu.akoos.photos.domain.usecase.RenamePersonUseCase
import eu.akoos.photos.data.usecase.SemanticSearchUseCaseImpl
import eu.akoos.photos.domain.usecase.SemanticSearchUseCase
import eu.akoos.photos.data.usecase.ObservePlacesUseCaseImpl
import eu.akoos.photos.domain.usecase.ObservePlacesUseCase
import eu.akoos.photos.data.usecase.SameNamedPeopleMergeImpl
import eu.akoos.photos.domain.usecase.SameNamedPeopleMerge
import eu.akoos.photos.data.usecase.ImportFaceIndexUseCaseImpl
import eu.akoos.photos.domain.usecase.ImportFaceIndexUseCase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ExtractPeopleModule {

    @Binds
    @Singleton
    abstract fun bindAddPhotosToPersonUseCase(impl: AddPhotosToPersonUseCaseImpl): AddPhotosToPersonUseCase

    @Binds
    @Singleton
    abstract fun bindAssignFacesToPersonUseCase(impl: AssignFacesToPersonUseCaseImpl): AssignFacesToPersonUseCase

    @Binds
    @Singleton
    abstract fun bindAssignPersonNameUseCase(impl: AssignPersonNameUseCaseImpl): AssignPersonNameUseCase

    @Binds
    @Singleton
    abstract fun bindIgnorePeopleUseCase(impl: IgnorePeopleUseCaseImpl): IgnorePeopleUseCase

    @Binds
    @Singleton
    abstract fun bindNotThisPersonUseCase(impl: NotThisPersonUseCaseImpl): NotThisPersonUseCase

    @Binds
    @Singleton
    abstract fun bindObservePeopleUseCase(impl: ObservePeopleUseCaseImpl): ObservePeopleUseCase

    @Binds
    @Singleton
    abstract fun bindPersonCentroids(impl: PersonCentroidsImpl): PersonCentroids

    @Binds
    @Singleton
    abstract fun bindRenamePersonUseCase(impl: RenamePersonUseCaseImpl): RenamePersonUseCase

    @Binds
    @Singleton
    abstract fun bindSemanticSearchUseCase(impl: SemanticSearchUseCaseImpl): SemanticSearchUseCase

    @Binds
    @Singleton
    abstract fun bindObservePlacesUseCase(impl: ObservePlacesUseCaseImpl): ObservePlacesUseCase

    @Binds
    @Singleton
    abstract fun bindSameNamedPeopleMerge(impl: SameNamedPeopleMergeImpl): SameNamedPeopleMerge

    @Binds
    @Singleton
    abstract fun bindImportFaceIndexUseCase(impl: ImportFaceIndexUseCaseImpl): ImportFaceIndexUseCase
}
