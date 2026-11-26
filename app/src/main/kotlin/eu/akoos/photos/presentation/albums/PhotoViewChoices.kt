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

package eu.akoos.photos.presentation.albums

import eu.akoos.photos.domain.settings.Settings
import eu.akoos.photos.domain.settings.SettingsStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * A choice kept per album or folder, as the entries "ordinal:id" of a string set. Only the items whose
 * choice differs from the default have an entry, so the set grows with what the user changed and not
 * with the library.
 */
internal object PerItemChoice {

    /** The ordinal stored for [id], or null when it follows the default. */
    fun of(entries: Set<String>, id: String): Int? {
        for (entry in entries) {
            val cut = entry.indexOf(':')
            if (cut > 0 && entry.substring(cut + 1) == id) return entry.substring(0, cut).toIntOrNull()
        }
        return null
    }

    /** [entries] with [id] set to [ordinal]; choosing the default again drops its entry. */
    fun with(entries: Set<String>, id: String, ordinal: Int, defaultOrdinal: Int): Set<String> {
        val kept = entries.filterNot { entry ->
            val cut = entry.indexOf(':')
            cut > 0 && entry.substring(cut + 1) == id
        }.toMutableSet()
        if (ordinal != defaultOrdinal) kept.add("$ordinal:$id")
        return kept
    }
}

/**
 * How each album and each device folder lists its photos: the direction and the span of dates its
 * headers gather. A choice made on one belongs to that one alone; the album or folder without one
 * follows the default, which is the value the older single setting held.
 */
data class PhotoViewChoices(
    val albumSort: Set<String> = emptySet(),
    val albumSortDefault: AlbumPhotoSortMode = AlbumPhotoSortMode.Default,
    val albumGrouping: Set<String> = emptySet(),
    val albumGroupingDefault: AlbumPhotoGrouping = AlbumPhotoGrouping.Default,
    val folderSort: Set<String> = emptySet(),
    val folderSortDefault: AlbumPhotoSortMode = AlbumPhotoSortMode.Default,
    val folderGrouping: Set<String> = emptySet(),
    val folderGroupingDefault: AlbumPhotoGrouping = AlbumPhotoGrouping.Default,
) {
    fun sortOfAlbum(albumLinkId: String): AlbumPhotoSortMode =
        PerItemChoice.of(albumSort, albumLinkId)?.let { AlbumPhotoSortMode.fromOrdinal(it) } ?: albumSortDefault

    fun groupingOfAlbum(albumLinkId: String): AlbumPhotoGrouping =
        PerItemChoice.of(albumGrouping, albumLinkId)?.let { AlbumPhotoGrouping.fromOrdinal(it) } ?: albumGroupingDefault

    fun sortOfFolder(folderName: String): AlbumPhotoSortMode =
        PerItemChoice.of(folderSort, folderName)?.let { AlbumPhotoSortMode.fromOrdinal(it) } ?: folderSortDefault

    fun groupingOfFolder(folderName: String): AlbumPhotoGrouping =
        PerItemChoice.of(folderGrouping, folderName)?.let { AlbumPhotoGrouping.fromOrdinal(it) } ?: folderGroupingDefault

    companion object {
        fun observe(store: SettingsStore): Flow<PhotoViewChoices> = combine(
            store.observe(Settings.ALBUM_PHOTO_SORT_OVERRIDES),
            store.observe(Settings.ALBUM_PHOTO_SORT_MODE),
            store.observe(Settings.ALBUM_PHOTO_GROUPING_OVERRIDES),
            store.observe(Settings.ALBUM_PHOTO_GROUPING),
            store.observe(Settings.DEVICE_FOLDER_PHOTO_SORT_OVERRIDES),
            store.observe(Settings.DEVICE_FOLDER_PHOTO_SORT_MODE),
            store.observe(Settings.DEVICE_FOLDER_PHOTO_GROUPING_OVERRIDES),
            store.observe(Settings.DEVICE_FOLDER_PHOTO_GROUPING),
        ) { values ->
            PhotoViewChoices(
                albumSort = values[0] as Set<String>,
                albumSortDefault = AlbumPhotoSortMode.fromOrdinal(values[1] as Int),
                albumGrouping = values[2] as Set<String>,
                albumGroupingDefault = AlbumPhotoGrouping.fromOrdinal(values[3] as Int),
                folderSort = values[4] as Set<String>,
                folderSortDefault = AlbumPhotoSortMode.fromOrdinal(values[5] as Int),
                folderGrouping = values[6] as Set<String>,
                folderGroupingDefault = AlbumPhotoGrouping.fromOrdinal(values[7] as Int),
            )
        }
    }
}

/** Writes one album's or one folder's choice, leaving every other album and folder as it was. */
internal object PhotoViewChoiceStore {

    suspend fun setAlbumSort(store: SettingsStore, albumLinkId: String, mode: AlbumPhotoSortMode) {
        val overrides = store.get(Settings.ALBUM_PHOTO_SORT_OVERRIDES)
        val defaultOrdinal = AlbumPhotoSortMode.fromOrdinal(store.get(Settings.ALBUM_PHOTO_SORT_MODE)).ordinal
        store.set(
            Settings.ALBUM_PHOTO_SORT_OVERRIDES,
            PerItemChoice.with(overrides, albumLinkId, mode.ordinal, defaultOrdinal),
        )
    }

    suspend fun setAlbumGrouping(store: SettingsStore, albumLinkId: String, grouping: AlbumPhotoGrouping) {
        val overrides = store.get(Settings.ALBUM_PHOTO_GROUPING_OVERRIDES)
        val defaultOrdinal = AlbumPhotoGrouping.fromOrdinal(store.get(Settings.ALBUM_PHOTO_GROUPING)).ordinal
        store.set(
            Settings.ALBUM_PHOTO_GROUPING_OVERRIDES,
            PerItemChoice.with(overrides, albumLinkId, grouping.ordinal, defaultOrdinal),
        )
    }

    suspend fun setFolderSort(store: SettingsStore, folderName: String, mode: AlbumPhotoSortMode) {
        val overrides = store.get(Settings.DEVICE_FOLDER_PHOTO_SORT_OVERRIDES)
        val defaultOrdinal = AlbumPhotoSortMode.fromOrdinal(store.get(Settings.DEVICE_FOLDER_PHOTO_SORT_MODE)).ordinal
        store.set(
            Settings.DEVICE_FOLDER_PHOTO_SORT_OVERRIDES,
            PerItemChoice.with(overrides, folderName, mode.ordinal, defaultOrdinal),
        )
    }

    suspend fun setFolderGrouping(store: SettingsStore, folderName: String, grouping: AlbumPhotoGrouping) {
        val overrides = store.get(Settings.DEVICE_FOLDER_PHOTO_GROUPING_OVERRIDES)
        val defaultOrdinal = AlbumPhotoGrouping.fromOrdinal(store.get(Settings.DEVICE_FOLDER_PHOTO_GROUPING)).ordinal
        store.set(
            Settings.DEVICE_FOLDER_PHOTO_GROUPING_OVERRIDES,
            PerItemChoice.with(overrides, folderName, grouping.ordinal, defaultOrdinal),
        )
    }
}
