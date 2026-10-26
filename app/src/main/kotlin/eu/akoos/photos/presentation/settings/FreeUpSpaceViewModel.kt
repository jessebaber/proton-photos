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

package eu.akoos.photos.presentation.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.akoos.photos.R
import eu.akoos.photos.domain.entity.SyncState
import eu.akoos.photos.domain.usecase.FreeUpSpaceUseCase
import eu.akoos.photos.worker.FreeUpPipelineWorker
import eu.akoos.photos.worker.ManualFreeUpWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

/**
 * Free up space screen: lists reclaimable candidates, enqueues/cancels [ManualFreeUpWorker]
 * (shared [FreeUpPipelineWorker]), and observes WorkInfo phase/counts. Reclaim runs in the worker
 * — this ViewModel does not own a foreground finish loop.
 */
@HiltViewModel
class FreeUpSpaceViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val freeUpSpace: FreeUpSpaceUseCase,
    private val localRepo: eu.akoos.photos.domain.repository.LocalMediaRepository,
    private val accountManager: AccountManager,
    private val pendingDeleteBridge: FreeUpPendingDeleteBridge,
) : ViewModel() {

    data class UiState(
        val loading: Boolean = true,
        val candidates: List<SyncState> = emptyList(),
        val phase: FreeUpSpacePhase = FreeUpSpacePhase.Idle,
        val checked: Int = 0,
        val checkTotal: Int = 0,
        val rechecked: Int = 0,
        val recheckTotal: Int = 0,
        val processed: Int = 0,
        val removed: Int = 0,
        val deletionTotal: Int = 0,
        val readyCount: Int = 0,
        val message: String? = null,
        val reclaimableBytes: Long = 0L,
    ) {
        val isEmpty: Boolean get() = !loading && candidates.isEmpty() && readyCount == 0
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    /** User-initiated stop; cleared by [start] / [continueWork]. Does not freeze WorkInfo counts. */
    private var stoppedLatch = false

    /** Dedupes ready-set reloads against identical progress fingerprints. */
    private var lastReadyReloadKey: String? = null

    /** Tracks WorkInfo id+state so Done/Error messages fire once per terminal transition. */
    private var lastTerminalWorkKey: String? = null

    init {
        load()
        observeWorker()
        observeConsentSettlement()
    }

    fun load() {
        viewModelScope.launch {
            refreshFromStore()
        }
    }

    /**
     * Refresh candidates / ready count from the store. While Rechecking or Freeing, preserves the
     * live grid (candidates + reclaimableBytes) and only clears loading / updates readyCount —
     * coherent-grid load guard; mid-run bytes come from settle/prune, not MediaStore.
     */
    private suspend fun refreshFromStore() {
        val userId = accountManager.getPrimaryUserId().first() ?: run {
            _uiState.update { it.copy(loading = false, candidates = emptyList()) }
            return
        }
        _uiState.update { it.copy(loading = true) }
        val rows = freeUpSpace.candidates(userId, Long.MAX_VALUE, protectDownloaded = false)
        val ready = freeUpSpace.readyToReclaim(userId)
        val bytes = totalBytes(rows)
        _uiState.update {
            val liveGrid = it.phase == FreeUpSpacePhase.Rechecking ||
                it.phase == FreeUpSpacePhase.Freeing
            it.copy(
                loading = false,
                candidates = if (liveGrid) it.candidates else rows,
                readyCount = ready.size,
                reclaimableBytes = if (liveGrid) it.reclaimableBytes else bytes,
            )
        }
    }

    /**
     * Enqueue the manual pipeline. No-ops while Checking/Rechecking/Freeing, or while a consent
     * chunk is already queued (avoid a second pipeline racing the MediaStore dialog).
     */
    fun start() {
        if (_uiState.value.phase in setOf(
                FreeUpSpacePhase.Checking,
                FreeUpSpacePhase.Rechecking,
                FreeUpSpacePhase.Freeing,
            )
        ) return
        if (pendingDeleteBridge.uris.value.isNotEmpty()) return
        stoppedLatch = false
        lastReadyReloadKey = null
        _uiState.update {
            it.copy(
                phase = FreeUpSpacePhase.Checking,
                checked = 0,
                checkTotal = 0,
                rechecked = 0,
                recheckTotal = 0,
                processed = 0,
                removed = 0,
                deletionTotal = 0,
                message = null,
            )
        }
        ManualFreeUpWorker.enqueue(context)
    }

    /** Resume after Stopped (or Ready with residual markers and empty consent queue). */
    fun continueWork() {
        when (_uiState.value.phase) {
            FreeUpSpacePhase.Stopped,
            FreeUpSpacePhase.Ready,
            -> {
                if (pendingDeleteBridge.uris.value.isNotEmpty()) return
                stoppedLatch = false
                lastReadyReloadKey = null
                ManualFreeUpWorker.enqueue(context)
            }
            else -> Unit
        }
    }

    /** Cancel manual unique work only; remaining verified markers stay recoverable for Continue. */
    fun stop() {
        when (_uiState.value.phase) {
            FreeUpSpacePhase.Checking,
            FreeUpSpacePhase.Rechecking,
            FreeUpSpacePhase.Freeing,
            -> {
                stoppedLatch = true
                ManualFreeUpWorker.cancel(context)
                _uiState.update { it.copy(phase = FreeUpSpacePhase.Stopped) }
                load()
            }
            else -> Unit
        }
    }

    fun clearMessage() = _uiState.update { it.copy(message = null) }

    private fun observeWorker() {
        viewModelScope.launch {
            WorkManager.getInstance(context)
                .getWorkInfosForUniqueWorkFlow(ManualFreeUpWorker.UNIQUE_NAME)
                .collect { infos ->
                    val info = infos.firstOrNull()
                    val workState = info?.state
                    val running = workState == WorkInfo.State.RUNNING ||
                        workState == WorkInfo.State.ENQUEUED ||
                        workState == WorkInfo.State.BLOCKED
                    val succeeded = workState == WorkInfo.State.SUCCEEDED
                    val failed = workState == WorkInfo.State.FAILED
                    val cancelled = workState == WorkInfo.State.CANCELLED

                    val progress = info?.progress
                    val output = info?.outputData
                    val workerPhase = workString(progress, output, FreeUpPipelineWorker.KEY_PHASE)
                    val processed = workInt(
                        progress,
                        output,
                        FreeUpPipelineWorker.KEY_PROCESSED,
                        fallbackKey = FreeUpPipelineWorker.KEY_DONE,
                    )
                    val total = workInt(progress, output, FreeUpPipelineWorker.KEY_TOTAL)
                    val removed = workInt(progress, output, FreeUpPipelineWorker.KEY_REMOVED)
                    val needsPermission = workInt(
                        progress,
                        output,
                        FreeUpPipelineWorker.KEY_NEEDS_PERMISSION_COUNT,
                    )

                    val userId = accountManager.getPrimaryUserId().first()
                    val readyCount = if (userId != null) {
                        freeUpSpace.readyToReclaimCount(userId)
                    } else {
                        0
                    }

                    val current = _uiState.value
                    val terminalKey = info?.let { "${it.id}:$workState" }
                    val isNewTerminal = terminalKey != null &&
                        terminalKey != lastTerminalWorkKey &&
                        (succeeded || failed)
                    if (isNewTerminal) lastTerminalWorkKey = terminalKey

                    val awaitingConsent = workerPhase == FreeUpPipelineWorker.PHASE_AWAITING_CONSENT ||
                        needsPermission > 0
                    val shouldPublishDone = isNewTerminal &&
                        succeeded &&
                        !awaitingConsent &&
                        removed > 0
                    val doneMessage = shouldPublishDone ||
                        (current.message != null && current.phase == FreeUpSpacePhase.Done)

                    val phase = deriveFreeUpSpaceState(
                        workRunning = running,
                        workSucceeded = succeeded,
                        workFailed = failed,
                        readyCount = readyCount,
                        doneMessage = doneMessage && !running,
                        workerPhase = workerPhase,
                        stopped = stoppedLatch,
                        workCancelled = cancelled,
                    )

                    _uiState.update { state ->
                        var next = state.copy(
                            phase = phase,
                            readyCount = readyCount,
                        )
                        when (workerPhase) {
                            FreeUpPipelineWorker.PHASE_RECHECKING -> {
                                next = next.copy(rechecked = processed, recheckTotal = total)
                            }
                            FreeUpPipelineWorker.PHASE_DELETING,
                            FreeUpPipelineWorker.PHASE_AWAITING_CONSENT,
                            -> {
                                next = next.copy(
                                    processed = processed,
                                    removed = removed,
                                    deletionTotal = total,
                                )
                            }
                            else -> {
                                // verifying / enqueued / unknown — drive the check progress bar
                                if (running || succeeded || failed) {
                                    next = next.copy(checked = processed, checkTotal = total)
                                }
                            }
                        }
                        when {
                            shouldPublishDone -> next.copy(
                                phase = FreeUpSpacePhase.Done,
                                message = freedMessage(removed),
                                readyCount = 0,
                            )
                            isNewTerminal && failed -> next.copy(
                                phase = FreeUpSpacePhase.Error,
                                message = context.getString(
                                    R.string.settings_free_up_error,
                                    "",
                                ),
                            )
                            else -> next
                        }
                    }

                    if (userId != null && shouldReloadReadyGrid(running, succeeded, workerPhase)) {
                        reloadReadyCandidates(
                            userId = userId,
                            reloadKey = "$workerPhase|$processed|$total|$removed",
                        )
                    }

                    if (shouldPublishDone) {
                        refreshFromStore()
                    }
                }
        }
    }

    /**
     * Reconcile typed MediaStore consent outcomes when not mid pipeline run.
     * Grant: prune settled URIs, refresh, Done/Idle or Ready.
     * Deny: Stopped (or Idle), refresh; do not auto-relaunch consent.
     */
    private fun observeConsentSettlement() {
        viewModelScope.launch {
            pendingDeleteBridge.settled.collect { settlement ->
                val phase = _uiState.value.phase
                if (phase == FreeUpSpacePhase.Checking ||
                    phase == FreeUpSpacePhase.Rechecking ||
                    phase == FreeUpSpacePhase.Freeing
                ) {
                    return@collect
                }
                if (settlement.granted) {
                    _uiState.update {
                        val (pruned, pruneBytes) = pruneFreeUpCandidates(
                            it.candidates,
                            it.reclaimableBytes,
                            settlement.uris.toList(),
                        )
                        it.copy(candidates = pruned, reclaimableBytes = pruneBytes)
                    }
                    refreshFromStore()
                    val readyCount = _uiState.value.readyCount
                    if (readyCount == 0 && pendingDeleteBridge.uris.value.isEmpty()) {
                        val freed = settlement.uris.size
                        _uiState.update {
                            it.copy(
                                phase = if (freed > 0) {
                                    FreeUpSpacePhase.Done
                                } else {
                                    FreeUpSpacePhase.Idle
                                },
                                message = if (freed > 0) freedMessage(freed) else null,
                            )
                        }
                    } else {
                        _uiState.update { it.copy(phase = FreeUpSpacePhase.Ready) }
                    }
                } else {
                    val userId = accountManager.getPrimaryUserId().first()
                    val readyCount = if (userId != null) {
                        freeUpSpace.readyToReclaimCount(userId)
                    } else {
                        0
                    }
                    if (readyCount > 0) {
                        stoppedLatch = true
                        _uiState.update { it.copy(phase = FreeUpSpacePhase.Stopped) }
                    } else {
                        stoppedLatch = false
                        _uiState.update { it.copy(phase = FreeUpSpacePhase.Idle) }
                    }
                    refreshFromStore()
                }
            }
        }
    }

    /**
     * Reload indexed remaining candidates from [FreeUpSpaceUseCase.readyToReclaim] and settle
     * size-aware onto the live grid. Never calls MediaStore [totalBytes] mid-run.
     */
    private suspend fun reloadReadyCandidates(userId: UserId, reloadKey: String) {
        if (reloadKey == lastReadyReloadKey) return
        lastReadyReloadKey = reloadKey
        val ready = freeUpSpace.readyToReclaim(userId)
        _uiState.update { current ->
            val known = current.candidates.associate { it.localUri to it.sizeBytes }
            val (settled, settleBytes) = settleFreeUpCandidates(ready, known)
            // Phase is owned by WorkInfo / stoppedLatch — only refresh the indexed grid here.
            current.copy(
                candidates = settled,
                reclaimableBytes = settleBytes,
                readyCount = ready.size,
            )
        }
    }

    private fun shouldReloadReadyGrid(
        running: Boolean,
        succeeded: Boolean,
        workerPhase: String?,
    ): Boolean {
        if (workerPhase == FreeUpPipelineWorker.PHASE_RECHECKING ||
            workerPhase == FreeUpPipelineWorker.PHASE_DELETING ||
            workerPhase == FreeUpPipelineWorker.PHASE_VERIFYING
        ) {
            return running
        }
        if (workerPhase == FreeUpPipelineWorker.PHASE_AWAITING_CONSENT) {
            return running || succeeded
        }
        return false
    }

    private suspend fun totalBytes(rows: List<SyncState>): Long {
        if (rows.isEmpty()) return 0L
        val sizeByUri = runCatching {
            localRepo.observeLocalMedia().first().associate { it.uri to it.sizeBytes }
        }.getOrDefault(emptyMap())
        return rows.sumOf { row ->
            row.sizeBytes.takeIf { it > 0L } ?: sizeByUri[row.localUri] ?: 0L
        }
    }

    private fun freedMessage(freed: Int): String =
        if (freed > 0) {
            context.resources.getQuantityString(R.plurals.settings_freed_photos, freed, freed)
        } else {
            context.getString(R.string.settings_free_up_none)
        }

    private fun workString(
        progress: androidx.work.Data?,
        output: androidx.work.Data?,
        key: String,
    ): String? = when {
        progress?.keyValueMap?.containsKey(key) == true -> progress.getString(key)
        output?.keyValueMap?.containsKey(key) == true -> output.getString(key)
        else -> null
    }

    private fun workInt(
        progress: androidx.work.Data?,
        output: androidx.work.Data?,
        key: String,
        fallbackKey: String? = null,
        default: Int = 0,
    ): Int = when {
        progress?.keyValueMap?.containsKey(key) == true -> progress.getInt(key, default)
        output?.keyValueMap?.containsKey(key) == true -> output.getInt(key, default)
        fallbackKey != null && progress?.keyValueMap?.containsKey(fallbackKey) == true ->
            progress.getInt(fallbackKey, default)
        fallbackKey != null && output?.keyValueMap?.containsKey(fallbackKey) == true ->
            output.getInt(fallbackKey, default)
        else -> default
    }
}
