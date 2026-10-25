package com.example.data.model

sealed interface RootState {
    data object Checking : RootState
    data class Granted(
        val suVersion: String,
        val suPath: String,
        val provider: String = "Magisk / KernelSU / APatch"
    ) : RootState
    data class Denied(
        val reason: String = "SU request was rejected or timed out"
    ) : RootState
    data class Unavailable(
        val reason: String = "No SU binary detected on device"
    ) : RootState
}

data class CleaningResult(
    val freedBytes: Long,
    val durationMs: Long,
    val totalAppsScanned: Int,
    val pathsCleared: Int,
    val logOutput: List<String>,
    val timestamp: Long = System.currentTimeMillis()
)
