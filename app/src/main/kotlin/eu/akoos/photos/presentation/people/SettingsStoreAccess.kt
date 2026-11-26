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

package eu.akoos.photos.presentation.people

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import eu.akoos.photos.domain.settings.BootPreferences
import eu.akoos.photos.domain.settings.SettingsStore

@EntryPoint
@InstallIn(SingletonComponent::class)
interface PeopleSettingsStoreEntryPoint {
    fun settingsStore(): SettingsStore
    fun bootPreferences(): BootPreferences
}

@Composable
fun rememberSettingsStore(): SettingsStore {
    val context = LocalContext.current
    return remember(context) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            PeopleSettingsStoreEntryPoint::class.java,
        ).settingsStore()
    }
}

fun Context.settingsStore(): SettingsStore =
    EntryPointAccessors.fromApplication(
        applicationContext,
        PeopleSettingsStoreEntryPoint::class.java,
    ).settingsStore()

fun Context.bootPreferences(): BootPreferences =
    EntryPointAccessors.fromApplication(
        applicationContext,
        PeopleSettingsStoreEntryPoint::class.java,
    ).bootPreferences()
