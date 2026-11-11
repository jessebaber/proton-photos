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

package eu.akoos.photos.data.settings

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.akoos.photos.data.preferences.AlbumGridPrefsBoot
import eu.akoos.photos.data.preferences.LanguagePrefsBoot
import eu.akoos.photos.data.preferences.SeamlessGridPrefsBoot
import eu.akoos.photos.data.preferences.ThemePrefsBoot
import eu.akoos.photos.domain.settings.BootPreferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BootPreferencesImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : BootPreferences {
    override fun themeMode(): String = ThemePrefsBoot.read(context)
    override fun writeThemeMode(value: String) = ThemePrefsBoot.write(context, value)
    override fun languageTag(): String = LanguagePrefsBoot.read(context)
    override fun writeLanguageTag(value: String) = LanguagePrefsBoot.write(context, value)
    override fun seamlessGrid(): Boolean = SeamlessGridPrefsBoot.read(context)
    override fun writeSeamlessGrid(value: Boolean) = SeamlessGridPrefsBoot.write(context, value)
    override fun albumGridColumns(): Int = AlbumGridPrefsBoot.read(context)
    override fun writeAlbumGridColumns(value: Int) = AlbumGridPrefsBoot.write(context, value)
    override fun albumShowCollection(): Boolean = AlbumGridPrefsBoot.readShowCollection(context)
    override fun writeAlbumShowCollection(value: Boolean) =
        AlbumGridPrefsBoot.writeShowCollection(context, value)
    override fun albumsViewMode(): Int? = AlbumGridPrefsBoot.readViewMode(context)
    override fun writeAlbumsViewMode(value: Int) = AlbumGridPrefsBoot.writeViewMode(context, value)
}
