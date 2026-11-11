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

/** Typed settings facade for presentation, navigation, widget and util. */
interface SettingsStore {
    fun <T> observe(setting: Setting<T>): Flow<T>
    suspend fun <T> get(setting: Setting<T>): T
    suspend fun <T> set(setting: Setting<T>, value: T)
    suspend fun <T> update(setting: Setting<T>, transform: (T) -> T)
    suspend fun edit(block: SettingsEditor.() -> Unit)
}

/** Atomic multi-key writer used by [SettingsStore.edit]. */
interface SettingsEditor {
    fun <T> set(setting: Setting<T>, value: T)
}
