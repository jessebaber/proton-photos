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

package eu.akoos.photos.domain.settings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * The device folders whose photos also join a Drive album as they upload.
 * Presentation reads/writes this through [SettingsStore]; data keeps its own Preferences helpers.
 */
data class AlbumMirrorFolders(
    val optedIn: Set<String> = emptySet(),
    val optedOut: Set<String> = emptySet(),
    val allByDefault: Boolean = false,
) {
    operator fun contains(folder: String): Boolean =
        folder in optedIn || (allByDefault && folder !in optedOut)

    fun flipped(folder: String): AlbumMirrorFolders =
        if (folder in this) copy(optedIn = optedIn - folder, optedOut = optedOut + folder)
        else copy(optedIn = optedIn + folder, optedOut = optedOut - folder)
}

/** Album-mirror rule from the three settings keys. */
suspend fun SettingsStore.albumMirrorFolders(): AlbumMirrorFolders {
    val backupEverything = get(Settings.BACKUP_EVERYTHING)
    val allByDefault = get(Settings.ALBUM_ALL_BY_DEFAULT)
    return AlbumMirrorFolders(
        optedIn = get(Settings.ALBUM_OPT_IN_FOLDER_NAMES),
        optedOut = get(Settings.ALBUM_OPT_OUT_FOLDER_NAMES),
        allByDefault = backupEverything && allByDefault,
    )
}

/** Stores the two folder sets; the backup mode is not written. */
suspend fun SettingsStore.putAlbumMirrorFolders(rule: AlbumMirrorFolders) {
    edit {
        set(Settings.ALBUM_OPT_IN_FOLDER_NAMES, rule.optedIn)
        set(Settings.ALBUM_OPT_OUT_FOLDER_NAMES, rule.optedOut)
    }
}

/** Whether continuous backup is effectively armed. */
suspend fun SettingsStore.syncEffectivelyEnabled(): Boolean {
    val autoSync = get(Settings.AUTO_SYNC)
    val backupEverything = get(Settings.BACKUP_EVERYTHING)
    val folders = get(Settings.SYNC_FOLDER_NAMES)
    return autoSync && (backupEverything || !folders.isNullOrEmpty())
}

/** Live album-mirror rule for screens that combine it with other flows. */
fun SettingsStore.observeAlbumMirrorFolders(): Flow<AlbumMirrorFolders> =
    combine(
        observe(Settings.BACKUP_EVERYTHING),
        observe(Settings.ALBUM_ALL_BY_DEFAULT),
        observe(Settings.ALBUM_OPT_IN_FOLDER_NAMES),
        observe(Settings.ALBUM_OPT_OUT_FOLDER_NAMES),
    ) { backupEverything, allByDefault, optedIn, optedOut ->
        AlbumMirrorFolders(
            optedIn = optedIn,
            optedOut = optedOut,
            allByDefault = backupEverything && allByDefault,
        )
    }
