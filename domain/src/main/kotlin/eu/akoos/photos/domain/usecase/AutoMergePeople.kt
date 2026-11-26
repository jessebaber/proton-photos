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

/** Centroid cosine an unnamed cluster must clear to be folded into a named person with no question asked: the
 *  join floor, so an automatic fold never takes in what the grouping itself would keep apart. The looser
 *  [FACE_SUGGEST_THRESHOLD] only offers a match for the user to confirm. */
const val AUTO_MERGE_THRESHOLD = FACE_CLUSTER_THRESHOLD

data class AutoMergePair(val fromClusterId: Long, val toPersonId: Long)

/**
 * Chooses the auto-merge folds for one pass. For every NAMED person it takes its single closest UNNAMED
 * candidate cluster whose centroid cosine clears [threshold], skipping the Unsorted bucket [otherId] and any (named, candidate) pair the user
 * already rejected as "not this person". A candidate that is closest to two named people is given to the
 * nearer one, and only unnamed candidates fold, so two names the user kept apart are never silently
 * combined and no fold chains within a pass. Pure: no database, no Android, so the selection is unit-tested.
 */
fun pickAutoMergePairs(
    centroids: Map<Long, FloatArray>,
    namedIds: Set<Long>,
    otherId: Long,
    rejectedPairs: Set<Pair<Long, Long>>,
    threshold: Float,
): List<AutoMergePair> {
    val candidateIds = centroids.keys.filter { it != otherId && it !in namedIds }
    if (candidateIds.isEmpty() || namedIds.isEmpty()) return emptyList()
    // candidateId -> (nearest named person so far, its similarity), so a candidate folds into one person only.
    val claim = HashMap<Long, Pair<Long, Float>>()
    for (namedId in namedIds) {
        val nc = centroids[namedId] ?: continue
        for (cid in candidateIds) {
            if ((namedId to cid) in rejectedPairs) continue
            val c = centroids[cid] ?: continue
            val s = cosineSimilarity(nc, c)
            if (s <= threshold) continue
            val prior = claim[cid]
            if (prior == null || s > prior.second) claim[cid] = namedId to s
        }
    }
    return claim.map { (cid, ns) -> AutoMergePair(cid, ns.first) }
}
