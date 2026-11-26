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

package eu.akoos.photos.domain.vault

/** Parse/format helpers for Settings.HIDDEN_REMOTE_HIDE_QUEUE entries ("linkId|epochMs"). */
object RemoteHideQueue {
    fun parse(entries: Set<String>?): Map<String, Long> {
        if (entries.isNullOrEmpty()) return emptyMap()
        val out = HashMap<String, Long>(entries.size)
        for (entry in entries) {
            val sep = entry.lastIndexOf('|')
            if (sep <= 0 || sep == entry.lastIndex) continue
            val id = entry.substring(0, sep)
            val at = entry.substring(sep + 1).toLongOrNull() ?: continue
            if (id.isNotBlank()) out[id] = at
        }
        return out
    }

    fun format(times: Map<String, Long>): Set<String> =
        times.mapTo(HashSet(times.size)) { (key, at) -> "$key|$at" }
}
