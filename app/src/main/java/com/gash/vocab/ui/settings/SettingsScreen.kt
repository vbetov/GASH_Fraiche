package com.gash.vocab.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun SettingsScreen(onShowStats: () -> Unit = {}, onCloseApp: () -> Unit = {}, vm: SettingsViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current

    // Dialog state for database import flow
    var showDbConfirmDialog by remember { mutableStateOf(false) }
    var showExportFirstDialog by remember { mutableStateOf(false) }

    // File picker launchers
    val vocabImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { vm.importVocab(context, it) } }

    val progressImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { vm.importProgress(context, it) } }

    val progressExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { vm.exportProgress(context, it) } }

    val dbImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { vm.importDatabase(context, it) } }

    // Step 1: "Are you sure?" dialog
    if (showDbConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDbConfirmDialog = false },
            title = { Text("Replace database?") },
            text = {
                Text("Are you sure? This will overwrite ALL vocabulary and progress data with the contents of the selected file. This cannot be undone.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showDbConfirmDialog = false
                    showExportFirstDialog = true
                }) {
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDbConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Step 2: "Export progress first?" dialog
    if (showExportFirstDialog) {
        AlertDialog(
            onDismissRequest = { showExportFirstDialog = false },
            title = { Text("Export progress first?") },
            text = {
                Text("Would you like to export your current progress data before replacing the database?")
            },
            confirmButton = {
                TextButton(onClick = {
                    showExportFirstDialog = false
                    progressExportLauncher.launch("gash_progress_${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmm"))}.json")
                    // After export, user will need to tap Import Database again
                }) {
                    Text("Yes, export first")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showExportFirstDialog = false
                    dbImportLauncher.launch(arrayOf("application/octet-stream", "*/*"))
                }) {
                    Text("No, proceed")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        // ── View Statistics ──────────────────────────────────

        Button(
            onClick = onShowStats,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("📊")
            Spacer(Modifier.width(8.dp))
            Text("View Statistics")
        }

        // ── Close App ────────────────────────────────────────

        OutlinedButton(
            onClick = onCloseApp,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Text("Close App & Backup")
        }

        // ── Progress Import/Export ────────────────────────────

        SettingsCard("Progress Data") {
            Text(
                text = "Export your study progress to a file or restore from a backup.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        progressExportLauncher.launch("gash_progress_${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmm"))}.json")
                    },
                    enabled = !state.isExporting,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("⬆️")
                    Spacer(Modifier.width(4.dp))
                    Text("Export")
                }

                Button(
                    onClick = {
                        progressImportLauncher.launch(arrayOf("application/json"))
                    },
                    enabled = !state.isImporting,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("⬇️")
                    Spacer(Modifier.width(4.dp))
                    Text("Import")
                }
            }

            state.progressMessage?.let { msg ->
                Spacer(Modifier.height(8.dp))
                StatusText(msg)
            }
        }

        // ── Vocabulary Import ─────────────────────────────────

        SettingsCard("Vocabulary Import") {
            Text(
                text = "Import new vocabulary entries from a JSON file. " +
                        "Existing words are updated; new words are added.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = { vocabImportLauncher.launch(arrayOf("application/json")) },
                enabled = !state.isImporting,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isImporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Importing...")
                } else {
                    Text("📂")
                    Spacer(Modifier.width(8.dp))
                    Text("Select JSON File")
                }
            }

            state.importMessage?.let { msg ->
                Spacer(Modifier.height(8.dp))
                StatusText(msg)
            }
        }

        // ── Study Limits ──────────────────────────────────────

        SettingsCard("Study Limits") {
            SettingSlider(
                label = "New cards per day",
                value = state.newPerDay,
                range = 1f..50f,
                onValueChange = { vm.setNewPerDay(it) }
            )

            Spacer(Modifier.height(8.dp))

            if (state.isUncappedToday) {
                OutlinedButton(
                    onClick = { vm.toggleUncapToday() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Uncapped for today — tap to restore limit")
                }
            } else {
                TextButton(
                    onClick = { vm.toggleUncapToday() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Uncap new cards for today")
                }
            }

            Spacer(Modifier.height(12.dp))

            SettingSlider(
                label = "Reviews per day",
                value = state.reviewsPerDay,
                range = 10f..200f,
                onValueChange = { vm.setReviewsPerDay(it) }
            )
        }

        // ── Review Settings ───────────────────────────────────

        SettingsCard("Review Settings") {
            OutlinedTextField(
                value = state.learningSteps,
                onValueChange = { vm.setLearningSteps(it) },
                label = { Text("Learning steps (min)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                supportingText = { Text("Comma-separated, e.g. 1,10") }
            )

            Spacer(Modifier.height(12.dp))

            SettingSlider(
                label = "Graduating interval (days)",
                value = state.graduatingInterval,
                range = 1f..15f,
                onValueChange = { vm.setGraduatingInterval(it) }
            )

            Spacer(Modifier.height(12.dp))

            SettingSlider(
                label = "Easy interval (days)",
                value = state.easyInterval,
                range = 1f..30f,
                onValueChange = { vm.setEasyInterval(it) }
            )
        }

        // ── Database File Import ─────────────────────────────

        SettingsCard("Import Database File") {
            Text(
                text = "Replace the entire database with a .db file from your device. " +
                        "A backup is automatically saved to Android/media/com.gash.vocab/backup/ whenever you close the app.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = { showDbConfirmDialog = true },
                enabled = !state.isDbImporting,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                if (state.isDbImporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onError
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Importing...")
                } else {
                    Text("📂")
                    Spacer(Modifier.width(8.dp))
                    Text("Import Database File")
                }
            }

            state.dbImportMessage?.let { msg ->
                Spacer(Modifier.height(8.dp))
                StatusText(msg)
            }
        }

        // ── POS Category Mapping ──────────────────────────────

        SettingsCard("POS Category Mapping") {
            Text(
                text = "Assign each part of speech to a display category. Unmapped values appear under \"Other\".",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(12.dp))

            state.posCategoryMap.toSortedMap().forEach { (pos, category) ->
                PosMappingRow(
                    pos = pos,
                    category = category,
                    onCategoryChange = { vm.updatePosCategory(pos, it) }
                )
            }

            Spacer(Modifier.height(8.dp))

            TextButton(
                onClick = { vm.resetPosMapping() },
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Reset to Defaults")
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun SettingSlider(
    label: String,
    value: Int,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Int) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "$value",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = range,
            steps = (range.endInclusive - range.start).toInt() - 1
        )
    }
}

@Composable
private fun PosMappingRow(
    pos: String,
    category: String,
    onCategoryChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = pos,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        OutlinedTextField(
            value = category,
            onValueChange = onCategoryChange,
            modifier = Modifier.weight(0.8f),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun StatusText(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = if (message.contains("fail", ignoreCase = true) || message.contains("error", ignoreCase = true))
            MaterialTheme.colorScheme.error
        else MaterialTheme.colorScheme.primary
    )
}
