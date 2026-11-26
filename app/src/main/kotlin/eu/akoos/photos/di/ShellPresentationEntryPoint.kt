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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import eu.akoos.photos.domain.catalog.AlbumCatalog
import eu.akoos.photos.domain.catalog.PhotoCatalog
import eu.akoos.photos.domain.catalog.SyncCatalog
import eu.akoos.photos.domain.catalog.SyncSnapshot
import eu.akoos.photos.domain.settings.SettingsStore
import eu.akoos.photos.domain.thumbnails.ThumbnailDecrypt
import eu.akoos.photos.domain.upload.LocalOverwrite
import eu.akoos.photos.domain.work.BackgroundWork

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ShellPresentationEntryPoint {
    fun settingsStore(): SettingsStore
    fun backgroundWork(): BackgroundWork
    fun localOverwrite(): LocalOverwrite
    fun thumbnailDecrypt(): ThumbnailDecrypt
    fun albumCatalog(): AlbumCatalog
    fun photoCatalog(): PhotoCatalog
    fun syncCatalog(): SyncCatalog
    fun syncSnapshot(): SyncSnapshot
}

fun shellPresentationEntryPoint(context: Context): ShellPresentationEntryPoint =
    EntryPointAccessors.fromApplication(
        context.applicationContext,
        ShellPresentationEntryPoint::class.java,
    )

fun Context.settingsStore(): SettingsStore =
    shellPresentationEntryPoint(this).settingsStore()

@Composable
fun rememberSettingsStore(): SettingsStore {
    val context = LocalContext.current
    return remember(context) { context.settingsStore() }
}
