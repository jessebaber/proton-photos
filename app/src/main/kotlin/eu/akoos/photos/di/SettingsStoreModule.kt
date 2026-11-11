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

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import eu.akoos.photos.data.preferences.settingsDataStore
import eu.akoos.photos.data.settings.BootPreferencesImpl
import eu.akoos.photos.data.settings.SettingsBindings
import eu.akoos.photos.domain.settings.BootPreferences
import eu.akoos.photos.domain.settings.SettingsStore
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SettingsStoreModule {
    @Binds
    @Singleton
    abstract fun bindBootPreferences(impl: BootPreferencesImpl): BootPreferences

    companion object {
        @Provides
        @Singleton
        fun provideSettingsStore(@ApplicationContext context: Context): SettingsStore =
            SettingsBindings.create(context.settingsDataStore)
    }
}
