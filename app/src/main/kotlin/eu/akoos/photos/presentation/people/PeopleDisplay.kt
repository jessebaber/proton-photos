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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.akoos.photos.domain.settings.Settings
import eu.akoos.photos.presentation.gallery.AlbumsViewMode
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

/** How the People page lays out its people: [columns] cards per row in a grid, or one row each in a list. */
data class PeopleDisplay(val viewMode: AlbumsViewMode, val columns: Int)

/** The cards-per-row choices the People page offers, the same as the Albums tab's cover sizes. */
internal val PeopleColumnChoices = listOf(2, 3, 4)

private fun columnsOf(stored: Int?): Int = stored?.takeIf { it in PeopleColumnChoices } ?: 2

/**
 * The People page's layout from raw DataStore preferences. Kept for call sites that still hold a
 * [Preferences] snapshot; keys match [Settings] names so defaults stay aligned.
 */
internal fun peopleDisplayOf(prefs: Preferences): PeopleDisplay {
    val follow = prefs[booleanPreferencesKey(Settings.PEOPLE_FOLLOW_ALBUMS.name)]
        ?: Settings.PEOPLE_FOLLOW_ALBUMS.default
    return if (follow) {
        PeopleDisplay(
            AlbumsViewMode.fromOrdinal(prefs[intPreferencesKey(Settings.ALBUMS_VIEW_MODE.name)]),
            columnsOf(prefs[intPreferencesKey(Settings.ALBUM_GRID_COLUMNS.name)]),
        )
    } else {
        ownPeopleDisplayOf(prefs)
    }
}

/** The People page's own layout, the one it takes while it does not follow the Albums tab. */
internal fun ownPeopleDisplayOf(prefs: Preferences): PeopleDisplay =
    PeopleDisplay(
        AlbumsViewMode.fromOrdinal(prefs[intPreferencesKey(Settings.PEOPLE_VIEW_MODE.name)]),
        columnsOf(prefs[intPreferencesKey(Settings.PEOPLE_GRID_COLUMNS.name)]),
    )

/**
 * Live People page layout. Opens on the Albums tab's stored layout, which the page follows by default,
 * so the common case lands in its final shape on the first frame.
 */
@Composable
fun rememberPeopleDisplay(): PeopleDisplay {
    val context = LocalContext.current
    val store = rememberSettingsStore()
    val boot = remember(context) { context.bootPreferences() }
    val initial = remember {
        PeopleDisplay(
            AlbumsViewMode.fromOrdinal(boot.albumsViewMode()),
            columnsOf(boot.albumGridColumns()),
        )
    }
    val display by remember(store) {
        combine(
            store.observe(Settings.PEOPLE_FOLLOW_ALBUMS),
            store.observe(Settings.ALBUMS_VIEW_MODE),
            store.observe(Settings.ALBUM_GRID_COLUMNS),
            store.observe(Settings.PEOPLE_VIEW_MODE),
            store.observe(Settings.PEOPLE_GRID_COLUMNS),
        ) { follow, albumsMode, albumsCols, peopleMode, peopleCols ->
            if (follow) {
                PeopleDisplay(AlbumsViewMode.fromOrdinal(albumsMode), columnsOf(albumsCols))
            } else {
                PeopleDisplay(AlbumsViewMode.fromOrdinal(peopleMode), columnsOf(peopleCols))
            }
        }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = initial)
    return display
}
