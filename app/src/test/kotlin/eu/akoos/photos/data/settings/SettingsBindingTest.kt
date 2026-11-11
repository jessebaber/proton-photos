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
import eu.akoos.photos.domain.settings.Setting
import eu.akoos.photos.domain.settings.Settings
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.Rule

class SettingsBindingTest {

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun everyCatalogSettingIsBoundExactlyOnceWithMatchingKeyName() {
        val store = SettingsBindings.create(tempStore())
        // Reading every catalog setting exercises the binding map.
        // build() already required every catalog entry is bound and names unique.
        assertEquals(Settings.all.map { it.name }.toSet().size, Settings.all.size)
        // Smoke: observe a few types without throwing.
        store.observe(Settings.AUTO_SYNC)
        store.observe(Settings.COMPRESS_UPLOAD_TIER)
        store.observe(Settings.THEME_MODE)
        assertTrue(Settings.all.isNotEmpty())
    }

    @Test
    fun unboundSettingFailsBuild() {
        val dataStore = tempStore()
        try {
            DataStoreSettingsStore.Builder(dataStore).build(
                listOf(Setting("not_bound_anywhere", false)),
            )
            fail("expected unbound setting to fail")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("not_bound_anywhere"))
        }
    }

    private fun tempStore() = PreferenceDataStoreFactory.create(
        produceFile = { File(tmp.newFolder(), "settings.preferences_pb") },
    )
}
