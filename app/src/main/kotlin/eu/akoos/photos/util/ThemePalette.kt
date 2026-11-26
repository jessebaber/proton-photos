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

package eu.akoos.photos.util

/**
 * Accent-color palette. Orthogonal to theme light/dark — picking Forest doesn't switch
 * between light and dark, only the accent tokens shift. [Default] is the original Proton
 * purple kept for backward compatibility.
 */
enum class ThemePalette(val storageKey: String) {
    Default("default"),
    Forest("forest"),
    Sunset("sunset"),
    Sea("sea"),
    Sepia("sepia"),
    Mono("mono"),
    Lavender("lavender"),
    Rose("rose"),
    Mint("mint"),
    Gold("gold"),
    Ruby("ruby");

    companion object {
        fun fromKey(key: String?): ThemePalette =
            entries.firstOrNull { it.storageKey == key } ?: Default
    }
}

/** Dark-mode accent ARGB for [palette] (primary, secondary). */
fun themeAccentArgbDark(palette: ThemePalette): Pair<Int, Int> = when (palette) {
    ThemePalette.Default -> 0xFF8B7CFF.toInt() to 0xFF6957D7.toInt()
    ThemePalette.Forest -> 0xFF7BC47F.toInt() to 0xFF3F8C44.toInt()
    ThemePalette.Sunset -> 0xFFFF8A65.toInt() to 0xFFE64A19.toInt()
    ThemePalette.Sea -> 0xFF4FC3F7.toInt() to 0xFF0288D1.toInt()
    ThemePalette.Sepia -> 0xFFD4A574.toInt() to 0xFF8B6F47.toInt()
    ThemePalette.Mono -> 0xFFE0E0E0.toInt() to 0xFF9E9E9E.toInt()
    ThemePalette.Lavender -> 0xFFC9A7EB.toInt() to 0xFF9B7FD4.toInt()
    ThemePalette.Rose -> 0xFFF48FB1.toInt() to 0xFFE06699.toInt()
    ThemePalette.Mint -> 0xFF6FE0C4.toInt() to 0xFF2FB89C.toInt()
    ThemePalette.Gold -> 0xFFFFD166.toInt() to 0xFFE9AE3C.toInt()
    ThemePalette.Ruby -> 0xFFFF7A88.toInt() to 0xFFE84C5E.toInt()
}

/** Light-mode accent ARGB for [palette] (primary, secondary). */
fun themeAccentArgbLight(palette: ThemePalette): Pair<Int, Int> = when (palette) {
    ThemePalette.Default -> 0xFF6957D7.toInt() to 0xFF4A37BC.toInt()
    ThemePalette.Forest -> 0xFF388E3C.toInt() to 0xFF1B5E20.toInt()
    ThemePalette.Sunset -> 0xFFE64A19.toInt() to 0xFFBF360C.toInt()
    ThemePalette.Sea -> 0xFF0288D1.toInt() to 0xFF01579B.toInt()
    ThemePalette.Sepia -> 0xFF8B6F47.toInt() to 0xFF5D4037.toInt()
    ThemePalette.Mono -> 0xFF424242.toInt() to 0xFF212121.toInt()
    ThemePalette.Lavender -> 0xFF9575CD.toInt() to 0xFF6A4CAF.toInt()
    ThemePalette.Rose -> 0xFFD6336C.toInt() to 0xFFA61E4D.toInt()
    ThemePalette.Mint -> 0xFF12A98C.toInt() to 0xFF0B7A66.toInt()
    ThemePalette.Gold -> 0xFFCC8A00.toInt() to 0xFF995F00.toInt()
    ThemePalette.Ruby -> 0xFFC62838.toInt() to 0xFF8E1B27.toInt()
}

/** Resolve the primary accent ARGB for [palette] in the given light/dark mode. */
fun paletteAccentArgb(palette: ThemePalette, isLight: Boolean): Int =
    if (isLight) themeAccentArgbLight(palette).first else themeAccentArgbDark(palette).first
