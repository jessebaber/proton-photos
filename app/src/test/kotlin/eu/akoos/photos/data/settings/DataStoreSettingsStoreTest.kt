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

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import eu.akoos.photos.data.preferences.SettingsKeys
import eu.akoos.photos.domain.settings.Settings
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DataStoreSettingsStoreTest {

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun roundTripBooleanAndNullableAndEdit() = runBlocking {
        val dataStore = PreferenceDataStoreFactory.create(
            produceFile = { File(tmp.newFolder(), "settings.preferences_pb") },
        )
        val store = SettingsBindings.create(dataStore)

        dataStore.edit { it[SettingsKeys.AUTO_SYNC] = false }
        assertEquals(false, store.get(Settings.AUTO_SYNC))

        store.set(Settings.AUTO_SYNC, true)
        assertEquals(true, dataStore.data.first()[SettingsKeys.AUTO_SYNC])

        assertNull(store.get(Settings.COMPRESS_UPLOAD_TIER))
        store.set(Settings.COMPRESS_UPLOAD_TIER, 2)
        assertEquals(2, store.get(Settings.COMPRESS_UPLOAD_TIER))
        store.set(Settings.COMPRESS_UPLOAD_TIER, null)
        assertNull(store.get(Settings.COMPRESS_UPLOAD_TIER))
        assertNull(dataStore.data.first()[SettingsKeys.COMPRESS_UPLOAD_TIER])

        store.edit {
            set(Settings.STRIP_GPS, true)
            set(Settings.STRIP_ON_UPLOAD, true)
        }
        assertEquals(true, store.get(Settings.STRIP_GPS))
        assertEquals(true, store.get(Settings.STRIP_ON_UPLOAD))
    }
}
