package com.willykez.codeorganizer.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.willykez.codeorganizer.model.AiProvider
import com.willykez.codeorganizer.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    var providerMenuExpanded by remember { mutableStateOf(false) }
    var apiKeyField by remember(state.provider, state.apiKey) { mutableStateOf(state.apiKey) }
    var modelField by remember(state.provider, state.model) { mutableStateOf(state.model) }
    var baseUrlField by remember(state.provider, state.baseUrl) { mutableStateOf(state.baseUrl) }
    var showKey by remember { mutableStateOf(false) }

    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            val contentResolver = viewModel.getApplication<android.app.Application>().contentResolver
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            viewModel.saveTreeUri(uri)
        }
    }

    fun saveCurrentFields() {
        viewModel.saveApiKey(apiKeyField)
        viewModel.saveModel(modelField)
        viewModel.saveBaseUrl(baseUrlField)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionCard(title = "AI provider") {
                // A Box with a clickable "field" + a DropdownMenu anchored to it, rather than
                // ExposedDropdownMenuBox — keeps this resilient across Material3 versions where
                // menuAnchor()'s signature has been a moving target.
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = state.provider.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Provider") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    // Transparent overlay: a read-only OutlinedTextField won't reliably forward
                    // clicks, so this catches the tap and opens the menu instead.
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { providerMenuExpanded = true }
                    )
                    DropdownMenu(
                        expanded = providerMenuExpanded,
                        onDismissRequest = { providerMenuExpanded = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AiProvider.entries.forEach { provider ->
                            DropdownMenuItem(
                                text = { Text(provider.displayName) },
                                onClick = {
                                    providerMenuExpanded = false
                                    viewModel.selectProvider(provider)
                                }
                            )
                        }
                    }
                }

                Text(
                    "Each provider keeps its own key, model, and base URL — switching back " +
                        "won't lose what you entered before.",
                    style = MaterialTheme.typography.bodySmall
                )

                Text(state.provider.apiKeyHelp, style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = apiKeyField,
                    onValueChange = { apiKeyField = it },
                    label = { Text("API key") },
                    singleLine = true,
                    visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showKey = !showKey }) {
                            Icon(
                                if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle visibility"
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = modelField,
                    onValueChange = { modelField = it },
                    label = { Text("Model name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Tap a suggestion or type any model name this provider supports:",
                    style = MaterialTheme.typography.bodySmall
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    state.provider.sampleModels.forEach { sample ->
                        AssistChip(
                            onClick = { modelField = sample },
                            label = { Text(sample) }
                        )
                    }
                }

                OutlinedTextField(
                    value = baseUrlField,
                    onValueChange = { baseUrlField = it },
                    label = { Text("Base URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Only change this for a self-hosted / regional / proxy endpoint — " +
                        "the default matches ${state.provider.displayName}'s standard API.",
                    style = MaterialTheme.typography.bodySmall
                )

                Button(
                    onClick = { saveCurrentFields() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save")
                }
            }

            SectionCard(title = "Destination folder") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (state.treeUriString.isBlank()) Icons.Default.Folder else Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        if (state.treeUriString.isBlank())
                            "  No folder selected yet."
                        else
                            "  Folder selected. Files will be created/overwritten inside it.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                TextButton(onClick = { folderPicker.launch(null) }) {
                    Icon(Icons.Default.CreateNewFolder, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("  Choose folder")
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}
