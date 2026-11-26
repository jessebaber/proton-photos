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

package eu.akoos.photos.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ApplicationProvider
import java.lang.reflect.Modifier
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import me.proton.core.domain.entity.UserId
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AccountScopedPreferencesTest {

    /** Device / install keys that must survive account wipe. */
    private val deviceKept: Set<Preferences.Key<*>> = buildSet {
        val fields = SettingsKeys::class.java.declaredFields
            .filter { Modifier.isStatic(it.modifiers) && Preferences.Key::class.java.isAssignableFrom(it.type) }
        for (field in fields) {
            field.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val key = field.get(null) as Preferences.Key<*>
            if (key !in AccountScopedPreferences.accountTiedKeys) add(key)
        }
    }

    @Test
    fun everySettingsKeysFieldIsClassified() {
        val keys = SettingsKeys::class.java.declaredFields
            .filter { Modifier.isStatic(it.modifiers) && Preferences.Key::class.java.isAssignableFrom(it.type) }
            .map { field ->
                field.isAccessible = true
                @Suppress("UNCHECKED_CAST")
                field.get(null) as Preferences.Key<*>
            }
        val classified = AccountScopedPreferences.accountTiedKeys + deviceKept
        val missing = keys.filter { it !in classified }
        if (missing.isNotEmpty()) {
            fail("Unclassified SettingsKeys: ${missing.joinToString { it.name }}")
        }
        // Negative: a fabricated key is neither account-tied nor device-kept.
        val fake = booleanPreferencesKey("not_a_real_settings_key")
        assertTrue(fake !in AccountScopedPreferences.accountTiedKeys)
        assertTrue(fake !in deviceKept)
    }

    @Test
    fun clearRemovesAccountTiedAndKeepsDeviceAndOtherUserDynamic() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val userA = UserId("user-a")
        val userB = UserId("user-b")
        context.settingsDataStore.edit { prefs ->
            prefs[SettingsKeys.BACKUP_EVERYTHING] = true
            prefs[SettingsKeys.SYNC_FOLDER_NAMES] = setOf("Camera")
            prefs[SettingsKeys.THEME_MODE] = "dark"
            prefs[SettingsKeys.ONBOARDING_COMPLETE] = true
            prefs[booleanPreferencesKey("event_anchor_${userA.id}_vol")] = true
            prefs[booleanPreferencesKey("event_anchor_${userB.id}_vol")] = true
            prefs[stringPreferencesKey("photo_listing_cursor_${userA.id}_vol")] = "cursor-a"
            prefs[stringPreferencesKey("photo_listing_cursor_${userB.id}_vol")] = "cursor-b"
        }

        AccountScopedPreferences.clear(context, userA)
        val after = context.settingsDataStore.data.first()
        assertNull(after[SettingsKeys.BACKUP_EVERYTHING])
        assertNull(after[SettingsKeys.SYNC_FOLDER_NAMES])
        assertTrue(after[SettingsKeys.THEME_MODE] == "dark")
        assertTrue(after[SettingsKeys.ONBOARDING_COMPLETE] == true)
        assertNull(after[booleanPreferencesKey("event_anchor_${userA.id}_vol")])
        assertTrue(after[booleanPreferencesKey("event_anchor_${userB.id}_vol")] == true)
        assertNull(after[stringPreferencesKey("photo_listing_cursor_${userA.id}_vol")])
        assertTrue(after[stringPreferencesKey("photo_listing_cursor_${userB.id}_vol")] == "cursor-b")
    }
}
