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

package eu.akoos.photos.worker

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import eu.akoos.photos.R
import eu.akoos.photos.MainActivity
import eu.akoos.photos.data.notification.NotificationIds
import eu.akoos.photos.data.notification.ensureNotificationChannel
import eu.akoos.photos.domain.usecase.FreeUpSpaceUseCase
import eu.akoos.photos.util.isOfflineError
import eu.akoos.photos.util.isTransientApiError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import me.proton.core.accountmanager.domain.AccountManager
import java.util.concurrent.TimeUnit

/**
 * Background Drive verify for manual Free up space. Runs as a `dataSync` FGS so leave-screen / lock
 * does not kill the long chunked check. Persists successes into `free_up_verified`; reclaim stays a
 * short foreground finish. AUTO_FREE_UP / thermal / freshness gates do **not** apply here.
 */
@HiltWorker
class ManualFreeUpWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val params: WorkerParameters,
    private val freeUpSpace: FreeUpSpaceUseCase,
    private val accountManager: AccountManager,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val userId = accountManager.getPrimaryUserId().first() ?: return Result.success()
        runCatching { setForeground(buildForegroundInfo(done = 0, total = 0)) }
            .onFailure { Log.w(TAG, "setForeground initial failed: ${it.message}") }

        return try {
            val candidates = freeUpSpace.candidates(userId, Long.MAX_VALUE, protectDownloaded = false)
            if (candidates.isEmpty()) {
                return Result.success(workDataOf(KEY_DONE to 0, KEY_TOTAL to 0))
            }
            freeUpSpace.verifyForReclaim(userId, candidates) { done, total ->
                runCatching {
                    setProgressAsync(workDataOf(KEY_DONE to done, KEY_TOTAL to total))
                }
                runCatching { setForegroundAsync(buildForegroundInfo(done, total)) }
            }
            val ready = freeUpSpace.readyToReclaim(userId)
            if (ready.isNotEmpty()) {
                postReadyNotification(ready.size)
            }
            Result.success(
                workDataOf(
                    KEY_DONE to candidates.size,
                    KEY_TOTAL to candidates.size,
                    KEY_READY to ready.size,
                ),
            )
        } catch (e: CancellationException) {
            // User Cancel action: drop this run's verified markers so a half-checked set is not
            // offered for reclaim.
            runCatching { freeUpSpace.clearVerified(userId) }
            Log.d(TAG, "manual free-up verify cancelled")
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "manual free-up verify failed: ${e.message}")
            if (isTransientApiError(e) || isOfflineError(e) || runAttemptCount < MAX_ATTEMPTS) {
                Result.retry()
            } else {
                Result.failure()
            }
        } finally {
            runCatching { NotificationManagerCompat.from(context).cancel(NotificationIds.MANUAL_FREE_UP) }
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo =
        buildForegroundInfo(done = 0, total = 0)

    private fun buildForegroundInfo(done: Int, total: Int): ForegroundInfo {
        ensureChannel(context)
        val cancelIntent = WorkManager.getInstance(context).createCancelPendingIntent(id)
        val title = context.getString(R.string.free_up_verify_notification_title)
        val content = if (total > 0) {
            context.getString(R.string.free_up_verify_progress, done, total)
        } else {
            context.getString(R.string.free_up_verifying)
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText(content)
            .setProgress(total.coerceAtLeast(1), done.coerceAtMost(total.coerceAtLeast(1)), total <= 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                context.getString(R.string.cancel),
                cancelIntent,
            )
            .build()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NotificationIds.MANUAL_FREE_UP,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            ForegroundInfo(NotificationIds.MANUAL_FREE_UP, notification)
        }
    }

    private fun postReadyNotification(readyCount: Int) {
        ensureChannel(context)
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_OPEN_FREE_UP, true)
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            NotificationIds.MANUAL_FREE_UP_READY,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(context.getString(R.string.free_up_ready_notification_title))
            .setContentText(context.getString(R.string.free_up_ready_notification_text, readyCount))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()
        runCatching {
            NotificationManagerCompat.from(context).notify(NotificationIds.MANUAL_FREE_UP_READY, notification)
        }
    }

    companion object {
        const val TAG = "manual_free_up_worker"
        const val UNIQUE_NAME = "manual_free_up"
        const val CHANNEL_ID = "manual_free_up"
        const val KEY_DONE = "done"
        const val KEY_TOTAL = "total"
        const val KEY_READY = "ready"
        private const val MAX_ATTEMPTS = 3

        fun ensureChannel(context: Context) {
            ensureNotificationChannel(
                context,
                id = CHANNEL_ID,
                name = context.getString(R.string.free_up_verify_channel_name),
                description = context.getString(R.string.free_up_verify_channel_desc),
                importance = NotificationManager.IMPORTANCE_LOW,
                silent = false,
            )
        }

        /** Constraints for the manual verify worker: network required; no AUTO_FREE_UP / thermal. */
        fun verifyConstraints(): Constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        fun enqueue(context: Context) {
            ensureChannel(context)
            val request = OneTimeWorkRequestBuilder<ManualFreeUpWorker>()
                .addTag(TAG)
                .setConstraints(verifyConstraints())
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_NAME,
                ExistingWorkPolicy.KEEP,
                request,
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_NAME)
        }
    }
}
