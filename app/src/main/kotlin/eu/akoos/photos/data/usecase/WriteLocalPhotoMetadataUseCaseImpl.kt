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

import android.app.RecoverableSecurityException
import android.content.ContentValues
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.akoos.photos.data.upload.LocalOverwriteGuard
import eu.akoos.photos.domain.usecase.ExifAsciiText
import eu.akoos.photos.domain.usecase.MetadataWriteResult
import eu.akoos.photos.domain.usecase.TextTagEdit
import eu.akoos.photos.domain.usecase.WriteLocalPhotoMetadataUseCase
import eu.akoos.photos.util.ExifDateFormat
import eu.akoos.photos.util.ExifHelper
import eu.akoos.photos.util.MetadataStripConfig
import eu.akoos.photos.util.Mp4CreationTime
import eu.akoos.photos.util.StripResult
import eu.akoos.photos.util.mimeFromPath
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Normalizer
import java.time.ZoneId
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "WriteLocalMetadata"

/**
 * Writes edited capture-date and location metadata back onto a LOCAL (device / MediaStore) photo. GPS
 * lives in EXIF; the capture date is written to both the MediaStore DATE_TAKEN column (the fast index
 * galleries read) and into the file itself (the embedded EXIF datetime for an image, the mvhd creation
 * time for MP4-family video), so the two sources agree for a photo the user just changed rather than
 * drifting apart on the next scan.
 *
 * On Android 10+ a write against a file the app does not own raises a recoverable [SecurityException];
 * the write then returns [MetadataWriteResult.NeedsPermission<IntentSender>] carrying the system consent request the
 * caller launches before retrying. Every write runs on [Dispatchers.IO].
 */
@Singleton
class WriteLocalPhotoMetadataUseCaseImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : WriteLocalPhotoMetadataUseCase<IntentSender> {

    /**
     * Sets the capture date to [newDateTakenMs]. Updates the MediaStore DATE_TAKEN column for any photo
     * or video; additionally writes the date into the file itself wherever the container allows it, so
     * the index and the file do not diverge. A container with nowhere to put it updates the column only
     * and still succeeds.
     *
     * An app-private file has no row and so no column, and it is a photo the vault holds, which keeps
     * its date in its file NAME and reads it from there everywhere. So the name is the write that
     * decides such a photo's date, the caller performs it on a landed write, and a container with
     * nothing to stamp inside it still lands: the date has somewhere to go either way.
     */
    override suspend fun writeCaptureDate(uri: String, newDateTakenMs: Long): MetadataWriteResult<IntentSender> =
        withContext(Dispatchers.IO) {
            val parsed = Uri.parse(uri)
            val mime = resolveMime(parsed)
            // The value inside the file is what a media scan re-derives the DATE_TAKEN column from, so
            // it goes first: the embedded EXIF datetime for an image, the mvhd creation time for
            // MP4-family video. The column write below keeps the fast index in step at once. HEIC and
            // the containers with neither leg keep the column value only.
            val inFile = when {
                WriteLocalPhotoMetadataUseCase.isExifWritableImageMime(mime) -> writeExifCaptureDate(uri, newDateTakenMs)
                WriteLocalPhotoMetadataUseCase.isMvhdStampableMime(mime) -> stampVideoCaptureDate(uri, newDateTakenMs)
                else -> null
            }
            if (inFile != null && inFile !is MetadataWriteResult.Success) return@withContext inFile
            // A file this app owns has no MediaStore row behind it, so there is no column to keep in
            // step. It is a vaulted photo, whose date the vault keeps in the file name and prefers to
            // the file's own metadata everywhere it reads one, so that name is the write that counts:
            // a HEIC, a GIF or a Matroska video has nothing to stamp inside it and still saves.
            //
            // The test is ownership, not the scheme. A `file://` uri under shared storage does have a
            // row, and answering Success there would report a date as saved that reached neither the
            // file nor the column.
            if (isAppOwnedFile(parsed)) return@withContext MetadataWriteResult.Success
            updateDateTakenColumn(parsed, newDateTakenMs)
        }

