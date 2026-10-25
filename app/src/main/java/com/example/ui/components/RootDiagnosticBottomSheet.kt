package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.RootState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RootDiagnosticBottomSheet(
    sheetState: SheetState,
    rootState: RootState,
    onRecheck: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.testTag("root_diagnostic_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 28.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Root Status",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(12.dp))

            when (rootState) {
                is RootState.Granted -> {
                    Text(
                        text = "Root is active via ${rootState.provider}.\nBinary: ${rootState.suPath}\nVersion: ${rootState.suVersion}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                is RootState.Denied -> {
                    Text(
                        text = "Root access was denied. Grant permission in Magisk / KernelSU / APatch to enable single-click cache cleaning.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                is RootState.Unavailable -> {
                    Text(
                        text = "No su binary found. The device is not rooted or su is missing. Running in internal sandbox mode.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                RootState.Checking -> {
                    Text(
                        text = "Checking root status...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Why root is needed: Android isolates app caches (/data/data/*/cache) per app. Only root (UID 0) can clear all other apps' caches with a single click.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
                Button(
                    onClick = onRecheck,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("recheck_root_button")
                ) {
                    Text("Re-check Root")
                }
            }
        }
    }
}
