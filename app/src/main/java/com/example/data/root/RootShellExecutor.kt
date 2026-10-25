package com.example.data.root

import android.os.Build
import android.os.SystemClock
import com.example.data.model.CacheDirectoryEntry
import com.example.data.model.CleanTarget
import com.example.data.model.ReclaimableStorageReport
import com.example.data.model.RootState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

object RootShellExecutor {

    private val SU_BINARY_LOCATIONS = arrayOf(
        "/system/bin/su",
        "/system/xbin/su",
        "/sbin/su",
        "/system/sd/xbin/su",
        "/system/bin/failsafe/su",
        "/data/local/xbin/su",
        "/data/local/bin/su",
        "/data/local/su"
    )

    data class CommandResult(
        val exitCode: Int,
        val stdout: List<String>,
        val stderr: List<String>,
        val durationMs: Long
    ) {
        val isSuccess: Boolean get() = exitCode == 0
    }

    fun isSuBinaryPresent(): Boolean {
        for (path in SU_BINARY_LOCATIONS) {
            try {
                if (File(path).exists()) return true
            } catch (_: Exception) {}
        }
        val pathEnv = System.getenv("PATH") ?: ""
        for (dir in pathEnv.split(":")) {
            try {
                if (File(dir, "su").exists()) return true
            } catch (_: Exception) {}
        }
        return false
    }

    suspend fun checkRootState(): RootState = withContext(Dispatchers.IO) {
        if (!isSuBinaryPresent()) {
            val probe = runCommand("su", "-c", "id", timeoutMs = 1500)
            if (!probe.isSuccess) {
                return@withContext RootState.Unavailable("No SU binary detected on system")
            }
        }

        val testResult = runCommand("su", "-c", "id", timeoutMs = 3000)
        val combinedOutput = (testResult.stdout + testResult.stderr).joinToString(" ")

        if (testResult.isSuccess && (combinedOutput.contains("uid=0") || combinedOutput.contains("root"))) {
            val versionResult = runCommand("su", "-v", timeoutMs = 1500)
            val suVersion = versionResult.stdout.firstOrNull()?.trim() ?: "SU SuperUser"

            var foundPath = SU_BINARY_LOCATIONS.firstOrNull { File(it).exists() }
            if (foundPath == null) {
                val whichResult = runCommand("which", "su", timeoutMs = 1000)
                foundPath = whichResult.stdout.firstOrNull()?.trim() ?: "/system/bin/su"
            }

            val providerName = when {
                suVersion.contains("MAGISK", ignoreCase = true) -> "MagiskSU"
                suVersion.contains("KSU", ignoreCase = true) || suVersion.contains("KernelSU", ignoreCase = true) -> "KernelSU"
                suVersion.contains("APatch", ignoreCase = true) -> "APatch"
                else -> "Superuser (Rooted)"
            }

            RootState.Granted(
                suVersion = suVersion,
                suPath = foundPath,
                provider = providerName
            )
        } else {
            RootState.Denied("Root access was denied or timed out by root manager")
        }
    }

