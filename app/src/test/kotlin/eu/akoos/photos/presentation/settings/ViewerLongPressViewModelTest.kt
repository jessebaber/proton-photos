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

import android.content.Context
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import eu.akoos.photos.data.preferences.settingsDataStore
import eu.akoos.photos.data.settings.SettingsBindings
import eu.akoos.photos.domain.settings.Settings
import eu.akoos.photos.domain.settings.SettingsStore
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ViewerLongPressViewModelTest {

    private lateinit var context: Context
    private lateinit var settingsStore: SettingsStore
    private val built = ArrayList<ViewerLongPressViewModel>()

    private fun viewModel() = ViewerLongPressViewModel(context, settingsStore).also { built += it }

    private suspend fun awaitStored(expected: Boolean) = withTimeout(5_000) {
        while (settingsStore.get(Settings.VIEWER_LONG_PRESS_OPENS_MENU) != expected) delay(20)
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        context = ApplicationProvider.getApplicationContext()
        settingsStore = SettingsBindings.create(context.settingsDataStore)
        runBlocking { settingsStore.set(Settings.VIEWER_LONG_PRESS_OPENS_MENU, false) }
    }

    @After
    fun tearDown() {
        built.forEach { it.viewModelScope.cancel() }
        runBlocking { settingsStore.set(Settings.VIEWER_LONG_PRESS_OPENS_MENU, false) }
        Dispatchers.resetMain()
    }

    @Test
    fun `a fresh install keeps the long press on select text`() = runBlocking {
        assertFalse(settingsStore.get(Settings.VIEWER_LONG_PRESS_OPENS_MENU))
        val seen = CopyOnWriteArrayList<Boolean>()
        val collecting = launch(Dispatchers.Default) { viewModel().opensMenu.collect { seen += it } }
        delay(500)
        collecting.cancel()
        assertFalse(seen.last())
    }

    @Test
    fun `picking open the menu is stored and read back, and picking select text undoes it`() = runBlocking {
        val viewModel = viewModel()

        viewModel.set(opensMenu = true)
        awaitStored(true)
        assertTrue(withTimeout(5_000) { viewModel.opensMenu.first { it } })

        viewModel.set(opensMenu = false)
        awaitStored(false)
        assertFalse(withTimeout(5_000) { viewModel.opensMenu.first { !it } })
    }
}
