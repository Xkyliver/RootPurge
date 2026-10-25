package com.example

import com.example.data.model.AppCacheItem
import com.example.data.model.CacheDirectoryEntry
import com.example.data.model.CleanTarget
import com.example.data.model.ReclaimableStorageReport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testAppCacheItem_formatBytes() {
        assertEquals("0 B", AppCacheItem.formatBytes(0))
        assertEquals("512 B", AppCacheItem.formatBytes(512))
        assertEquals("1.0 MB", AppCacheItem.formatBytes(1024 * 1024))
        assertEquals("1.5 MB", AppCacheItem.formatBytes((1.5 * 1024 * 1024).toLong()))
        assertEquals("2.00 GB", AppCacheItem.formatBytes(2L * 1024 * 1024 * 1024))
    }

    @Test
    fun testCleanTarget_defaults() {
        assertTrue(CleanTarget.APP_CACHE.defaultEnabled)
        assertTrue(CleanTarget.CODE_CACHE.defaultEnabled)
        assertTrue(CleanTarget.entries.size >= 4)
    }

    @Test
    fun testReclaimableStorageReport_calculation() {
        val entries = listOf(
            CacheDirectoryEntry("/data/data/com.example.app/cache", 1024 * 1024L, "App Cache"),
            CacheDirectoryEntry("/data/data/com.example.app/code_cache", 512 * 1024L, "Code Cache")
        )
        val report = ReclaimableStorageReport(
            totalReclaimableBytes = 1536 * 1024L,
            appCacheBytes = 1024 * 1024L,
            codeCacheBytes = 512 * 1024L,
            externalCacheBytes = 0L,
            systemTempBytes = 0L,
            directoryEntries = entries,
            appCacheSizes = mapOf("com.example.app" to 1536 * 1024L),
            totalDirectoriesTraversed = entries.size,
            durationMs = 24L
        )

        assertEquals("1.5 MB", report.formattedTotal)
        assertEquals(2, report.totalDirectoriesTraversed)
        assertEquals(1, report.appCacheSizes.size)
    }
}
