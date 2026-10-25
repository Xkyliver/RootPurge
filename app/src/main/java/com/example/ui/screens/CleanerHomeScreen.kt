package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppCacheItem
import com.example.ui.components.AppCacheItemCard
import com.example.ui.components.CleanCompleteDialog
import com.example.ui.components.CleanTargetsRow
import com.example.ui.components.RootDiagnosticBottomSheet
import com.example.ui.components.RootStatusChip
import com.example.ui.components.TerminalLogDrawer
import com.example.ui.components.TurboGaugeCard
import com.example.ui.viewmodel.AppFilter
import com.example.ui.viewmodel.CleanerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleanerHomeScreen(
    viewModel: CleanerViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "RootPurge",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                actions = {
                    RootStatusChip(
                        rootState = state.rootState,
                        onClick = { viewModel.setRootDiagnosticVisible(true) }
                    )

                    IconButton(
                        onClick = { viewModel.toggleTerminalDrawer() },
                        modifier = Modifier.testTag("top_bar_terminal_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = "Shell Log",
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.checkRootAndScan() },
                        enabled = !state.isScanning,
                        modifier = Modifier.testTag("top_bar_refresh_button")
                    ) {
                        if (state.isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Main Clean Action Card
            item(key = "gauge_card") {
                TurboGaugeCard(
                    totalCacheBytes = state.totalCacheSizeBytes,
                    isPurging = state.isPurging,
                    purgeProgress = state.purgeProgress,
                    purgeStageText = state.purgeStageText,
                    lastDurationMs = state.lastCleanDurationMs,
                    storageStats = state.storageStats,
                    reclaimableReport = state.reclaimableReport,
                    onPurgeClick = { viewModel.performSingleClickTurboPurge() }
                )
            }

            // Target categories
            item(key = "targets") {
                CleanTargetsRow(
                    selectedTargets = state.selectedTargets,
                    onToggleTarget = { viewModel.toggleTarget(it) }
                )
            }

            // Shell Log console if opened
            if (state.showTerminalDrawer) {
                item(key = "terminal_logs") {
                    TerminalLogDrawer(
                        logs = state.terminalLogs,
                        onClose = { viewModel.toggleTerminalDrawer() }
                    )
                }
            }

            // App list search & filter
            item(key = "search_and_filters") {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("Search apps...", style = MaterialTheme.typography.bodySmall) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            if (state.searchQuery.isNotEmpty()) {
                                IconButton(onClick = {
                                    viewModel.setSearchQuery("")
                                    focusManager.clearFocus()
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("app_search_field")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val filters = listOf(
                            AppFilter.ALL to "All (${state.allApps.size})",
                            AppFilter.USER_APPS to "User",
                            AppFilter.SYSTEM_APPS to "System",
                            AppFilter.HEAVY_ONLY to ">50 MB"
                        )

                        items(filters) { (filter, label) ->
                            FilterChip(
                                selected = state.activeFilter == filter,
                                onClick = { viewModel.setFilter(filter) },
                                label = { Text(label, style = MaterialTheme.typography.bodySmall) },
                                modifier = Modifier.testTag("filter_${filter.name.lowercase()}")
                            )
                        }
                    }
                }
            }

            // Apps list
            if (state.isScanning && state.allApps.isEmpty()) {
                item(key = "loading") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Scanning caches...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (state.filteredApps.isEmpty()) {
                item(key = "empty") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (state.searchQuery.isNotBlank()) "No apps found" else "All caches clean",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(
                    items = state.filteredApps,
                    key = { it.packageName }
                ) { appItem ->
                    AppCacheItemCard(
                        item = appItem,
                        onToggleWhitelist = { viewModel.toggleAppWhitelist(appItem.packageName) },
                        onCleanSingle = { viewModel.purgeSingleApp(appItem) }
                    )
                }
            }

            item(key = "spacer") {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (state.showResultDialog && state.lastResult != null) {
        CleanCompleteDialog(
            result = state.lastResult!!,
            onDismiss = { viewModel.dismissResultDialog() },
            onViewLogs = {
                if (!state.showTerminalDrawer) {
                    viewModel.toggleTerminalDrawer()
                }
            }
        )
    }

    if (state.showRootDiagnostic) {
        RootDiagnosticBottomSheet(
            sheetState = sheetState,
            rootState = state.rootState,
            onRecheck = {
                viewModel.checkRootAndScan()
                viewModel.setRootDiagnosticVisible(false)
            },
            onDismiss = { viewModel.setRootDiagnosticVisible(false) }
        )
    }
}
