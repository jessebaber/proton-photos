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
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

/**
 * Walks `src/main/kotlin` (Gradle sets the working directory to `app/`) and compares live
 * violations with the ratcheting allowlist. The allowlist may only shrink.
 */
class ArchitectureBoundaryTest {

    @Test
    fun seededAllowlistMatchesTree() {
        val root = File("src/main/kotlin")
        assertTrue("expected $root (cwd is app/)", root.isDirectory)

        val actual = linkedSetOf<Pair<String, String>>()
        root.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { file ->
                val relative = file.relativeTo(root).path.replace('\\', '/')
                for (v in LayerRules.check(relative, file.readText())) {
                    actual.add(v.rule to v.relativePath)
                }
            }

        val allowed = loadAllowlist()
        val unexpected = actual - allowed
        val stale = allowed - actual

        if (unexpected.isNotEmpty() || stale.isNotEmpty()) {
            val message = buildString {
                if (unexpected.isNotEmpty()) {
                    appendLine("New violations (add a fix, do not grow the allowlist):")
                    unexpected.sortedBy { it.second }.forEach { appendLine("  ${it.first} ${it.second}") }
                }
                if (stale.isNotEmpty()) {
                    appendLine("Stale allowlist entries (remove them):")
                    stale.sortedBy { it.second }.forEach { appendLine("  ${it.first} ${it.second}") }
                }
            }
            fail(message)
        }
    }

    @Test
    fun ratchetRejectsStaleCleanEntry() {
        val cleanSource = """
            package eu.akoos.photos.presentation.gallery
            import eu.akoos.photos.domain.entity.GalleryItem
            class Clean
        """.trimIndent()
        val path = "eu/akoos/photos/presentation/gallery/Clean.kt"
        assertTrue(LayerRules.check(path, cleanSource).isEmpty())
        // A leftover allowlist line for a file that no longer violates must be treated as stale.
        val allowlisted = setOf("presentation-data" to path)
        val actual = emptySet<Pair<String, String>>()
        assertEquals(allowlisted, allowlisted - actual)
    }

    @Test
    fun ratchetRejectsDeletedFileEntry() {
        val allowlisted = setOf("domain-location" to "eu/akoos/photos/domain/usecase/Gone.kt")
        val actual = emptySet<Pair<String, String>>()
        val stale = allowlisted - actual
        assertEquals(allowlisted, stale)
    }

    private fun loadAllowlist(): Set<Pair<String, String>> {
        val stream = checkNotNull(
            javaClass.classLoader.getResourceAsStream("architecture/allowlist.txt"),
        ) { "missing architecture/allowlist.txt on the test classpath" }
        return stream.bufferedReader().useLines { lines ->
            lines
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") }
                .map { line ->
                    val space = line.indexOf(' ')
                    require(space > 0) { "allowlist line must be '<rule> <path>': $line" }
                    line.substring(0, space) to line.substring(space + 1)
                }
                .toSet()
        }
    }
}
