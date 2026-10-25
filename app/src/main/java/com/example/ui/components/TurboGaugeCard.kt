package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppCacheItem
import com.example.data.model.ReclaimableStorageReport
import com.example.data.repository.CacheCleanerRepository

@Composable
fun TurboGaugeCard(
    totalCacheBytes: Long,
    isPurging: Boolean,
    purgeProgress: Float,
    purgeStageText: String,
    lastDurationMs: Long,
    storageStats: CacheCleanerRepository.StorageStats?,
    reclaimableReport: ReclaimableStorageReport?,
    onPurgeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .testTag("turbo_gauge_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Reclaimable Cache",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = AppCacheItem.formatBytes(totalCacheBytes),
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color = if (totalCacheBytes > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary
            )

            // Plain directory traversal breakdown
            if (reclaimableReport != null && reclaimableReport.totalReclaimableBytes > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "App: ${AppCacheItem.formatBytes(reclaimableReport.appCacheBytes)} • Code: ${AppCacheItem.formatBytes(reclaimableReport.codeCacheBytes)} • Media: ${AppCacheItem.formatBytes(reclaimableReport.externalCacheBytes)}",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (reclaimableReport.totalDirectoriesTraversed > 0) {
                    Text(
                        text = "Traversed ${reclaimableReport.totalDirectoriesTraversed} directories across /data/data/* in ${reclaimableReport.durationMs}ms",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            } else if (storageStats != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${AppCacheItem.formatBytes(storageStats.availableBytes)} free on disk • ${(storageStats.usedPercent * 100).toInt()}% used",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (lastDurationMs > 0) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Last cleaned in ${lastDurationMs} ms",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isPurging) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = purgeStageText.ifBlank { "Cleaning cache..." },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                Button(
                    onClick = onPurgeClick,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("single_click_purge_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Clean all",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CLEAN ALL CACHE",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}
