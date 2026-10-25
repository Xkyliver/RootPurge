package com.example.data.model

data class AppCacheItem(
    val packageName: String,
    val appName: String,
    val cacheSizeBytes: Long,
    val isSystemApp: Boolean = false,
    val isWhitelisted: Boolean = false
) {
    val formattedSize: String
        get() = formatBytes(cacheSizeBytes)

    companion object {
        fun formatBytes(bytes: Long): String {
            if (bytes <= 0) return "0 B"
            val kb = bytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format("%.2f GB", gb)
                mb >= 1.0 -> String.format("%.1f MB", mb)
                kb >= 1.0 -> String.format("%.0f KB", kb)
                else -> "$bytes B"
            }
        }
    }
}
