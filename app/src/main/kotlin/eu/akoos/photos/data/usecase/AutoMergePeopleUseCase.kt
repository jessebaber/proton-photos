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

package eu.akoos.photos.data.usecase

import androidx.room.withTransaction
import eu.akoos.photos.data.db.AppDatabase
import eu.akoos.photos.data.db.dao.FaceDao
import eu.akoos.photos.data.db.dao.NotPersonDao
import eu.akoos.photos.data.db.dao.PersonDao
import eu.akoos.photos.domain.usecase.AUTO_MERGE_THRESHOLD
import eu.akoos.photos.domain.usecase.AssignPersonNameUseCase
import eu.akoos.photos.domain.usecase.FACE_EMBEDDING_DIM
import eu.akoos.photos.domain.usecase.pickAutoMergePairs
import eu.akoos.photos.domain.usecase.unpackEmbedding
import eu.akoos.photos.util.SQL_CHUNK_SIZE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext
import kotlin.math.sqrt

/**
 * Applies, automatically, the closest of the merges the manual "this may be the same person" card would
 * offer: each named person absorbs its closest look-alike unnamed cluster at or past the join floor
 * ([AUTO_MERGE_THRESHOLD]). Opt-in (the scheduler only calls this when the user turned auto-merge on), and
 * it reuses the ordinary [AssignPersonNameUseCase] fold without confirming the folded faces, so a rebuild
 * can still take apart a fold the user never looked at. Runs one pass (closest single candidate per person,
 * no chaining); a later indexing pass catches any further matches. Guest-safe: [account] is the user id or
 * the local partition, exactly as clustering keys it.
 */
@Singleton
class AutoMergePeopleUseCase @Inject constructor(
    private val faceDao: FaceDao,
    private val personDao: PersonDao,
    private val notPersonDao: NotPersonDao,
    private val assignPersonName: AssignPersonNameUseCase,
    private val appDatabase: AppDatabase,
) {
    /**
     * Folds the chosen candidates and returns how many clusters were merged (0 when nothing qualifies). The
     * folds land in one transaction and only while [writeAllowed] holds, asked inside it, so nothing the
     * caller fences against (an import, a sign-out) can commit between the check and the folds; a refused
     * pass folds nothing and returns 0.
     */
    suspend operator fun invoke(
        account: String,
        writeAllowed: () -> Boolean = { true },
    ): Int = withContext(Dispatchers.Default) {
        val named = personDao.namedPeopleForUser(account).filter { !it.displayName.isNullOrBlank() }
        if (named.isEmpty()) return@withContext 0
        val namedIds = named.mapTo(HashSet()) { it.id }
        val nameById = named.associate { it.id to it.displayName!!.trim() }
        val input = readInput(account, nameById)
        if (input.centroids.isEmpty()) return@withContext 0
        val otherId = personDao.otherPersonId(account) ?: -1L

        val pairs = pickAutoMergePairs(input.centroids, namedIds, otherId, input.rejectedPairs, AUTO_MERGE_THRESHOLD)
        if (pairs.isEmpty()) return@withContext 0
        appDatabase.withTransaction {
            if (!writeAllowed()) return@withTransaction 0
            for (pair in pairs) {
                val name = nameById[pair.toPersonId] ?: continue
                // Fold the candidate into the named person (by name); the surviving side is the named person.
                // Its faces are not confirmed: the app chose this fold, so a later rebuild may still undo it.
                assignPersonName(account, pair.fromClusterId, name, confirm = false)
            }
            pairs.size
        }
    }

    /**
     * Every person's L2-normalised mean embedding over their kept faces, mirroring the manual suggestion
     * path, and the (named person, candidate) pairs the user rejected. The faces' ids and people are read
     * first, clearest detection first; the vectors then follow by id a slice at a time and are summed in
     * that same order, so the centroids match a whole-table read bit for bit while only one slice of
     * vectors is ever held.
     */
    internal suspend fun readInput(account: String, nameById: Map<Long, String>): AutoMergeInput {
        val marks = notPersonDao.allForUser(account)
        val markedFaceIds = marks.mapTo(HashSet()) { it.faceId }
        val links = faceDao.keptPersonLinksByScoreDesc(account)
        val personOfMarkedFace = HashMap<String, Long>()
        val sums = HashMap<Long, DoubleArray>()
        for (start in links.indices step SQL_CHUNK_SIZE) {
            val slice = links.subList(start, minOf(start + SQL_CHUNK_SIZE, links.size))
            val vectors = HashMap<String, ByteArray>(slice.size * 2)
            for (v in faceDao.vectorsByIdsChunk(account, slice.map { it.id })) vectors[v.id] = v.embedding
            for (link in slice) {
                if (link.id in markedFaceIds) personOfMarkedFace[link.id] = link.personId
                val e = unpackEmbedding(vectors[link.id] ?: continue)
                if (e.size != FACE_EMBEDDING_DIM) continue
                val acc = sums.getOrPut(link.personId) { DoubleArray(e.size) }
                for (i in e.indices) acc[i] += e[i]
            }
            coroutineContext.ensureActive()
        }
        val centroids = HashMap<Long, FloatArray>()
        for ((pid, acc) in sums) {
            var norm = 0.0
            for (v in acc) norm += v * v
            val inv = if (norm > 0.0) 1.0 / sqrt(norm) else 0.0
            centroids[pid] = FloatArray(acc.size) { (acc[it] * inv).toFloat() }
        }

        // (named person, candidate cluster) pairs the user rejected: a candidate holding a face marked
        // "not this person" under that person's name is never re-offered, so it is not auto-merged either.
        val rejectedByName = marks.groupBy({ it.personName }, { it.faceId })
        val rejectedPairs = HashSet<Pair<Long, Long>>()
        for ((namedId, name) in nameById) {
            val rejected = rejectedByName[name] ?: continue
            for (faceId in rejected) {
                val cid = personOfMarkedFace[faceId] ?: continue
                if (cid != namedId && cid in centroids) rejectedPairs.add(namedId to cid)
            }
        }
        return AutoMergeInput(centroids, rejectedPairs)
    }
}

/** What one auto-merge pass chooses from: each person's centroid and the pairs the user rejected. */
internal class AutoMergeInput(
    val centroids: Map<Long, FloatArray>,
    val rejectedPairs: Set<Pair<Long, Long>>,
)
