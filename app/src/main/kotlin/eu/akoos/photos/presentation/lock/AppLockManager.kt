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

package eu.akoos.photos.presentation.lock

import eu.akoos.photos.domain.settings.Settings
import eu.akoos.photos.domain.settings.SettingsStore
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppLockManager @Inject constructor(
    private val settingsStore: SettingsStore,
) {
    val isLockEnabled: Flow<Boolean> = settingsStore.observe(Settings.APP_LOCK_ENABLED)

    /**
     * Timeout in seconds between backgrounding the app and re-locking it on resume.
     * 0 = lock immediately (the default before this option existed). Larger values let the user briefly
     * switch to other apps without re-authenticating on every return. A value saved under the old
     * minutes key migrates read-side (seconds = minutes * 60) when the seconds key is absent.
     */
    val lockTimeoutSeconds: Flow<Int> = combine(
        settingsStore.observe(Settings.APP_LOCK_TIMEOUT_SECONDS),
        settingsStore.observe(Settings.APP_LOCK_TIMEOUT_MINUTES),
    ) { seconds, minutes ->
        if (seconds != 0) seconds else (minutes?.times(60) ?: 0)
    }

    /** The enabled flag and the timeout in one read, for the in-memory cache of [AppLockSession]. */
    val settings: Flow<AppLockSettings> = combine(isLockEnabled, lockTimeoutSeconds) { enabled, timeout ->
        AppLockSettings(
            enabled = enabled,
            timeoutMs = timeout.toLong() * 1000L,
        )
    }.distinctUntilChanged()

    suspend fun setLockEnabled(enabled: Boolean) {
        settingsStore.set(Settings.APP_LOCK_ENABLED, enabled)
    }

    suspend fun setLockTimeoutSeconds(seconds: Int) {
        settingsStore.set(Settings.APP_LOCK_TIMEOUT_SECONDS, seconds.coerceAtLeast(0))
    }

    private val _resetLockTimestamps = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val resetLockTimestamps: SharedFlow<Unit> = _resetLockTimestamps.asSharedFlow()

    fun notifyResetLockTimestamps() {
        _resetLockTimestamps.tryEmit(Unit)
    }
}
