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

package eu.akoos.photos.architecture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LayerRulesTest {

    @Test
    fun cleanPresentationFilePasses() {
        val source = """
            package eu.akoos.photos.presentation.gallery
            import eu.akoos.photos.domain.entity.GalleryItem
            class GalleryScreen
        """.trimIndent()
        assertTrue(LayerRules.check("eu/akoos/photos/presentation/gallery/GalleryScreen.kt", source).isEmpty())
    }

    @Test
    fun compositionRootIsIgnored() {
        val source = """
            package eu.akoos.photos
            import eu.akoos.photos.data.db.AppDatabase
            class App
        """.trimIndent()
        assertTrue(LayerRules.check("eu/akoos/photos/App.kt", source).isEmpty())
        assertTrue(LayerRules.check("eu/akoos/photos/di/NetworkModule.kt", source).isEmpty())
    }

    @Test
    fun presentationImportOfDataFails() {
        val source = """
            package eu.akoos.photos.presentation.gallery
            import eu.akoos.photos.data.db.dao.PhotoListingDao
            class Bad
        """.trimIndent()
        val v = LayerRules.check("eu/akoos/photos/presentation/gallery/Bad.kt", source)
        assertEquals(listOf(LayerRules.Violation("presentation-data", "eu/akoos/photos/presentation/gallery/Bad.kt")), v)
    }

    @Test
    fun presentationFullyQualifiedWorkerFails() {
        val source = """
            package eu.akoos.photos.presentation.gallery
            fun run() { eu.akoos.photos.worker.SyncWorker.runNow() }
        """.trimIndent()
        val v = LayerRules.check("eu/akoos/photos/presentation/gallery/Bad.kt", source)
        assertEquals(listOf(LayerRules.Violation("presentation-worker", "eu/akoos/photos/presentation/gallery/Bad.kt")), v)
    }

    @Test
    fun dataImportingPresentationFails() {
        val source = """
            package eu.akoos.photos.data.gif
            import eu.akoos.photos.presentation.gifmaker.GifFrameTransform
            class GifExporter
        """.trimIndent()
        val v = LayerRules.check("eu/akoos/photos/data/gif/GifExporter.kt", source)
        assertEquals(listOf(LayerRules.Violation("data-presentation", "eu/akoos/photos/data/gif/GifExporter.kt")), v)
    }

    @Test
    fun utilImportingDataFails() {
        val source = """
            package eu.akoos.photos.util
            import eu.akoos.photos.data.preferences.SettingsKeys
            object SignOutCleanup
        """.trimIndent()
        val v = LayerRules.check("eu/akoos/photos/util/SignOutCleanup.kt", source)
        assertEquals(listOf(LayerRules.Violation("util-data", "eu/akoos/photos/util/SignOutCleanup.kt")), v)
    }

    @Test
    fun newDomainFileIsViolation() {
        val source = """
            package eu.akoos.photos.domain.usecase
            class BrandNew
        """.trimIndent()
        val v = LayerRules.check("eu/akoos/photos/domain/usecase/BrandNew.kt", source)
        assertEquals(listOf(LayerRules.Violation("domain-location", "eu/akoos/photos/domain/usecase/BrandNew.kt")), v)
    }

    @Test
    fun commentOnlyMentionIsIgnored() {
        val source = """
            package eu.akoos.photos.presentation.gallery
            // import eu.akoos.photos.data.db.AppDatabase
            class Clean
        """.trimIndent()
        assertTrue(LayerRules.check("eu/akoos/photos/presentation/gallery/Clean.kt", source).isEmpty())
    }

    @Test
    fun kdocOnlyMentionIsIgnored() {
        val source = """
            package eu.akoos.photos.presentation.gallery
            /** See [eu.akoos.photos.data.preferences.SettingsKeys]. */
            class Clean
        """.trimIndent()
        assertTrue(LayerRules.check("eu/akoos/photos/presentation/gallery/Clean.kt", source).isEmpty())
    }

    @Test
    fun stringLiteralMentionIsIgnored() {
        val source = """
            package eu.akoos.photos.presentation.gallery
            val mime = "vnd.android.cursor.dir/image"
            val note = "eu.akoos.photos.data.preferences.SettingsKeys"
            class Clean
        """.trimIndent()
        assertTrue(LayerRules.check("eu/akoos/photos/presentation/gallery/Clean.kt", source).isEmpty())
    }
}
