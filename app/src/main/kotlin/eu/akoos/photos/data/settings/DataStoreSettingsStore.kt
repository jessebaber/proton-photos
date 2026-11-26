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

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import eu.akoos.photos.domain.settings.Setting
import eu.akoos.photos.domain.settings.SettingsEditor
import eu.akoos.photos.domain.settings.SettingsStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * [SettingsStore] backed by a [DataStore] of [Preferences]. Built from typed [Builder.bind]
 * calls so the compiler checks each catalog setting against its Preferences.Key.
 */
class DataStoreSettingsStore internal constructor(
    private val dataStore: DataStore<Preferences>,
    private val bindings: Map<String, Binding<*>>,
) : SettingsStore {

    override fun <T> observe(setting: Setting<T>): Flow<T> {
        val binding = bindingOf(setting)
        return dataStore.data.map { prefs -> binding.read(prefs) }
    }

    override suspend fun <T> get(setting: Setting<T>): T = observe(setting).first()

    override suspend fun <T> set(setting: Setting<T>, value: T) {
        val binding = bindingOf(setting)
        dataStore.edit { prefs -> binding.write(prefs, value) }
    }

    override suspend fun <T> update(setting: Setting<T>, transform: (T) -> T) {
        val binding = bindingOf(setting)
        dataStore.edit { prefs -> binding.write(prefs, transform(binding.read(prefs))) }
    }

    override suspend fun edit(block: SettingsEditor.() -> Unit) {
        dataStore.edit { prefs ->
            val editor = object : SettingsEditor {
                override fun <T> set(setting: Setting<T>, value: T) {
                    bindingOf(setting).write(prefs, value)
                }
            }
            editor.block()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> bindingOf(setting: Setting<T>): Binding<T> {
        val binding = bindings[setting.name]
            ?: error("Setting '${setting.name}' is not bound")
        return binding as Binding<T>
    }

    class Builder(private val dataStore: DataStore<Preferences>) {
        private val bindings = linkedMapOf<String, Binding<*>>()

        fun <T : Any> bind(setting: Setting<T>, key: Preferences.Key<T>): Builder {
            require(setting.name == key.name) {
                "Setting name '${setting.name}' must equal Preferences.Key name '${key.name}'"
            }
            require(setting.name !in bindings) { "Duplicate binding for '${setting.name}'" }
            bindings[setting.name] = Binding.NonNull(setting, key)
            return this
        }

        @JvmName("bindNullable")
        fun <T : Any> bind(setting: Setting<T?>, key: Preferences.Key<T>): Builder {
            require(setting.name == key.name) {
                "Setting name '${setting.name}' must equal Preferences.Key name '${key.name}'"
            }
            require(setting.name !in bindings) { "Duplicate binding for '${setting.name}'" }
            bindings[setting.name] = Binding.Nullable(setting, key)
            return this
        }

        fun build(catalog: Collection<Setting<*>>): DataStoreSettingsStore {
            val unbound = catalog.filter { it.name !in bindings }
            require(unbound.isEmpty()) {
                "Unbound settings: ${unbound.joinToString { it.name }}"
            }
            return DataStoreSettingsStore(dataStore, bindings.toMap())
        }
    }

    sealed class Binding<T> {
        abstract fun read(prefs: Preferences): T
        abstract fun write(prefs: MutablePreferences, value: T)

        class NonNull<T : Any>(
            private val setting: Setting<T>,
            private val key: Preferences.Key<T>,
        ) : Binding<T>() {
            override fun read(prefs: Preferences): T = prefs[key] ?: setting.default
            override fun write(prefs: MutablePreferences, value: T) {
                prefs[key] = value
            }
        }

        class Nullable<T : Any>(
            private val setting: Setting<T?>,
            private val key: Preferences.Key<T>,
        ) : Binding<T?>() {
            override fun read(prefs: Preferences): T? = prefs[key] ?: setting.default
            override fun write(prefs: MutablePreferences, value: T?) {
                if (value == null) prefs.remove(key) else prefs[key] = value
            }
        }
    }
}