    fun runCommand(vararg cmd: String, timeoutMs: Long = 8000): CommandResult {
        val startTime = SystemClock.elapsedRealtime()
        val stdoutList = mutableListOf<String>()
        val stderrList = mutableListOf<String>()
        var process: Process? = null

        return try {
            val processBuilder = ProcessBuilder(*cmd)
            process = processBuilder.start()

            val stdoutReader = BufferedReader(InputStreamReader(process.inputStream))
            val stderrReader = BufferedReader(InputStreamReader(process.errorStream))

            val completed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
            } else {
                Thread.sleep(100)
                true
            }

            if (!completed) {
                process.destroy()
                return CommandResult(
                    exitCode = -1,
                    stdout = emptyList(),
                    stderr = listOf("Command timed out after ${timeoutMs}ms"),
                    durationMs = SystemClock.elapsedRealtime() - startTime
                )
            }

            var line: String?
            while (stdoutReader.readLine().also { line = it } != null) {
                line?.let { stdoutList.add(it) }
            }
            while (stderrReader.readLine().also { line = it } != null) {
                line?.let { stderrList.add(it) }
            }

            val exitCode = process.exitValue()
            val duration = SystemClock.elapsedRealtime() - startTime
            CommandResult(exitCode, stdoutList, stderrList, duration)
        } catch (e: Exception) {
            process?.destroy()
            CommandResult(
                exitCode = -1,
                stdout = emptyList(),
                stderr = listOf("Execution error: ${e.message}"),
                durationMs = SystemClock.elapsedRealtime() - startTime
            )
        }
    }

    suspend fun executeSuScript(script: String, timeoutMs: Long = 15000): CommandResult = withContext(Dispatchers.IO) {
        runCommand("su", "-c", script, timeoutMs = timeoutMs)
    }

    suspend fun cleanCachesWithRoot(
        targets: Set<CleanTarget>,
        whitelistedPackages: Set<String>
    ): CommandResult = withContext(Dispatchers.IO) {
        val commands = mutableListOf<String>()

        if (targets.contains(CleanTarget.APP_CACHE)) {
            if (whitelistedPackages.isEmpty()) {
                commands.add("rm -rf /data/data/*/cache/* 2>/dev/null")
                commands.add("rm -rf /data/user/*/*/cache/* 2>/dev/null")
                commands.add("rm -rf /data/user_de/*/*/cache/* 2>/dev/null")
            } else {
                for (pkg in whitelistedPackages) {
                    commands.add("touch /data/data/$pkg/.keep 2>/dev/null")
                }
                commands.add("rm -rf /data/data/*/cache/* 2>/dev/null")
            }
        }

        if (targets.contains(CleanTarget.CODE_CACHE)) {
            commands.add("rm -rf /data/data/*/code_cache/* 2>/dev/null")
            commands.add("rm -rf /data/user/*/*/code_cache/* 2>/dev/null")
        }

        if (targets.contains(CleanTarget.EXTERNAL_CACHE)) {
            commands.add("rm -rf /sdcard/Android/data/*/cache/* 2>/dev/null")
            commands.add("rm -rf /storage/emulated/0/Android/data/*/cache/* 2>/dev/null")
        }

        if (targets.contains(CleanTarget.SYSTEM_TEMP)) {
            commands.add("rm -rf /cache/* 2>/dev/null")
            commands.add("rm -rf /data/local/tmp/* 2>/dev/null")
            commands.add("pm trim-caches 999999999999 2>/dev/null")
        }

        if (targets.contains(CleanTarget.DROP_RAM)) {
            commands.add("sync")
            commands.add("echo 3 > /proc/sys/vm/drop_caches 2>/dev/null")
        }

        val fullScript = if (commands.isNotEmpty()) commands.joinToString(" && ") else "true"
        executeSuScript(fullScript)
    }

    suspend fun cleanSingleAppWithRoot(packageName: String): CommandResult = withContext(Dispatchers.IO) {
        val script = "rm -rf /data/data/$packageName/cache/* /data/data/$packageName/code_cache/* /sdcard/Android/data/$packageName/cache/* 2>/dev/null"
        executeSuScript(script)
    }

    suspend fun queryCacheSizesWithRoot(): Map<String, Long> = withContext(Dispatchers.IO) {
        val report = traverseAndCalculateReclaimableStorage()
        report.appCacheSizes
    }

    suspend fun traverseAndCalculateReclaimableStorage(): ReclaimableStorageReport = withContext(Dispatchers.IO) {
        val startTime = SystemClock.elapsedRealtime()
        val entries = mutableListOf<CacheDirectoryEntry>()
        val appMap = mutableMapOf<String, Long>()

        val script = "du -sk /data/data/*/cache /data/data/*/code_cache /sdcard/Android/data/*/cache /cache /data/local/tmp 2>/dev/null"
        val cmd = executeSuScript(script, timeoutMs = 8000)

        var appCacheTotal = 0L
        var codeCacheTotal = 0L
        var externalCacheTotal = 0L
        var systemTempTotal = 0L

        for (line in cmd.stdout) {
            val parts = line.trim().split(Regex("\\s+"), limit = 2)
            if (parts.size == 2) {
                val sizeKb = parts[0].toLongOrNull() ?: continue
                val path = parts[1]
                val bytes = sizeKb * 1024L

                val category = when {
                    path.endsWith("/code_cache") || path.contains("/code_cache/") -> {
                        codeCacheTotal += bytes
                        "Code Cache"
                    }
                    path.contains("/Android/data/") -> {
                        externalCacheTotal += bytes
                        "External Cache"
                    }
                    path == "/cache" || path.startsWith("/cache/") || path.contains("/data/local/tmp") -> {
                        systemTempTotal += bytes
                        "System Temp"
                    }
                    else -> {
                        appCacheTotal += bytes
                        "App Cache"
                    }
                }

                entries.add(CacheDirectoryEntry(path, bytes, category))

                val segments = path.split("/")
                val pkgIndex = when {
                    path.contains("/data/data/") -> segments.indexOf("data") + 2
                    path.contains("/Android/data/") -> segments.indexOf("data") + 1
                    else -> -1
                }
                if (pkgIndex in segments.indices) {
                    val pkg = segments[pkgIndex]
                    appMap[pkg] = (appMap[pkg] ?: 0L) + bytes
                }
            }
        }

        val total = appCacheTotal + codeCacheTotal + externalCacheTotal + systemTempTotal
        val duration = SystemClock.elapsedRealtime() - startTime

        ReclaimableStorageReport(
            totalReclaimableBytes = total,
            appCacheBytes = appCacheTotal,
            codeCacheBytes = codeCacheTotal,
            externalCacheBytes = externalCacheTotal,
            systemTempBytes = systemTempTotal,
            directoryEntries = entries,
            appCacheSizes = appMap,
            totalDirectoriesTraversed = entries.size,
            durationMs = duration
        )
    }
}
