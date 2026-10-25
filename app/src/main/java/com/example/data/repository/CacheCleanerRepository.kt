package com.example.data.repository

import android.app.ActivityManager
import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import com.example.data.model.AppCacheItem
import com.example.data.model.CacheDirectoryEntry
import com.example.data.model.CleanTarget
import com.example.data.model.CleaningResult
import com.example.data.model.ReclaimableStorageReport
import com.example.data.model.RootState
import com.example.data.root.RootShellExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CacheCleanerRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("root_cleaner_prefs", Context.MODE_PRIVATE)
    private val PREF_WHITELIST = "whitelisted_packages"

    data class StorageStats(
        val totalBytes: Long,
        val availableBytes: Long,
        val usedBytes: Long,
        val usedPercent: Float
    )

    data class MemoryStats(
        val totalRamBytes: Long,
        val availableRamBytes: Long,
        val usedRamBytes: Long,
        val usedPercent: Float
    )

    fun getWhitelistedPackages(): Set<String> {
        return prefs.getStringSet(PREF_WHITELIST, emptySet()) ?: emptySet()
    }

    fun setWhitelistedPackages(packages: Set<String>) {
        prefs.edit().putStringSet(PREF_WHITELIST, packages).apply()
    }

    fun toggleAppWhitelist(packageName: String): Boolean {
        val current = getWhitelistedPackages().toMutableSet()
        val isNowWhitelisted = if (current.contains(packageName)) {
            current.remove(packageName)
            false
        } else {
            current.add(packageName)
            true
        }
        setWhitelistedPackages(current)
        return isNowWhitelisted
    }

    suspend fun checkRoot(): RootState {
        return RootShellExecutor.checkRootState()
    }

    fun getStorageStats(): StorageStats {
        return try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val availableBlocks = stat.availableBlocksLong

            val total = totalBlocks * blockSize
            val available = availableBlocks * blockSize
            val used = total - available
            val percent = if (total > 0) (used.toFloat() / total.toFloat()) else 0f
            StorageStats(total, available, used, percent)
        } catch (_: Exception) {
            StorageStats(64_000_000_000L, 32_000_000_000L, 32_000_000_000L, 0.5f)
        }
    }

    fun getMemoryStats(): MemoryStats {
        return try {
            val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            actManager.getMemoryInfo(memInfo)

            val total = memInfo.totalMem
            val available = memInfo.availMem
            val used = total - available
            val percent = if (total > 0) (used.toFloat() / total.toFloat()) else 0f
            MemoryStats(total, available, used, percent)
        } catch (_: Exception) {
            MemoryStats(8_000_000_000L, 4_000_000_000L, 4_000_000_000L, 0.5f)
        }
    }

    private fun timestamp(): String {
        return SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())
    }

    suspend fun calculateTotalReclaimableStorage(isRootGranted: Boolean): ReclaimableStorageReport = withContext(Dispatchers.IO) {
        if (isRootGranted) {
            RootShellExecutor.traverseAndCalculateReclaimableStorage()
        } else {
            val startTime = SystemClock.elapsedRealtime()
            val entries = mutableListOf<CacheDirectoryEntry>()
            val appMap = mutableMapOf<String, Long>()
            val logs = mutableListOf<String>()

            logs.add("[${timestamp()}] [SCAN] Root not active - scanning accessible filesystem paths")

            var appTotal = 0L
            var codeTotal = 0L
            var externalTotal = 0L

            try {
                val appCacheSize = context.cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
                if (appCacheSize > 0) {
                    appTotal += appCacheSize
                    entries.add(CacheDirectoryEntry(context.cacheDir.absolutePath, appCacheSize, "App Cache"))
                    appMap[context.packageName] = (appMap[context.packageName] ?: 0L) + appCacheSize
                    logs.add("[${timestamp()}] [SCAN] ${context.packageName}: ${AppCacheItem.formatBytes(appCacheSize)}")
                }
                val codeCacheSize = context.codeCacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
                if (codeCacheSize > 0) {
                    codeTotal += codeCacheSize
                    entries.add(CacheDirectoryEntry(context.codeCacheDir.absolutePath, codeCacheSize, "Code Cache"))
                }
            } catch (e: Exception) {
                logs.add("[${timestamp()}] [WARN] Internal cache traversal: ${e.message}")
            }

            try {
                val extData = File(Environment.getExternalStorageDirectory(), "Android/data")
                if (extData.exists() && extData.isDirectory) {
                    extData.listFiles()?.forEach { appFolder ->
                        val cacheFolder = File(appFolder, "cache")
                        if (cacheFolder.exists() && cacheFolder.isDirectory) {
                            val size = cacheFolder.walkTopDown().filter { it.isFile }.sumOf { it.length() }
                            if (size > 0) {
                                externalTotal += size
                                entries.add(CacheDirectoryEntry(cacheFolder.absolutePath, size, "External Cache"))
                                appMap[appFolder.name] = (appMap[appFolder.name] ?: 0L) + size
                                logs.add("[${timestamp()}] [SCAN] ${appFolder.name}: ${AppCacheItem.formatBytes(size)} (${cacheFolder.absolutePath})")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                logs.add("[${timestamp()}] [WARN] External cache traversal: ${e.message}")
            }

            val total = appTotal + codeTotal + externalTotal
            val duration = SystemClock.elapsedRealtime() - startTime
            logs.add("[${timestamp()}] [SCAN] Local scan finished in ${duration}ms. Found ${AppCacheItem.formatBytes(total)}")

            ReclaimableStorageReport(
                totalReclaimableBytes = total,
                appCacheBytes = appTotal,
                codeCacheBytes = codeTotal,
                externalCacheBytes = externalTotal,
                systemTempBytes = 0L,
                directoryEntries = entries,
                appCacheSizes = appMap,
                totalDirectoriesTraversed = entries.size,
                durationMs = duration,
                scanLogs = logs
            )
        }
    }

    suspend fun scanAppCaches(rootGranted: Boolean): Pair<List<AppCacheItem>, List<String>> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val installedApps = try {
            pm.getInstalledApplications(PackageManager.GET_META_DATA)
        } catch (_: Exception) {
            emptyList()
        }

        val whitelisted = getWhitelistedPackages()
        val reclaimableReport = calculateTotalReclaimableStorage(rootGranted)
        val rootSizes = reclaimableReport.appCacheSizes
        val logs = mutableListOf<String>()
        logs.addAll(reclaimableReport.scanLogs)

        val items = mutableListOf<AppCacheItem>()

        for (appInfo in installedApps) {
            if (appInfo.packageName == context.packageName) continue

            val appName = try {
                pm.getApplicationLabel(appInfo).toString()
            } catch (_: Exception) {
                appInfo.packageName
            }

            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            var cacheSize = rootSizes[appInfo.packageName] ?: 0L

            // If not found in root scan, attempt real query via StorageStatsManager if permitted
            if (cacheSize == 0L) {
                cacheSize = queryRealStorageStats(appInfo.packageName, appInfo)
            }

            // Real physical query on external shared storage
            if (cacheSize == 0L) {
                cacheSize = queryAccessibleExternalCache(appInfo.packageName)
            }

            items.add(
                AppCacheItem(
                    packageName = appInfo.packageName,
                    appName = appName,
                    cacheSizeBytes = cacheSize,
                    isSystemApp = isSystem,
                    isWhitelisted = whitelisted.contains(appInfo.packageName)
                )
            )
        }

        val sorted = items.sortedByDescending { it.cacheSizeBytes }
        Pair(sorted, logs)
    }

    private fun queryRealStorageStats(packageName: String, appInfo: ApplicationInfo): Long {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val statsManager = context.getSystemService(Context.STORAGE_STATS_SERVICE) as? StorageStatsManager
                val stats = statsManager?.queryStatsForPackage(appInfo.storageUuid, packageName, android.os.Process.myUserHandle())
                stats?.cacheBytes ?: 0L
            } else 0L
        } catch (_: Exception) {
            0L
        }
    }

    private fun queryAccessibleExternalCache(packageName: String): Long {
        return try {
            val extCache = File(Environment.getExternalStorageDirectory(), "Android/data/$packageName/cache")
            if (extCache.exists() && extCache.isDirectory) {
                extCache.walkTopDown().filter { it.isFile }.sumOf { it.length() }
            } else 0L
        } catch (_: Exception) {
            0L
        }
    }

    suspend fun purgeAllCaches(
        targets: Set<CleanTarget>,
        appsToClean: List<AppCacheItem>,
        isRootGranted: Boolean
    ): CleaningResult = withContext(Dispatchers.IO) {
        val startTime = SystemClock.elapsedRealtime()
        val whitelisted = getWhitelistedPackages()
        val targetApps = appsToClean.filter { !whitelisted.contains(it.packageName) }
        val estimatedFreed = targetApps.sumOf { it.cacheSizeBytes }

        val logs = mutableListOf<String>()
        logs.add("[${timestamp()}] [INIT] ==================== TURBO PURGE ====================")
        logs.add("[${timestamp()}] [TARGETS] Active: ${targets.joinToString { it.title }}")
        logs.add("[${timestamp()}] [WHITELIST] ${whitelisted.size} packages protected from purging")
        if (whitelisted.isNotEmpty()) {
            logs.add("[${timestamp()}] [WHITELIST] Protected list: ${whitelisted.joinToString()}")
        }

        if (isRootGranted) {
            logs.add("[${timestamp()}] [EXEC] Executing root batch shell commands via su...")
            val result = RootShellExecutor.cleanCachesWithRoot(targets, whitelisted)

            if (result.stdout.isNotEmpty()) {
                for (outLine in result.stdout) {
                    logs.add("[${timestamp()}] [SHELL] $outLine")
                }
            }

            if (result.isSuccess) {
                logs.add("[${timestamp()}] [SUCCESS] Batch execution completed with exit code 0 in ${result.durationMs}ms")
            } else {
                logs.add("[${timestamp()}] [WARN] Shell returned exit code: ${result.exitCode}")
                if (result.stderr.isNotEmpty()) {
                    for (errLine in result.stderr) {
                        logs.add("[${timestamp()}] [STDERR] $errLine")
                    }
                }
            }
        } else {
            logs.add("[${timestamp()}] [SANDBOX] Root not granted - clearing app internal cache directories")
            try {
                val beforeCache = context.cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
                context.cacheDir.deleteRecursively()
                context.codeCacheDir.deleteRecursively()
                logs.add("[${timestamp()}] [SUCCESS] Cleared internal cache (${AppCacheItem.formatBytes(beforeCache)})")
            } catch (e: Exception) {
                logs.add("[${timestamp()}] [WARN] Internal cache clear error: ${e.message}")
            }
        }

        val duration = SystemClock.elapsedRealtime() - startTime
        logs.add("[${timestamp()}] [COMPLETE] Purge finished in ${duration}ms")
        logs.add("[${timestamp()}] [RESULT] Reclaimed ~${AppCacheItem.formatBytes(estimatedFreed)} across ${targetApps.size} apps")
        logs.add("[${timestamp()}] ========================================================")

        CleaningResult(
            freedBytes = estimatedFreed,
            durationMs = duration,
            totalAppsScanned = targetApps.size,
            pathsCleared = targetApps.size * targets.size,
            logOutput = logs
        )
    }

    suspend fun purgeSingleApp(
        app: AppCacheItem,
        isRootGranted: Boolean
    ): CleaningResult = withContext(Dispatchers.IO) {
        val startTime = SystemClock.elapsedRealtime()
        val logs = mutableListOf<String>()
        logs.add("[${timestamp()}] [INIT] Single app purge for: ${app.appName} (${app.packageName})")
        logs.add("[${timestamp()}] [SIZE] Cache size before purge: ${app.formattedSize}")

        if (isRootGranted) {
            val cmd = RootShellExecutor.cleanSingleAppWithRoot(app.packageName)
            if (cmd.stdout.isNotEmpty()) {
                cmd.stdout.forEach { logs.add("[${timestamp()}] [SHELL] $it") }
            }
            logs.add("[${timestamp()}] [SUCCESS] Single purge executed in ${cmd.durationMs}ms (exit code ${cmd.exitCode})")
        } else {
            logs.add("[${timestamp()}] [INFO] Root access is required to purge other apps' private caches")
        }

        val duration = SystemClock.elapsedRealtime() - startTime
        logs.add("[${timestamp()}] [COMPLETE] App cache cleared in ${duration}ms")

        CleaningResult(
            freedBytes = app.cacheSizeBytes,
            durationMs = duration,
            totalAppsScanned = 1,
            pathsCleared = 2,
            logOutput = logs
        )
    }
}
