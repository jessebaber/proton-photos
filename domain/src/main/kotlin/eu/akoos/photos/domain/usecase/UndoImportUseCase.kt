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

import me.proton.core.domain.entity.UserId
import java.util.Locale

/**
 * Counts for one undo of an import run: [undone] photos moved to the Drive trash, [keptChanged] photos
 * left in place because their bytes no longer match what the import uploaded, and [failed] photos that
 * could not be verified or that the trash call rejected.
 */
data class UndoResult(
    val undone: Int,
    val keptChanged: Int,
    val failed: Int,
)

/**
 * Reverses one import run by moving the photos it uploaded to the Drive trash, but only the photos
 * whose bytes still match what the run sent. Each candidate's current cloud ContentHash is read straight
 * from Drive and compared to the hash the stored upload sha1 maps to; a photo the user has since
 * replaced or edited no longer matches and is kept. Every removal is a recoverable trash move, never a
 * permanent delete, and any uncertainty (a link the fetch could not read, an underivable expected hash,
 * a hash that differs) resolves to keeping the photo, so an undo can never take a file the run did not
 * upload or a newer version the user chose to keep.
 */
interface UndoImportUseCase {
    suspend fun undo(userId: UserId, runId: String): UndoResult
}

/**
 * How an undo classifies its candidate rows, split off from the IO so the safety decision is a pure
 * value a test can pin. [toTrash] holds the links cleared for the recoverable trash move, [changed] the
 * ones kept because the user replaced the bytes, and [unverifiable] the ones kept because no reliable
 * hash was available to compare. A link with no current detail at all is treated as already gone and
 * appears in none of the three.
 */
data class ImportUndoPlan(
    val toTrash: List<String>,
    val changed: List<String>,
    val unverifiable: List<String>,
)

/**
 * The ledger fields an undo or history summary needs: [linkId] for counting, [name] for Live Photo
 * pairing, and [alreadyInDrive] for the skipped badge. Kept free of Room so presentation and tests
 * can call the pure helpers without a data-layer import.
 */
data class ImportLedgerRow(
    val linkId: String,
    val name: String? = null,
    val alreadyInDrive: Boolean = false,
)

/**
 * Decides per link what an undo does with it, using only content-hash equality so the rule stays pure
 * and testable. A link is cleared for [ImportUndoPlan.toTrash] only when its expected hash and the
 * current cloud hash are both present and equal: identical bytes, safe to trash. A present-but-different
 * current hash means the user replaced the photo, so the link is kept as [ImportUndoPlan.changed]. A link
 * whose current hash is missing (the link resolved but carried none) or whose expected hash could not be
 * derived is kept as [ImportUndoPlan.unverifiable], because trashing on an unconfirmed match could take
 * a photo the run never uploaded. A link entirely absent from [currentByLink] is treated as already gone
 * and left out of every bucket.
 */
fun planImportUndo(
    linkIds: List<String>,
    expectedByLink: Map<String, String?>,
    currentByLink: Map<String, String?>,
): ImportUndoPlan {
    val toTrash = mutableListOf<String>()
    val changed = mutableListOf<String>()
    val unverifiable = mutableListOf<String>()
    for (linkId in linkIds) {
        if (!currentByLink.containsKey(linkId)) continue
        val current = currentByLink[linkId]
        val expected = expectedByLink[linkId]
        when {
            current == null || expected == null -> unverifiable += linkId
            expected == current -> toTrash += linkId
            else -> changed += linkId
        }
    }
    return ImportUndoPlan(toTrash, changed, unverifiable)
}

/**
 * What an undo reports, counted in photos: a Live Photo once, although its video has a ledger row and a
 * link of its own and is trashed with it.
 */
fun undoResultOf(
    rows: List<ImportLedgerRow>,
    plan: ImportUndoPlan,
    trashed: Set<String>,
    failedTrash: Set<String>,
    trashCallFailed: Boolean,
): UndoResult {
    val photoLinks = ledgerPhotos(rows).mapTo(HashSet()) { it.linkId }
    fun Collection<String>.photos() = count { it in photoLinks }
    return UndoResult(
        undone = trashed.photos(),
        keptChanged = plan.changed.photos(),
        failed = failedTrash.photos() + plan.unverifiable.photos() + (if (trashCallFailed) plan.toTrash.photos() else 0),
    )
}

private val LEDGER_STILL = Regex("\\.(heic|heif|jpe?g)$", RegexOption.IGNORE_CASE)
private val LEDGER_MOTION = Regex("\\.(mov|mp4)$", RegexOption.IGNORE_CASE)

/**
 * A run's ledger [rows] as photos: the video of a Live Photo has a row of its own, under its still's name
 * with the video's extension, so a video sharing its name with a still of the same outcome is that
 * still's and is left out. Each file keeps its own date in the ledger, which can differ between the two
 * halves, so the name alone decides.
 */
fun ledgerPhotos(rows: List<ImportLedgerRow>): List<ImportLedgerRow> {
    fun stem(row: ImportLedgerRow) = row.name.orEmpty().substringBeforeLast('.').lowercase(Locale.ROOT)
    val stills = rows.filter { LEDGER_STILL.containsMatchIn(it.name.orEmpty()) }.groupBy { stem(it) to it.alreadyInDrive }
    val taken = HashSet<ImportLedgerRow>()
    return rows.filterNot { row ->
        if (!LEDGER_MOTION.containsMatchIn(row.name.orEmpty())) return@filterNot false
        val still = stills[stem(row) to row.alreadyInDrive]
            ?.firstOrNull { it !in taken }
            ?: return@filterNot false
        taken += still
        true
    }
}
