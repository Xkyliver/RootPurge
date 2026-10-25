package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RootState

@Composable
fun RootStatusChip(
    rootState: RootState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (statusText, icon, color) = when (rootState) {
        is RootState.Granted -> Triple("Root: Active", Icons.Default.Check, MaterialTheme.colorScheme.primary)
        is RootState.Denied -> Triple("Root: Denied", Icons.Default.Warning, MaterialTheme.colorScheme.error)
        is RootState.Unavailable -> Triple("Root: Not detected", Icons.Default.Info, MaterialTheme.colorScheme.tertiary)
        RootState.Checking -> Triple("Root: Checking...", Icons.Default.Info, MaterialTheme.colorScheme.outline)
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag("root_status_chip")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = statusText,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                color = color
            )
        }
    }
}
