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

package eu.akoos.photos.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import eu.akoos.photos.domain.debug.DeviceSimState
import eu.akoos.photos.domain.debug.LargeLibrarySim
import eu.akoos.photos.domain.settings.Settings
import eu.akoos.photos.domain.settings.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LargeLibrarySimViewModel @Inject constructor(
    private val settingsStore: SettingsStore,
    private val simulator: LargeLibrarySim,
) : ViewModel() {

    data class UiState(
        val count: Int = 0,
        val running: Boolean = false,
        val message: String? = null,
        val deviceTarget: Int = 0,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    val deviceState: StateFlow<DeviceSimState> = simulator.deviceState

    init {
        viewModelScope.launch {
            val saved = settingsStore.get(Settings.SIM_LARGE_LIBRARY_COUNT)
            _state.update { it.copy(count = saved) }
        }
        simulator.refreshDeviceCount()
    }

    fun setDeviceTarget(target: Int) = _state.update { it.copy(deviceTarget = target.coerceAtLeast(0)) }

    fun generateDevicePhotos() = simulator.generateDevicePhotos(_state.value.deviceTarget)

    fun removeDevicePhotos() = simulator.removeDevicePhotos()

    fun cancelDeviceWork() = simulator.cancelDeviceWork()

    fun setCount(count: Int) = _state.update { it.copy(count = count.coerceAtLeast(0)) }

    fun populate() {
        if (_state.value.running) return
        viewModelScope.launch {
            val n = _state.value.count
            _state.update { it.copy(running = true, message = null) }
            val result = simulator.populate(n)
            settingsStore.set(Settings.SIM_LARGE_LIBRARY_COUNT, n)
            _state.update { it.copy(running = false, message = result.message) }
        }
    }

    fun clear() {
        if (_state.value.running) return
        viewModelScope.launch {
            _state.update { it.copy(running = true, message = null) }
            val result = simulator.clear()
            settingsStore.set(Settings.SIM_LARGE_LIBRARY_COUNT, 0)
            _state.update { it.copy(running = false, count = 0, message = result.message) }
        }
    }
}
