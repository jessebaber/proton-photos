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

package eu.akoos.photos.domain.usecase

import java.text.Normalizer
import java.util.Locale

/**
 * The one rule both apps tell people's names apart by, the desktop's `foldPersonName` too: spaces around a
 * name, how its letters are composed and their case are set aside, accents are kept. So "adam" and "Adam" are
 * one person, "Ádám" and "Adam" two.
 */
fun personNameKey(name: String): String = Normalizer.normalize(name.trim(), Normalizer.Form.NFC).lowercase(Locale.ROOT)

/** Whether [a] and [b] name the same person by [personNameKey]. */
fun samePersonName(a: String?, b: String?): Boolean = a != null && b != null && personNameKey(a) == personNameKey(b)

/**
 * Keeps one spelling per person while names come in from elsewhere (typed, or read from another device's
 * copy): the first spelling met for a name stands, so a later one differing only by [personNameKey] is
 * written the same way. Seed it with the names already held here, so they win.
 */
class PersonNameSpelling {
    private val byKey = HashMap<String, String>()

    fun hold(name: String) {
        byKey.putIfAbsent(personNameKey(name), name.trim())
    }

    fun of(name: String): String = byKey.getOrPut(personNameKey(name)) { name.trim() }

    fun ofOrNull(name: String?): String? = name?.let(::of)
}
