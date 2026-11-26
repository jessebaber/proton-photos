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

package eu.akoos.photos.domain.label

import eu.akoos.photos.domain.entity.GalleryItem

/**
 * Where a photo's labels can sit. A Synced item may still hold labels under its device URI until a
 * re-key moves them to the Drive linkId.
 */
object LabelKeyReconciler {

    fun keysFor(item: GalleryItem): List<String> = when (item) {
        is GalleryItem.Synced ->
            if (item.local.uri == item.cloud.linkId) listOf(item.cloud.linkId)
            else listOf(item.cloud.linkId, item.local.uri)
        else -> listOf(item.stableId)
    }

    fun labelsFor(item: GalleryItem, byKey: Map<String, List<String>>): List<String> {
        val keys = keysFor(item)
        val primary = byKey[keys.first()].orEmpty()
        if (keys.size == 1) return primary
        val secondary = byKey[keys[1]].orEmpty()
        if (secondary.isEmpty()) return primary
        return mergeLabels(primary, secondary)
    }

    fun pendingMoves(items: Iterable<GalleryItem>, labelledKeys: Set<String>): List<Pair<String, String>> =
        items.mapNotNull { item ->
            if (item !is GalleryItem.Synced) return@mapNotNull null
            val uri = item.local.uri
            if (uri == item.cloud.linkId || uri !in labelledKeys) null else uri to item.cloud.linkId
        }

    private fun mergeLabels(primary: List<String>, secondary: List<String>): List<String> {
        if (secondary.isEmpty()) return primary
        val seen = primary.mapTo(HashSet(primary.size + secondary.size)) { it.lowercase() }
        val out = primary.toMutableList()
        for (label in secondary) {
            if (out.size >= MAX_LABELS) break
            val key = label.lowercase()
            if (key in seen) continue
            seen += key
            out += label
        }
        return out
    }

    const val MAX_LABELS = 5
}
