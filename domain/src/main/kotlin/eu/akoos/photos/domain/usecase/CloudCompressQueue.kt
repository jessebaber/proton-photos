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

import eu.akoos.photos.domain.entity.GalleryItem
import kotlinx.coroutines.flow.Flow

/**
 * Queues photos and videos on Drive for compression in place. Android/WorkManager/DataStore details
 * stay in the data-layer implementation.
 */
interface CloudCompressQueue {
    /** True when a start right now would wait: the back-up is set to Wi-Fi only and the phone is not on
     *  an unmetered network. */
    suspend fun waitsForWifi(): Boolean

    /** How many photos are waiting or being compressed. */
    val pending: Flow<Int>

    /** Queues the Drive-only photos and videos among [items] and returns how many that is. */
    fun enqueue(items: List<GalleryItem>): Int

    /**
     * Stops the compression without interrupting a photo halfway: every queued photo not started yet is
     * dropped, while the ones being compressed right now and any whose copy is already on Drive are finished.
     */
    fun cancel()

    /** Picks up photos still queued from an earlier session (a run the system stopped, a reboot). */
    fun resumeIfPending()
}

/** The back-up's Wi-Fi only switch as stored: on until the user turns it off, as Settings shows it. Pure. */
fun wifiOnlySetting(stored: Boolean?): Boolean = stored != false

/** The link of every photo or video in [items] that lives only on Drive: the ones compression in place
 *  applies to, newest first as the timeline shows them, whatever order the screen's selection holds. Pure. */
fun compressibleLinkIds(items: List<GalleryItem>): List<String> = items
    .filterIsInstance<GalleryItem.CloudOnly>()
    .filter { val mime = it.cloud.mimeType.lowercase(); mime.startsWith("image/") || mime.startsWith("video/") }
    .sortedByDescending { it.captureTimeMs }
    .map { it.cloud.linkId }
    .distinct()
