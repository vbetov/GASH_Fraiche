package com.gash.vocab.ui.browse

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gash.vocab.data.db.ProgressEntity
import com.gash.vocab.data.db.WordEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseScreen(vm: BrowseViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    var filtersExpanded by remember { mutableStateOf(true) }
    val listState = rememberLazyListState()

    // Auto-collapse filters when user starts scrolling
    val isScrolling by remember { derivedStateOf { listState.isScrollInProgress } }
    LaunchedEffect(isScrolling) {
        if (isScrolling && filtersExpanded) {
            filtersExpanded = false
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Collapsible filter section
        AnimatedVisibility(
            visible = filtersExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column {
                // Search bar
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { vm.setSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Search words...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    singleLine = true
                )

                // Filters row 1: Weeks
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterDropdown(
                        label = "Weeks",
                        selected = state.filterWeeks,
                        options = state.allWeeks,
                        onSelect = { vm.setFilterWeeks(it) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(4.dp))

                // Filters row 2: POS + Source
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterDropdown(
                        label = "Part of Speech",
                        selected = state.filterPos,
                        options = state.allPos,
                        onSelect = { vm.setFilterPos(it) },
                        modifier = Modifier.weight(1f)
                    )

                    FilterDropdown(
                        label = "Source",
                        selected = state.filterSource,
                        options = state.allSources,
                        onSelect = { vm.setFilterSource(it) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(4.dp))

                // Completion chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CompletionFilterChips(
                        selected = state.filterCompletion,
                        onSelect = { vm.setFilterCompletion(it) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Sort row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SortDropdown(
                        selected = state.sortField,
                        onSelect = { vm.setSortField(it) },
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(onClick = { vm.toggleSortDirection() }) {
                        Icon(
                            imageVector = if (state.sortAscending) Icons.Default.ArrowUpward
                            else Icons.Default.ArrowDownward,
                            contentDescription = if (state.sortAscending) "Ascending" else "Descending",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text(
                        text = "${state.words.size} words",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Collapsed toggle bar — shown when filters are hidden
        if (!filtersExpanded) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { filtersExpanded = true },
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${state.words.size} words",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = "Show filters",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Word list
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(state.words, key = { it.id }) { word ->
                WordCard(
                    word = word,
                    progress = state.progressMap[word.id],
                    expanded = state.expandedWordId == word.id,
                    onToggle = { vm.toggleExpanded(word.id) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompletionFilterChips(
    selected: CompletionLevel,
    onSelect: (CompletionLevel) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        CompletionLevel.entries.forEach { level ->
            FilterChip(
                selected = selected == level,
                onClick = { onSelect(level) },
                label = { Text(level.label, style = MaterialTheme.typography.labelSmall) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortDropdown(
    selected: SortField,
    onSelect: (SortField) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = "Sort: ${selected.label}",
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            SortField.entries.forEach { field ->
                DropdownMenuItem(
                    text = { Text(field.label) },
                    onClick = { onSelect(field); expanded = false }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterDropdown(
    label: String,
    selected: String?,
    options: List<String>,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selected ?: "All",
            onValueChange = {},
            readOnly = true,
            label = { Text(label, maxLines = 1) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("All") },
                onClick = { onSelect(null); expanded = false }
            )
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = { onSelect(option); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun WordCard(
    word: WordEntity,
    progress: ProgressEntity?,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onToggle() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = word.french,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = word.english,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Stats summary
                if (progress != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatChip("✓", progress.knewCheck, MaterialTheme.colorScheme.primary)
                        StatChip("✍", progress.knewCloze, MaterialTheme.colorScheme.tertiary)
                        StatChip("✗", progress.didntKnow, MaterialTheme.colorScheme.error)
                    }
                }

                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    DetailRow("POS", word.pos)
                    DetailRow("Source", word.source)
                    if (word.weeks.isNotBlank()) {
                        DetailRow("Weeks", word.weeks)
                    }

                    if (word.example.isNotBlank()) {
                        DetailRow("Example", word.example, italic = true)
                    }

                    if (word.notes.isNotBlank()) {
                        DetailRow("Notes", word.notes)
                    }

                    if (progress != null) {
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Performance",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(4.dp))
                        DetailRow("First seen", progress.firstEncountered?.take(10) ?: "—")
                        DetailRow("Last seen", progress.lastEncountered?.take(10) ?: "—")
                        DetailRow("Check ✓", "${progress.knewCheck}")
                        DetailRow("Cloze ✍", "${progress.knewCloze}")
                        DetailRow("Choice", "${progress.knewChoice}")
                        DetailRow("Didn't know ✗", "${progress.didntKnow}")
                        DetailRow("EF", String.format("%.2f", progress.easeFactor))
                        DetailRow("Interval", "${progress.intervalDays} days")
                    }
                }
            }
        }
    }
}

@Composable
private fun StatChip(symbol: String, count: Int, color: androidx.compose.ui.graphics.Color) {
    if (count > 0) {
        Text(
            text = "$symbol$count",
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String, italic: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
