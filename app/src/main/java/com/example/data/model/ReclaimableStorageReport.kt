package com.example.data.model

data class CacheDirectoryEntry(
    val directoryPath: String,
    val sizeBytes: Long,
    val category: String
)

data class ReclaimableStorageReport(
    val totalReclaimableBytes: Long,
    val appCacheBytes: Long,
    val codeCacheBytes: Long,
    val externalCacheBytes: Long,
    val systemTempBytes: Long,
    val directoryEntries: List<CacheDirectoryEntry>,
    val appCacheSizes: Map<String, Long>,
    val totalDirectoriesTraversed: Int,
    val durationMs: Long,
    val scanLogs: List<String> = emptyList()
) {
    val formattedTotal: String
        get() = AppCacheItem.formatBytes(totalReclaimableBytes)
}