    /**
     * Rewrites every EXIF datetime field so nothing left in the file can contradict [newDateTakenMs].
     * The three datetime tags take the new wall clock. EXIF datetime carries no zone of its own, so the
     * three offset tags take the offset of the zone that wall clock is read in; a leftover offset from
     * the original capture would otherwise put a reader that honours it at a different absolute instant.
     * The sub-second tags are cleared rather than rewritten, because a date picked by hand has no
     * sub-second part and any surviving digits would describe the previous capture.
     */
    private fun writeExifCaptureDate(uri: String, newDateTakenMs: Long): MetadataWriteResult<IntentSender> {
        val zone = ZoneId.systemDefault()
        val stamp = ExifDateFormat.toExifLocal(newDateTakenMs, zone)
        val offset = ExifDateFormat.toExifOffset(newDateTakenMs, zone)
        return editExifViaFd(uri) { exif ->
            exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, stamp)
            exif.setAttribute(ExifInterface.TAG_DATETIME_DIGITIZED, stamp)
            exif.setAttribute(ExifInterface.TAG_DATETIME, stamp)
            exif.setAttribute(ExifInterface.TAG_OFFSET_TIME_ORIGINAL, offset)
            exif.setAttribute(ExifInterface.TAG_OFFSET_TIME_DIGITIZED, offset)
            exif.setAttribute(ExifInterface.TAG_OFFSET_TIME, offset)
            exif.setAttribute(ExifInterface.TAG_SUBSEC_TIME_ORIGINAL, null)
            exif.setAttribute(ExifInterface.TAG_SUBSEC_TIME_DIGITIZED, null)
            exif.setAttribute(ExifInterface.TAG_SUBSEC_TIME, null)
        }
    }

    /**
     * Stamps the video's mvhd creation time with [newDateTakenMs], the value MediaStore re-derives
     * DATE_TAKEN from on the next scan. Goes through the same "rw" descriptor as the EXIF leg, so a
     * foreign file raises the same consent request instead of failing. The stamp itself is best effort:
     * it only overwrites fixed-width timestamp fields and stops at the first box it cannot read, so a
     * container it cannot patch leaves the file untouched and the column value stands, which is the
     * behaviour of a container with no mvhd at all.
     */
    private fun stampVideoCaptureDate(uri: String, newDateTakenMs: Long): MetadataWriteResult<IntentSender> =
        try {
            val pfd = context.contentResolver.openFileDescriptor(Uri.parse(uri), "rw")
                ?: return MetadataWriteResult.Failed("could not open $uri")
            val stamped = pfd.use { Mp4CreationTime.stamp(it.fileDescriptor, newDateTakenMs) }
            if (!stamped) Log.w(TAG, "mvhd stamp did not complete for $uri")
            MetadataWriteResult.Success
        } catch (e: CancellationException) {
            throw e
        } catch (e: SecurityException) {
            needsPermissionFor(uri, e)
        } catch (e: Exception) {
            Log.w(TAG, "mvhd stamp failed for $uri: ${e.message}")
            MetadataWriteResult.Failed(e.message ?: "video date write failed")
        }

    /** Writes [latitude] / [longitude] into the photo's GPS EXIF. Image-only: a video returns
     *  [MetadataWriteResult.Failed] because it carries no EXIF GPS block. */
    override suspend fun writeLocation(uri: String, latitude: Double, longitude: Double): MetadataWriteResult<IntentSender> =
        withContext(Dispatchers.IO) {
            if (WriteLocalPhotoMetadataUseCase.isVideoMime(resolveMime(Uri.parse(uri)))) {
                return@withContext MetadataWriteResult.Failed(LOCATION_IMAGE_ONLY)
            }
            // setLatLong sets the coordinate plus its N/S and E/W reference tags, matching
            // ExifHelper.writeGpsLocation; the "rw" descriptor is what a foreign MediaStore file needs
            // (a raw path write is refused under scoped storage) and is where consent is demanded.
            editExifViaFd(uri) { exif -> exif.setLatLong(latitude, longitude) }
        }

    /** Removes the GPS EXIF block. Image-only, mirroring [writeLocation]. Reuses the audited GPS tag
     *  set from [ExifHelper.stripFieldsInPlace] so no location tag is left behind. */
    override suspend fun clearLocation(uri: String): MetadataWriteResult<IntentSender> =
        withContext(Dispatchers.IO) {
            if (WriteLocalPhotoMetadataUseCase.isVideoMime(resolveMime(Uri.parse(uri)))) {
                return@withContext MetadataWriteResult.Failed(LOCATION_IMAGE_ONLY)
            }
            when (ExifHelper.stripFieldsInPlace(context, uri, MetadataStripConfig(stripGps = true))) {
                is StripResult.Stripped -> MetadataWriteResult.Success
                is StripResult.NeedsPermission -> needsPermissionFor(uri, cause = null)
                is StripResult.Failed -> MetadataWriteResult.Failed("could not clear location")
            }
        }

    /**
     * Writes the descriptive EXIF text tags (ImageDescription, Artist, Copyright) in ONE descriptor
     * pass. Each tag defaults to [TextTagEdit.Unchanged] and is then absent from the pass entirely, so
     * a caller editing the artist leaves the description exactly as it is; [TextTagEdit.SetTo] with a
     * blank value clears the tag instead.
     *
     * EXIF defines these three as ASCII fields and ExifInterface encodes them as US-ASCII, which turns
     * every unmappable character into a question mark. Each value therefore goes through
     * [ExifAsciiText.transliterate] first, so "Nyaralás" reaches the file as "Nyaralas". A value with
     * no ASCII form at all transliterates to nothing and so removes the tag.
     *
     * Narrower than [writeLocation]: the container must be one ExifInterface can write (see
     * [isExifWritableImageMime]), so a video or a HEIC returns [MetadataWriteResult.Failed].
     */
    override suspend fun writeDescriptiveText(
        uri: String,
        description: TextTagEdit,
        artist: TextTagEdit,
        copyright: TextTagEdit,
    ): MetadataWriteResult<IntentSender> =
        withContext(Dispatchers.IO) {
            if (!WriteLocalPhotoMetadataUseCase.isExifWritableImageMime(resolveMime(Uri.parse(uri)))) {
                return@withContext MetadataWriteResult.Failed(TEXT_TAGS_EXIF_ONLY)
            }
            val writes = WriteLocalPhotoMetadataUseCase.textTagWrites(
                listOf(
                    ExifInterface.TAG_IMAGE_DESCRIPTION to description,
                    ExifInterface.TAG_ARTIST to artist,
                    ExifInterface.TAG_COPYRIGHT to copyright,
                )
            )
            if (writes.isEmpty()) return@withContext MetadataWriteResult.Success
            // The same "rw" descriptor as the other EXIF legs, so a foreign MediaStore file raises the
            // consent request rather than failing, and all three tags share a single save.
            editExifViaFd(uri) { exif ->
                writes.forEach { (tag, value) -> exif.setAttribute(tag, value) }
            }
        }

    /** Writes the DATE_TAKEN column (ms). GPS is never touched here; location is EXIF-only. */
    private fun updateDateTakenColumn(uri: Uri, dateTakenMs: Long): MetadataWriteResult<IntentSender> =
        try {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DATE_TAKEN, dateTakenMs)
            }
            context.contentResolver.update(uri, values, null, null)
            MetadataWriteResult.Success
        } catch (e: CancellationException) {
            throw e
        } catch (e: SecurityException) {
            needsPermissionFor(uri.toString(), e)
        } catch (e: Exception) {
            Log.w(TAG, "DATE_TAKEN update failed for $uri: ${e.message}")
            MetadataWriteResult.Failed(e.message ?: "date update failed")
        }

    /**
     * Opens a "rw" descriptor on [uri], runs [mutate] against its [ExifInterface], and saves. Maps a
     * recoverable [SecurityException] to [MetadataWriteResult.NeedsPermission<IntentSender>]; rethrows cancellation
     * before any other handling so a cancelled write never reports Failed.
     */
    private fun editExifViaFd(uri: String, mutate: (ExifInterface) -> Unit): MetadataWriteResult<IntentSender> =
        try {
            val parsed = Uri.parse(uri)
            // The save rewrites the file in place, so it runs behind a backup that is put back when it fails.
            LocalOverwriteGuard(context).run(uri) {
                val pfd = context.contentResolver.openFileDescriptor(parsed, "rw")
                    ?: throw java.io.IOException("could not open $uri")
                pfd.use {
                    val exif = ExifInterface(it.fileDescriptor)
                    mutate(exif)
                    exif.saveAttributes()
                }
            }
            MetadataWriteResult.Success
        } catch (e: CancellationException) {
            throw e
        } catch (e: SecurityException) {
            needsPermissionFor(uri, e)
        } catch (e: Exception) {
            Log.w(TAG, "EXIF write failed for $uri: ${e.message}")
            MetadataWriteResult.Failed(e.message ?: "exif write failed")
        }

    /**
     * Builds the consent result for a write the OS refused. On Android 11+ a fresh
     * [MediaStore.createWriteRequest] covers the item; on Android 10 the recoverable exception ([cause])
     * carries its own action. Returns [MetadataWriteResult.Failed] when neither is available.
     */
    private fun needsPermissionFor(uri: String, cause: SecurityException?): MetadataWriteResult<IntentSender> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val sender = runCatching {
                MediaStore.createWriteRequest(context.contentResolver, listOf(Uri.parse(uri))).intentSender
            }.getOrNull()
            if (sender != null) return MetadataWriteResult.NeedsPermission<IntentSender>(sender)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && cause is RecoverableSecurityException) {
            return MetadataWriteResult.NeedsPermission<IntentSender>(cause.userAction.actionIntent.intentSender)
        }
        return MetadataWriteResult.Failed(cause?.message ?: "write permission required")
    }

    /**
     * The container [uri] holds. The content resolver answers for a `content://` uri through its
     * provider and returns null for a plain `file://` one, so an app-private file — a photo the vault
     * holds — has its type read from its name instead. Without that fallback every write here reads a
     * blank type and takes the branch meant for a container it cannot write.
     */
    private fun resolveMime(uri: Uri): String =
        if (isPrivateFile(uri)) mimeFromPath(uri.path.orEmpty())
        else runCatching { context.contentResolver.getType(uri) }.getOrNull().orEmpty()

    /**
     * True when [uri] names a file inside this app's own storage, which is where the vault keeps what
     * it holds.
     *
     * Stricter than [isPrivateFile] on purpose, and the two are not interchangeable. Reading a type
     * from the name suits any `file://` uri, since the resolver answers for none of them. Treating a
     * name change as the whole write suits only a file this app owns: one under shared storage has a
     * MediaStore row that still needs its column, and nothing outside the vault reads a date from a
     * file name at all.
     */
    private fun isAppOwnedFile(uri: Uri): Boolean {
        if (!isPrivateFile(uri)) return false
        val path = uri.path ?: return false
        val owned = runCatching { context.filesDir.canonicalPath }.getOrNull() ?: return false
        val candidate = runCatching { java.io.File(path).canonicalPath }.getOrNull() ?: return false
        return candidate == owned || candidate.startsWith(owned + java.io.File.separator)
    }

    companion object {
        /** True when [uri] names a plain file rather than a provider row. */
        internal fun isPrivateFile(uri: Uri): Boolean = uri.scheme == "file"

        private const val LOCATION_IMAGE_ONLY = "location edit is image-only"
        private const val TEXT_TAGS_EXIF_ONLY = "text metadata edit needs a JPEG, PNG or WebP"
    }
}
