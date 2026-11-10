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

/**
 * Pure layer checker: given a path under `src/main/kotlin` and its source, returns the rule
 * violations that file currently has. Comments, KDoc and string literals do not count as references.
 */
object LayerRules {

    data class Violation(val rule: String, val relativePath: String)

    private val importRef = Regex("""import\s+(eu\.akoos\.photos\.[\w.]+)""")
    private val fqRef = Regex(
        """(?<![\w.])(eu\.akoos\.photos\.(?:data|worker|service|crypto|presentation|navigation|widget)(?:\.[\w]+)+)""",
    )

    private val presentationForbidden = mapOf(
        "eu.akoos.photos.data" to "presentation-data",
        "eu.akoos.photos.worker" to "presentation-worker",
        "eu.akoos.photos.service" to "presentation-service",
        "eu.akoos.photos.crypto" to "presentation-crypto",
    )
    private val dataForbidden = mapOf(
        "eu.akoos.photos.presentation" to "data-presentation",
        "eu.akoos.photos.navigation" to "data-navigation",
        "eu.akoos.photos.widget" to "data-widget",
    )
    private val utilForbidden = mapOf(
        "eu.akoos.photos.data" to "util-data",
        "eu.akoos.photos.worker" to "util-worker",
        "eu.akoos.photos.service" to "util-service",
        "eu.akoos.photos.crypto" to "util-crypto",
        "eu.akoos.photos.presentation" to "util-presentation",
        "eu.akoos.photos.navigation" to "util-navigation",
        "eu.akoos.photos.widget" to "util-widget",
    )

    fun check(relativePath: String, source: String): List<Violation> {
        val normalized = relativePath.replace('\\', '/')
        val underPhotos = normalized.removePrefix("eu/akoos/photos/").let {
            if (normalized.startsWith("eu/akoos/photos/")) it else normalized
        }
        // Accept both "eu/akoos/photos/…" and paths relative to that package root.
        val layerPath = if (normalized.startsWith("eu/akoos/photos/")) {
            underPhotos
        } else {
            normalized
        }
        val reportPath = if (normalized.startsWith("eu/akoos/photos/")) {
            normalized
        } else {
            "eu/akoos/photos/$normalized"
        }

        when {
            layerPath == "App.kt" || layerPath == "MainActivity.kt" || layerPath.startsWith("di/") ->
                return emptyList()
            layerPath.startsWith("domain/") ->
                return listOf(Violation("domain-location", reportPath))
            layerPath.startsWith("presentation/") ||
                layerPath.startsWith("navigation/") ||
                layerPath.startsWith("widget/") ->
                return packageViolations(reportPath, source, presentationForbidden)
            layerPath.startsWith("data/") ||
                layerPath.startsWith("worker/") ||
                layerPath.startsWith("service/") ||
                layerPath.startsWith("crypto/") ->
                return packageViolations(reportPath, source, dataForbidden)
            layerPath.startsWith("util/") ->
                return packageViolations(reportPath, source, utilForbidden)
            else -> return emptyList()
        }
    }

    private fun packageViolations(
        relativePath: String,
        source: String,
        forbidden: Map<String, String>,
    ): List<Violation> {
        val code = stripNoise(source)
        val roots = linkedSetOf<String>()
        for (ref in references(code)) {
            for ((pkg, rule) in forbidden) {
                if (ref == pkg || ref.startsWith("$pkg.")) {
                    roots.add(rule)
                }
            }
        }
        return roots.sorted().map { Violation(it, relativePath) }
    }

    /**
     * Strip block/line comments and string literals with a linear scanner.
     * Nested regex replace overflows the stack on large Kotlin sources.
     */
    private fun stripNoise(source: String): String {
        val out = StringBuilder(source.length)
        var i = 0
        while (i < source.length) {
            val c = source[i]
            val next = if (i + 1 < source.length) source[i + 1] else '\u0000'
            when {
                c == '/' && next == '/' -> {
                    i += 2
                    while (i < source.length && source[i] != '\n') i++
                }
                c == '/' && next == '*' -> {
                    i += 2
                    while (i + 1 < source.length && !(source[i] == '*' && source[i + 1] == '/')) i++
                    i = (i + 2).coerceAtMost(source.length)
                }
                c == '"' && next == '"' && i + 2 < source.length && source[i + 2] == '"' -> {
                    out.append("\"\"")
                    i += 3
                    while (i < source.length) {
                        if (source[i] == '"' &&
                            i + 2 < source.length &&
                            source[i + 1] == '"' &&
                            source[i + 2] == '"'
                        ) {
                            i += 3
                            break
                        }
                        i++
                    }
                }
                c == '"' -> {
                    out.append("\"\"")
                    i++
                    while (i < source.length) {
                        val ch = source[i]
                        if (ch == '\\') {
                            i = (i + 2).coerceAtMost(source.length)
                            continue
                        }
                        i++
                        if (ch == '"') break
                    }
                }
                c == '\'' -> {
                    out.append("''")
                    i++
                    while (i < source.length) {
                        val ch = source[i]
                        if (ch == '\\') {
                            i = (i + 2).coerceAtMost(source.length)
                            continue
                        }
                        i++
                        if (ch == '\'') break
                    }
                }
                else -> {
                    out.append(c)
                    i++
                }
            }
        }
        return out.toString()
    }

    private fun references(code: String): Set<String> {
        val found = linkedSetOf<String>()
        importRef.findAll(code).forEach { found.add(it.groupValues[1]) }
        fqRef.findAll(code).forEach { found.add(it.groupValues[1]) }
        return found
    }
}
