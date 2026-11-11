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

import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.datastore.preferences.core.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.akoos.photos.R
import eu.akoos.photos.data.notification.NotificationIds
import eu.akoos.photos.data.notification.ensureNotificationChannel
import eu.akoos.photos.data.preferences.SettingsKeys
import eu.akoos.photos.data.preferences.settingsDataStore
import eu.akoos.photos.domain.entity.SyncStatus
import eu.akoos.photos.domain.usecase.PendingDeleteProbe
import eu.akoos.photos.domain.usecase.pendingDeleteEntryIsStale
import eu.akoos.photos.domain.usecase.pendingDeleteProbe
import eu.akoos.photos.domain.usecase.trimPendingDeleteQueue
import eu.akoos.photos.domain.repository.SyncStateRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reconciles the delete-after-backup pending queue against MediaStore reality and
 * refreshes (or cancels) the system consent notification accordingly. The two
 * pieces always travel together — the notification's `createTrashRequest`
 * PendingIntent throws if any of its URIs is stale, so we prune first and then
 * post the notification with whatever's left alive.
 *
 * Idempotent and cheap (one row probe per queued URI). Called from two places:
 *  - [UploadPendingUseCase] at the end of every sync run, to surface the
 *    notification immediately after a successful upload batch.
 *  - `MainActivity.onResume`, so files the user deleted externally while we
 *    were not running still get reconciled the moment they open the app —
 *    even without network connectivity to fire the worker's content URI
 *    trigger.
 */
@Singleton
class PendingDeleteNotificationUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val syncStateRepo: SyncStateRepository,
) {
    suspend operator fun invoke() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        val prefs = context.settingsDataStore.data.first()
        // User opted out of the delete-after-backup reminder — don't post or prune.
        if (prefs[SettingsKeys.NOTIFY_DELETE_REMINDER] == false) return
        val pendingRaw = prefs[SettingsKeys.PENDING_DELETE_URIS] ?: emptySet()
        if (pendingRaw.isEmpty()) return

        // Probe each URI for a row and its trashed flag, and let [pendingDeleteProbe] rule
        // on it. Stale entries get their SyncState collapsed to CLOUD_ONLY so the gallery
        // stops painting them as locally present, and they're removed from the queue so
        // the next createTrashRequest doesn't trip over them. A probe that could not ask
        // (no cursor, or a provider error) proves nothing: the entry stays queued and its
        // row untouched, and only this round's notification leaves it out.
        val probed = pendingRaw.groupBy { uriStr ->
            runCatching {
                context.contentResolver.query(
                    Uri.parse(uriStr),
                    arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.IS_TRASHED),
                    null, null, null,
                )?.use { c ->
                    if (!c.moveToFirst()) {
                        pendingDeleteProbe(queried = true, rowExists = false, isTrashed = false)
                    } else {
                        val col = c.getColumnIndex(MediaStore.MediaColumns.IS_TRASHED)
                        pendingDeleteProbe(queried = true, rowExists = true, isTrashed = col >= 0 && c.getInt(col) != 0)
                    }
                } ?: pendingDeleteProbe(queried = false, rowExists = false, isTrashed = false)
            }.getOrDefault(PendingDeleteProbe.UNKNOWN)
        }
        val stale = probed[PendingDeleteProbe.STALE].orEmpty()
        val aliveAll = probed[PendingDeleteProbe.ALIVE].orEmpty()
        val unknown = probed[PendingDeleteProbe.UNKNOWN].orEmpty()
        // A queue that outgrows the cap is one the user keeps declining; drop the
        // longest-waiting entries so it can't grow without bound. Those files stay on
        // the device with a SYNCED row (free-up-space can still reclaim them), so
        // unlike a stale entry they must NOT be collapsed to CLOUD_ONLY.
        val kept = trimPendingDeleteQueue(aliveAll + unknown, SettingsKeys.PENDING_DELETE_URIS_MAX)
        val dropped = aliveAll.size + unknown.size - kept.size
        if (dropped > 0) {
            Log.w(TAG, "Delete queue over ${SettingsKeys.PENDING_DELETE_URIS_MAX}, dropped $dropped longest-waiting URI(s)")
        }
        if (stale.isNotEmpty() || dropped > 0) {
            context.settingsDataStore.edit { p ->
                p[SettingsKeys.PENDING_DELETE_URIS] = kept.toSet()
            }
        }
        if (stale.isNotEmpty()) {
            Log.d(TAG, "Pruning ${stale.size} stale URI(s) from delete queue")
            runCatching { syncStateRepo.updateStatuses(stale, SyncStatus.CLOUD_ONLY) }
        }
        val aliveSet = aliveAll.toSet()
        val alive = kept.filter { it in aliveSet }

        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        if (alive.isEmpty()) {
            // Nothing left to ask about — dismiss any lingering notification so the
            // user doesn't see a "X photos ready to remove" badge after they already
            // got rid of everything externally.
            nm.cancel(DELETE_NOTIFICATION_ID)
            return
        }
        val uris = alive.mapNotNull { runCatching { Uri.parse(it) }.getOrNull() }
        if (uris.isEmpty()) {
            nm.cancel(DELETE_NOTIFICATION_ID)
            return
        }

        ensureNotificationChannel(
            context,
            id = DELETE_CHANNEL_ID,
            name = context.getString(R.string.delete_consent_channel_name),
            description = context.getString(R.string.delete_consent_channel_desc),
            importance = NotificationManager.IMPORTANCE_DEFAULT,
            silent = true,
        )
        val trashRequestPi = runCatching {
            MediaStore.createTrashRequest(context.contentResolver, uris, true)
        }.getOrElse {
            Log.w(TAG, "createTrashRequest failed after prune: ${it.message}")
            return
        }
        val notif = NotificationCompat.Builder(context, DELETE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.delete_consent_title, uris.size))
            .setContentText(context.getString(R.string.delete_consent_text))
            .setContentIntent(trashRequestPi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        nm.notify(DELETE_NOTIFICATION_ID, notif)
        Log.d(TAG, "Delete consent notification refreshed for ${uris.size} URI(s)")
    }

    companion object {
        const val DELETE_CHANNEL_ID = "delete_consent"
        const val DELETE_NOTIFICATION_ID = NotificationIds.DELETE_CONSENT
        private const val TAG = "PendingDeleteNotif"
    }
}
