package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.AppCacheItem
import com.example.data.model.CleaningResult

@Composable
fun CleanCompleteDialog(
    result: CleaningResult,
    onDismiss: () -> Unit,
    onViewLogs: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Cache Cleaned")
        },
        text = {
            Column {
                Text(
                    text = "Freed ${AppCacheItem.formatBytes(result.freedBytes)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Cleaned ${result.totalAppsScanned} apps in ${result.durationMs} ms.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dialog_done_button")
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismiss()
                    onViewLogs()
                },
                modifier = Modifier.testTag("dialog_view_logs_button")
            ) {
                Text("View Log")
            }
        },
        modifier = Modifier.testTag("clean_complete_dialog")
    )
}
