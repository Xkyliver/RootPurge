package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AppCacheItem
import com.example.data.model.CleanTarget
import com.example.data.model.CleaningResult
import com.example.data.model.RootState
import com.example.data.repository.CacheCleanerRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppFilter {
    ALL,
    USER_APPS,
    SYSTEM_APPS,
    HEAVY_ONLY
}

data class CleanerUiState(
    val rootState: RootState = RootState.Checking,
    val isScanning: Boolean = false,
    val isPurging: Boolean = false,
    val purgeProgress: Float = 0f,
    val purgeStageText: String = "",
    val allApps: List<AppCacheItem> = emptyList(),
    val searchQuery: String = "",
    val activeFilter: AppFilter = AppFilter.ALL,
    val selectedTargets: Set<CleanTarget> = CleanTarget.entries.toSet(),
    val storageStats: CacheCleanerRepository.StorageStats? = null,
    val memoryStats: CacheCleanerRepository.MemoryStats? = null,
    val lastResult: CleaningResult? = null,
    val showResultDialog: Boolean = false,
    val showTerminalDrawer: Boolean = false,
    val showRootDiagnostic: Boolean = false,
    val terminalLogs: List<String> = emptyList(),
    val lifetimeFreedBytes: Long = 0L,
    val lastCleanDurationMs: Long = 0L,
    val reclaimableReport: com.example.data.model.ReclaimableStorageReport? = null
) {
    val totalCacheSizeBytes: Long
        get() = allApps.filter { !it.isWhitelisted }.sumOf { it.cacheSizeBytes }

    val formattedTotalCache: String
        get() = AppCacheItem.formatBytes(totalCacheSizeBytes)

    val filteredApps: List<AppCacheItem>
        get() {
            var list = allApps
            if (searchQuery.isNotBlank()) {
                list = list.filter {
                    it.appName.contains(searchQuery, ignoreCase = true) ||
                    it.packageName.contains(searchQuery, ignoreCase = true)
                }
            }
            list = when (activeFilter) {
                AppFilter.ALL -> list
                AppFilter.USER_APPS -> list.filter { !it.isSystemApp }
                AppFilter.SYSTEM_APPS -> list.filter { it.isSystemApp }
                AppFilter.HEAVY_ONLY -> list.filter { it.cacheSizeBytes > 50 * 1024 * 1024 }
            }
            return list
        }
}

class CleanerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CacheCleanerRepository(application.applicationContext)
    private val _uiState = MutableStateFlow(CleanerUiState())
    val uiState: StateFlow<CleanerUiState> = _uiState.asStateFlow()

    private val sharedPrefs = application.getSharedPreferences("root_cleaner_stats", Application.MODE_PRIVATE)

    init {
        val savedLifetimeFreed = sharedPrefs.getLong("lifetime_freed_bytes", 0L)
        val savedLastDuration = sharedPrefs.getLong("last_clean_duration_ms", 0L)
        _uiState.update {
            it.copy(
                lifetimeFreedBytes = savedLifetimeFreed,
                lastCleanDurationMs = savedLastDuration
            )
        }
        refreshTelemetry()
        checkRootAndScan()
    }

    fun refreshTelemetry() {
        val storage = repository.getStorageStats()
        val memory = repository.getMemoryStats()
        _uiState.update { it.copy(storageStats = storage, memoryStats = memory) }
    }

    fun checkRootAndScan() {
        viewModelScope.launch {
            _uiState.update { it.copy(rootState = RootState.Checking, isScanning = true) }
            val root = repository.checkRoot()
            val isRootGranted = root is RootState.Granted

            val report = repository.calculateTotalReclaimableStorage(isRootGranted)
            val (apps, scanLogs) = repository.scanAppCaches(isRootGranted)
            refreshTelemetry()

            _uiState.update {
                it.copy(
                    rootState = root,
                    allApps = apps,
                    reclaimableReport = report,
                    terminalLogs = scanLogs,
                    isScanning = false
                )
            }
        }
    }

    fun toggleTarget(target: CleanTarget) {
        _uiState.update { state ->
            val updated = state.selectedTargets.toMutableSet()
            if (updated.contains(target)) {
                if (updated.size > 1) updated.remove(target) // Keep at least one
            } else {
                updated.add(target)
            }
            state.copy(selectedTargets = updated)
        }
    }

    fun toggleAppWhitelist(packageName: String) {
        val isWhitelisted = repository.toggleAppWhitelist(packageName)
        _uiState.update { state ->
            val updatedList = state.allApps.map {
                if (it.packageName == packageName) it.copy(isWhitelisted = isWhitelisted) else it
            }
            state.copy(allApps = updatedList)
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setFilter(filter: AppFilter) {
        _uiState.update { it.copy(activeFilter = filter) }
    }

    fun toggleTerminalDrawer() {
        _uiState.update { it.copy(showTerminalDrawer = !it.showTerminalDrawer) }
    }

    fun setRootDiagnosticVisible(visible: Boolean) {
        _uiState.update { it.copy(showRootDiagnostic = visible) }
    }

    fun dismissResultDialog() {
        _uiState.update { it.copy(showResultDialog = false) }
    }

    /**
     * The core 1-Click Turbo Purge action.
     * Executes batch root commands with sub-second performance.
     */
    fun performSingleClickTurboPurge() {
        if (_uiState.value.isPurging) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isPurging = true,
                    purgeProgress = 0.15f,
                    purgeStageText = "Initializing root shell..."
                )
            }

            delay(60) // High-speed transition
            _uiState.update {
                it.copy(
                    purgeProgress = 0.45f,
                    purgeStageText = "Executing batch rm -rf commands..."
                )
            }

            val currentState = _uiState.value
            val isRootGranted = currentState.rootState is RootState.Granted

            val result = repository.purgeAllCaches(
                targets = currentState.selectedTargets,
                appsToClean = currentState.allApps,
                isRootGranted = isRootGranted
            )

            _uiState.update {
                it.copy(
                    purgeProgress = 0.90f,
                    purgeStageText = "Reclaiming storage..."
                )
            }

            // Update local apps to 0 cache for cleaned apps
            val whitelisted = repository.getWhitelistedPackages()
            val cleanedApps = currentState.allApps.map {
                if (!whitelisted.contains(it.packageName)) {
                    it.copy(cacheSizeBytes = 0L)
                } else {
                    it
                }
            }

            val newLifetime = currentState.lifetimeFreedBytes + result.freedBytes
            sharedPrefs.edit()
                .putLong("lifetime_freed_bytes", newLifetime)
                .putLong("last_clean_duration_ms", result.durationMs)
                .apply()

            refreshTelemetry()

            val combinedLogs = currentState.terminalLogs + result.logOutput

            _uiState.update {
                it.copy(
                    isPurging = false,
                    purgeProgress = 1.0f,
                    purgeStageText = "Purged in ${result.durationMs}ms!",
                    allApps = cleanedApps,
                    lastResult = result,
                    showResultDialog = true,
                    terminalLogs = combinedLogs,
                    lifetimeFreedBytes = newLifetime,
                    lastCleanDurationMs = result.durationMs
                )
            }
        }
    }

    /**
     * Cleans an individual app's cache.
     */
    fun purgeSingleApp(app: AppCacheItem) {
        viewModelScope.launch {
            val isRootGranted = _uiState.value.rootState is RootState.Granted
            val result = repository.purgeSingleApp(app, isRootGranted)

            val updatedApps = _uiState.value.allApps.map {
                if (it.packageName == app.packageName) it.copy(cacheSizeBytes = 0L) else it
            }

            refreshTelemetry()

            val combinedLogs = _uiState.value.terminalLogs + result.logOutput
            _uiState.update {
                it.copy(
                    allApps = updatedApps,
                    lastResult = result,
                    terminalLogs = combinedLogs
                )
            }
        }
    }

    fun clearLogs() {
        _uiState.update { it.copy(terminalLogs = emptyList()) }
    }
}
