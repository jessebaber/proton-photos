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

import kotlin.math.sqrt

/**
 * People's mean faces for comparing one person against the others, read without loading the whole face
 * table. A person's centroid comes from the per-person cache the grouping keeps ([usableCachedCentroid])
 * when that still describes the person, and otherwise from that person's own faces alone, read a page at
 * a time ([exactCentroid]); the one person the caller compares against is always summed from its own
 * faces. Guest-safe: [account] is the user id or the local partition, as the face rows are keyed.
 */
interface PersonCentroids {
    suspend fun load(account: String, exactFor: Long?, wanted: (Long) -> Boolean): Map<Long, FloatArray>
    suspend fun exactCentroid(account: String, personId: Long): FloatArray?
}

/**
 * Sums face vectors into their mean direction: each readable vector (a stale-width one is skipped) is
 * added in double precision and the total L2-normalised once at the end, so a person's centroid comes out
 * the same whether its faces arrive in one list or a page at a time.
 */
class CentroidSum {
    private var sum: DoubleArray? = null

    fun add(embedding: ByteArray) {
        val e = unpackEmbedding(embedding)
        if (e.size != FACE_EMBEDDING_DIM) return
        val acc = sum ?: DoubleArray(e.size).also { sum = it }
        for (i in e.indices) acc[i] += e[i]
    }

    /** The normalised mean, or null when nothing readable was added. */
    fun centroid(): FloatArray? {
        val acc = sum ?: return null
        var norm = 0.0
        for (v in acc) norm += v * v
        val inv = if (norm > 0.0) 1.0 / sqrt(norm) else 0.0
        return FloatArray(acc.size) { (acc[it] * inv).toFloat() }
    }
}

/**
 * The people a named person's merge suggestion may offer, most alike first: every person in [centroids]
 * other than those in [skip] whose centroid's cosine to [target] is above [threshold]. The caller takes
 * the first that the user has not already turned down. Pure so it is unit-tested.
 */
fun rankSuggestionCandidates(
    target: FloatArray,
    centroids: Map<Long, FloatArray>,
    skip: Set<Long>,
    threshold: Float,
): List<Long> =
    centroids.entries
        .filter { it.key !in skip }
        .map { it.key to cosineSimilarity(target, it.value) }
        .filter { it.second > threshold }
        .sortedByDescending { it.second }
        .map { it.first }
