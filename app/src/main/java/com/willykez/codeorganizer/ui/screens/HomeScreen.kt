package com.willykez.codeorganizer.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.willykez.codeorganizer.model.LogLine
import com.willykez.codeorganizer.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onOpenSettings: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val ready = state.apiKey.isNotBlank() && state.treeUriString.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Code Organizer") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SetupStatusCard(
                apiKeyReady = state.apiKey.isNotBlank(),
                folderReady = state.treeUriString.isNotBlank(),
                providerLabel = state.provider.displayName
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Paste project source", style = MaterialTheme.typography.titleMedium)
                if (state.pastedSource.isNotBlank()) {
                    TextButton(onClick = viewModel::clearSource) {
                        Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.height(16.dp))
                        Text("  Clear")
                    }
                }
            }
            OutlinedTextField(
                value = state.pastedSource,
                onValueChange = viewModel::onSourceChanged,
                placeholder = { Text("Paste a source dump, an ASCII folder tree, or both…") },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            Button(
                onClick = viewModel::organize,
                enabled = !state.isWorking && ready && state.pastedSource.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isWorking) {
                    CircularProgressIndicator(modifier = Modifier.height(18.dp), color = Color.White)
                    Text("  Organizing…")
                } else {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Text("  Organize into folder")
                }
            }

            if (state.log.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Log", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = viewModel::clearLog) {
                        Text("Clear")
                    }
                }
                LogPanel(log = state.log, modifier = Modifier.weight(1f))
            }
        }
    }

    // Preview & confirm: nothing is written to disk until the user taps "Write files".
    val projectName = state.pendingProjectName
    val pending = state.pendingFiles
    if (projectName != null && pending != null) {
        AlertDialog(
            onDismissRequest = viewModel::cancelPending,
            title = { Text("Write '$projectName' (${pending.size} file(s))?") },
            text = {
                Column {
                    Text(
                        "Everything below will be created inside a \"$projectName\" folder " +
                            "in your picked destination.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Box(modifier = Modifier.height(240.dp).padding(top = 8.dp)) {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(pending) { file ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.InsertDriveFile,
                                        contentDescription = null,
                                        modifier = Modifier.height(16.dp)
                                    )
                                    Text(
                                        "  ${file.path}",
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = viewModel::confirmWrite) { Text("Write files") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelPending) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SetupStatusCard(
    apiKeyReady: Boolean,
    folderReady: Boolean,
    providerLabel: String
) {
    AnimatedVisibility(visible = !apiKeyReady || !folderReady) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Finish setup in Settings",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                ChecklistLine("$providerLabel API key", apiKeyReady)
                ChecklistLine("Destination folder", folderReady)
            }
        }
    }
}

@Composable
private fun ChecklistLine(label: String, done: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (done) Icons.Default.CheckCircle else Icons.Default.WarningAmber,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.height(16.dp)
        )
        Text(
            "  $label",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}

@Composable
private fun LogPanel(log: List<LogLine>, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()
    LaunchedEffect(log.size) {
        if (log.isNotEmpty()) listState.animateScrollToItem(log.lastIndex)
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                RoundedCornerShape(8.dp)
            )
            .padding(12.dp)
    ) {
        LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(log) { line ->
                Text(
                    line.text,
                    color = if (line.isError) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
