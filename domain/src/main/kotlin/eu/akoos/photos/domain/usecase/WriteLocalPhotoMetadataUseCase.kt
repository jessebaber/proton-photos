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

/** The outcome of a metadata write against a local (device) photo. [C] is the consent payload. */
sealed interface MetadataWriteResult<out C> {
    data object Success : MetadataWriteResult<Nothing>

    data class NeedsPermission<C>(val intentSender: C) : MetadataWriteResult<C>

    data class Failed(val reason: String) : MetadataWriteResult<Nothing>
}

sealed interface TextTagEdit {
    data object Unchanged : TextTagEdit
    data class SetTo(val value: String) : TextTagEdit
}

private val MVHD_STAMPABLE_MIMES = setOf("video/mp4", "video/quicktime", "video/3gpp", "video/3gpp2")

/**
 * Writes edited capture-date and location metadata back onto a LOCAL photo. [C] is the platform
 * write-consent payload (IntentSender in the Android binding).
 */
interface WriteLocalPhotoMetadataUseCase<C> {
    suspend fun writeCaptureDate(uri: String, newDateTakenMs: Long): MetadataWriteResult<C>
    suspend fun writeLocation(uri: String, latitude: Double, longitude: Double): MetadataWriteResult<C>
    suspend fun clearLocation(uri: String): MetadataWriteResult<C>
    suspend fun writeDescriptiveText(
        uri: String,
        description: TextTagEdit = TextTagEdit.Unchanged,
        artist: TextTagEdit = TextTagEdit.Unchanged,
        copyright: TextTagEdit = TextTagEdit.Unchanged,
    ): MetadataWriteResult<C>

    companion object {
        fun isVideoMime(mimeType: String): Boolean =
            normalizeMime(mimeType).startsWith("video/")

        fun isExifWritableImageMime(mimeType: String): Boolean {
            val n = normalizeMime(mimeType)
            return n == "image/jpeg" || n == "image/png" || n == "image/webp"
        }

        fun isMvhdStampableMime(mimeType: String): Boolean =
            normalizeMime(mimeType) in MVHD_STAMPABLE_MIMES

        fun dateLivesOnlyInColumn(mimeType: String): Boolean =
            !isExifWritableImageMime(mimeType) && !isMvhdStampableMime(mimeType)

        fun textTagWrites(edits: List<Pair<String, TextTagEdit>>): List<Pair<String, String?>> =
            edits.mapNotNull { (tag, edit) ->
                if (edit !is TextTagEdit.SetTo) return@mapNotNull null
                tag to ExifAsciiText.transliterate(edit.value).takeIf { it.isNotEmpty() }
            }

        private fun normalizeMime(mimeType: String): String =
            mimeType.substringBefore(';').trim().lowercase(Locale.ROOT)
    }
}

object ExifAsciiText {
    const val MAX_LENGTH = 512
    private val NONSPACING_MARKS = Regex("\\p{Mn}+")
    private val WHITESPACE_RUN = Regex("\\s+")
    private val PRINTABLE_ASCII = 0x20..0x7E

    fun transliterate(value: String): String =
        Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace(NONSPACING_MARKS, "")
            .replace(WHITESPACE_RUN, " ")
            .filter { it.code in PRINTABLE_ASCII }
            .replace(WHITESPACE_RUN, " ")
            .trim()
            .take(MAX_LENGTH)
            .trimEnd()
}
