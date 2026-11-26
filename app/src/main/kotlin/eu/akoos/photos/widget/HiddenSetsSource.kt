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

package eu.akoos.photos.widget

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.akoos.photos.domain.catalog.AlbumCatalog
import eu.akoos.photos.domain.catalog.SyncCatalog
import eu.akoos.photos.domain.catalog.SyncSnapshot
import eu.akoos.photos.domain.entity.LOCAL_PARTITION
import eu.akoos.photos.domain.settings.Settings
import eu.akoos.photos.domain.settings.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

/**
 * The hidden records the screens that show the library outside the app read: the widget setup and
 * the picker other apps open. Both draw where the gallery's own filters do not run, so both read the
 * same sources those filters read.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HiddenSetsSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val accountManager: AccountManager,
    private val albumCatalog: AlbumCatalog,
    private val syncCatalog: SyncCatalog,
    private val syncSnapshot: SyncSnapshot,
    private val settingsStore: SettingsStore,
) {

    /** The hidden sets of whoever is signed in, re-read whenever a hide or a reveal lands. */
    fun observe(): Flow<WidgetHiddenSets> =
        accountManager.getPrimaryUserId().flatMapLatest { userId -> observeFor(userId) }

    /**
     * Device photos whose Drive copy is hidden, from the sync rows that pair the two. Read only while
     * an account is signed in and something is hidden: a guest has no Drive copies to pair.
     */
    fun hiddenTwins(hidden: Flow<WidgetHiddenSets>): Flow<Set<String>> = accountManager.getPrimaryUserId()
        .flatMapLatest { userId ->
            if (userId == null) {
                flowOf(emptySet())
            } else {
                hidden
                    .map { it.cloudIds }
                    .distinctUntilChanged()
                    .flatMapLatest { hiddenCloudIds ->
                        if (hiddenCloudIds.isEmpty()) {
                            flowOf(emptySet())
                        } else {
                            syncSnapshot.observe(userId).map { snapshot ->
                                hiddenTwinUris(
                                    snapshot.rows.asSequence().map { it.localUri to it.cloudFileId },
                                    hiddenCloudIds,
                                )
                            }
                        }
                    }
            }
        }
        // A pass over the whole library's sync rows, kept off the main thread.
        .flowOn(Dispatchers.Default)

    /**
     * The hidden-photo, hidden-album and hidden-folder settings, the hidden albums' membership rows,
     * and the vault's sync rows. [userId] keys those rows, the local partition when no one is signed
     * in, so a guest's screen works on the device alone.
     */
    private fun observeFor(userId: UserId?): Flow<WidgetHiddenSets> {
        val account = userId?.id ?: LOCAL_PARTITION
        val albumMembers = settingsStore.observe(Settings.HIDDEN_ALBUM_IDS)
            .distinctUntilChanged()
            .flatMapLatest { albumIds ->
                if (albumIds.isEmpty()) flowOf(emptySet())
                else albumCatalog.observeAssociatedPhotoLinkIdsForAlbums(albumIds).map { it.toSet() }
            }
        // Every hide and reveal rewrites the vault's records in settings, so a settings change is when
        // the vault's rows are read again. The read is indexed down to the vault's handful of rows.
        val withVaultedRows = combine(
            settingsStore.observe(Settings.HIDDEN_CLOUD_PHOTO_IDS),
            settingsStore.observe(Settings.HIDDEN_URI_CLOUD_ID_MAP),
            settingsStore.observe(Settings.HIDDEN_ALBUM_IDS),
            settingsStore.observe(Settings.HIDDEN_FOLDER_NAMES),
        ) { cloudIds, uriMap, albumIds, folderNames ->
            Quadruple(cloudIds, uriMap, albumIds, folderNames)
        }.mapLatest { (cloudIds, uriMap, albumIds, folderNames) ->
            val vaulted = syncCatalog.getVaulted(account).mapNotNull { it.cloudFileId }
            SettingsHidden(cloudIds, uriMap, albumIds, folderNames) to vaulted
        }
        return combine(withVaultedRows, albumMembers) { (settings, vaultedRowCloudIds), members ->
            widgetHiddenSets(
                individuallyHidden = settings.cloudIds,
                hiddenUriCloudIdMap = settings.uriMap,
                hiddenAlbumIds = settings.albumIds,
                hiddenFolderNames = settings.folderNames,
                hiddenAlbumMembers = members,
                vaultedRowCloudIds = vaultedRowCloudIds,
            )
        }.distinctUntilChanged()
    }
}

private data class SettingsHidden(
    val cloudIds: Set<String>,
    val uriMap: Set<String>,
    val albumIds: Set<String>,
    val folderNames: Set<String>,
)

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
