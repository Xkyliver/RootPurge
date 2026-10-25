package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.repository.CacheCleanerRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("RootPurge", appName)
  }

  @Test
  fun `verify cache repository storage stats`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repository = CacheCleanerRepository(context)
    val stats = repository.getStorageStats()
    assertNotNull(stats)
  }

  @Test
  fun `verify directory traversal calculation function`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repository = CacheCleanerRepository(context)
    val report = repository.calculateTotalReclaimableStorage(isRootGranted = false)
    assertNotNull(report)
    assertNotNull(report.directoryEntries)
  }
}
