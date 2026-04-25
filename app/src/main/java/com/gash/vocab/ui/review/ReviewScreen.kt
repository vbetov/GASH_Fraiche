package com.gash.vocab.ui.review

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(vm: ReviewViewModel = viewModel()) {
    val state by vm.state.collectAsState()

    if (state.mode == ReviewMode.START_PAGE) {
        StartPage(
            weeks = state.allWeeks,
            posValues = state.allPos,
            noCardsDue = state.noCardsDue,
            onStartAll = { vm.startSession() },
            onStartWeek = { vm.startWeekSession(it) },
            onStartPos = { vm.startPosSession(it) }
        )
        return
    }

    if (state.sessionComplete) {
        SessionComplete(
            onRestart = { vm.startSession() },
            onBackToStart = { vm.backToStart() }
        )
        return
    }

    val word = state.currentWord ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Undo back button
        if (state.canUndo) {
            IconButton(
                onClick = { vm.undo() },
                modifier = Modifier.align(Alignment.Start)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Undo",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Progress indicator
        Text(
            text = "${state.queueIndex + 1} / ${state.queue.size}" +
                    if (state.selectedWeek != null) "  (${state.selectedWeek})"
                    else if (state.selectedPos != null) "  (${state.selectedPos})"
                    else "",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(32.dp))

        when (state.mode) {
            ReviewMode.START_PAGE -> {} // handled above
            ReviewMode.FRONT -> FrontCard(
                french = word.french,
                pos = word.pos,
                onCheck = { vm.doCheck() },
                onCloze = { vm.doCloze() },
                onChoice = { vm.doChoice() },
                onExplore = { vm.doExplore() },
                onDontKnow = { vm.doDontKnow() }
            )

            ReviewMode.CHECK -> CheckCard(
                french = word.french,
                english = word.english,
                example = word.example,
                onKnew = { vm.confirmCheck() },
                onDidntKnow = { vm.failCheck() },
                onFailAndExplore = { vm.failAndExplore() }
            )

            ReviewMode.CLOZE -> ClozeCard(
                clozeSentence = word.cloze.getOrElse(state.selectedClozeIndex) { "" },
                answer = word.french,
                english = word.english,
                revealed = state.clozeRevealed,
                onReveal = { vm.revealCloze() },
                onKnew = { vm.confirmCloze() },
                onDidntKnow = { vm.failCloze() },
                onFailAndExplore = { vm.failAndExplore() }
            )

            ReviewMode.CHOICE -> ChoiceCard(
                french = word.french,
                options = state.choiceOptions,
                correctAnswer = word.english,
                selectedAnswer = state.choiceAnswer,
                isCorrect = state.isCorrect,
                onSelect = { vm.selectChoice(it) },
                onNext = { vm.advanceFromChoice() },
                onExplore = { vm.doExplore() }
            )

            ReviewMode.EXPLORE -> ExploreCard(
                word = word,
                onNext = { vm.advanceFromExplore() }
            )

            ReviewMode.RESULT -> {} // handled inline
        }
    }
}

@Composable
private fun FrontCard(
    french: String,
    pos: String,
    onCheck: () -> Unit,
    onCloze: () -> Unit,
    onChoice: () -> Unit,
    onExplore: () -> Unit,
    onDontKnow: () -> Unit
) {
    Text(
        text = french,
        fontSize = 32.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurface
    )

    Spacer(Modifier.height(8.dp))

    Text(
        text = pos,
        style = MaterialTheme.typography.bodyMedium,
        fontStyle = FontStyle.Italic,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(Modifier.height(48.dp))

    // Action buttons
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onCheck,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Check, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Check")
        }

        OutlinedButton(
            onClick = onCloze,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("✏️  Cloze")
        }

        OutlinedButton(
            onClick = onChoice,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("🔠  Choice")
        }

        OutlinedButton(
            onClick = onExplore,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Explore, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Don't Know — Explore")
        }

        OutlinedButton(
            onClick = onDontKnow,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text("❓  Don't Know")
        }
    }
}

@Composable
private fun CheckCard(
    french: String,
    english: String,
    example: String,
    onKnew: () -> Unit,
    onDidntKnow: () -> Unit,
    onFailAndExplore: () -> Unit
) {
    Text(
        text = french,
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurface
    )

    Spacer(Modifier.height(16.dp))

    Text(
        text = english,
        fontSize = 22.sp,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.primary
    )

    if (example.isNotBlank()) {
        Spacer(Modifier.height(16.dp))
        Text(
            text = example,
            style = MaterialTheme.typography.bodyLarge,
            fontStyle = FontStyle.Italic,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    Spacer(Modifier.height(48.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onDidntKnow,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(Icons.Default.Close, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text("Didn't Know")
        }

        Button(
            onClick = onKnew,
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.Check, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text("I Knew It")
        }
    }

    Spacer(Modifier.height(8.dp))

    OutlinedButton(
        onClick = onFailAndExplore,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.Default.Explore, contentDescription = null)
        Spacer(Modifier.width(4.dp))
        Text("Didn't Know — Explore Further")
    }
}

@Composable
private fun ClozeCard(
    clozeSentence: String,
    answer: String,
    english: String,
    revealed: Boolean,
    onReveal: () -> Unit,
    onKnew: () -> Unit,
    onDidntKnow: () -> Unit,
    onFailAndExplore: () -> Unit
) {
    Text(
        text = answer,
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurface
    )

    Spacer(Modifier.height(8.dp))

    Text(
        text = "Fill in the blank:",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(Modifier.height(16.dp))

    Text(
        text = clozeSentence,
        fontSize = 22.sp,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurface,
        lineHeight = 32.sp
    )

    Spacer(Modifier.height(32.dp))

    if (!revealed) {
        Button(onClick = onReveal, modifier = Modifier.fillMaxWidth()) {
            Text("Reveal Answer")
        }
    } else {
        AnimatedVisibility(visible = true, enter = fadeIn()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = english,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDidntKnow,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Didn't Know")
                    }

                    Button(
                        onClick = onKnew,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("I Knew It")
                    }
                }

                Spacer(Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onFailAndExplore,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Explore, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Didn't Know — Explore Further")
                }
            }
        }
    }
}

@Composable
private fun ChoiceCard(
    french: String,
    options: List<String>,
    correctAnswer: String,
    selectedAnswer: String?,
    isCorrect: Boolean?,
    onSelect: (String) -> Unit,
    onNext: () -> Unit,
    onExplore: () -> Unit
) {
    Text(
        text = french,
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurface
    )

    Spacer(Modifier.height(8.dp))

    Text(
        text = "Choose the correct translation:",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(Modifier.height(24.dp))

    options.forEach { option ->
        val isSelected = option == selectedAnswer
        val isCorrectOption = option == correctAnswer
        val answered = selectedAnswer != null

        val containerColor = when {
            !answered -> MaterialTheme.colorScheme.surfaceVariant
            isCorrectOption -> MaterialTheme.colorScheme.primaryContainer
            isSelected && !isCorrect!! -> MaterialTheme.colorScheme.errorContainer
            else -> MaterialTheme.colorScheme.surfaceVariant
        }

        val contentColor = when {
            !answered -> MaterialTheme.colorScheme.onSurfaceVariant
            isCorrectOption -> MaterialTheme.colorScheme.onPrimaryContainer
            isSelected && !isCorrect!! -> MaterialTheme.colorScheme.onErrorContainer
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }

        Button(
            onClick = { if (!answered) onSelect(option) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = containerColor,
                contentColor = contentColor,
                disabledContainerColor = containerColor,
                disabledContentColor = contentColor
            ),
            enabled = !answered
        ) {
            Text(
                text = option,
                modifier = Modifier.padding(vertical = 8.dp),
                fontSize = 16.sp
            )
        }
    }

    if (selectedAnswer != null) {
        Spacer(Modifier.height(24.dp))

        Text(
            text = if (isCorrect == true) "Correct!" else "Incorrect — the answer is: $correctAnswer",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = if (isCorrect == true) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(16.dp))

        Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
            Text("Next")
        }

        if (isCorrect == false) {
            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                onClick = onExplore,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Explore, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Explore Further")
            }
        }
    }
}

@Composable
private fun ExploreCard(
    word: com.gash.vocab.data.db.WordEntity,
    onNext: () -> Unit
) {
    Text(
        text = word.french,
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )

    Spacer(Modifier.height(4.dp))

    Text(
        text = word.english,
        fontSize = 20.sp,
        color = MaterialTheme.colorScheme.primary
    )

    Spacer(Modifier.height(4.dp))

    Text(
        text = word.pos,
        style = MaterialTheme.typography.bodyMedium,
        fontStyle = FontStyle.Italic,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    if (word.example.isNotBlank()) {
        Spacer(Modifier.height(16.dp))
        SectionHeader("Example")
        Text(
            text = word.example,
            style = MaterialTheme.typography.bodyLarge,
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.onSurface
        )
    }

    if (word.cloze.isNotEmpty()) {
        Spacer(Modifier.height(16.dp))
        SectionHeader("Cloze Sentences")
        word.cloze.forEach { sentence ->
            Text(
                text = "• $sentence",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }
    }

    if (word.related.isNotEmpty()) {
        Spacer(Modifier.height(16.dp))
        SectionHeader("Related Words")
        word.related.forEach { rel ->
            Text(
                text = "• $rel",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }
    }

    if (word.etymology.isNotEmpty()) {
        Spacer(Modifier.height(16.dp))
        SectionHeader("Etymology")
        word.etymology.forEach { ety ->
            Text(
                text = "• $ety",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }
    }

    if (word.notes.isNotBlank()) {
        Spacer(Modifier.height(16.dp))
        SectionHeader("Notes")
        Text(
            text = word.notes,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    Spacer(Modifier.height(32.dp))

    Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
        Text("Next")
    }

    Spacer(Modifier.height(8.dp))

    val clipboardManager = LocalClipboardManager.current
    val geminiContext = LocalContext.current

    OutlinedButton(
        onClick = {
            clipboardManager.setText(
                AnnotatedString("What is the etymology of the following French term/phrase: ${word.french}")
            )
            try {
                // Try launching the Gemini app directly
                val geminiIntent = geminiContext.packageManager
                    .getLaunchIntentForPackage("com.google.android.apps.bard")
                if (geminiIntent != null) {
                    geminiContext.startActivity(geminiIntent)
                } else {
                    // Gemini app not installed — open in browser
                    val browserIntent = android.content.Intent(
                        android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse("https://gemini.google.com/app")
                    ).apply {
                        setPackage("com.android.chrome")
                    }
                    try {
                        geminiContext.startActivity(browserIntent)
                    } catch (_: Exception) {
                        // Chrome not available — try default browser
                        val fallback = android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse("https://gemini.google.com/app")
                        )
                        geminiContext.startActivity(fallback)
                    }
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(
                    geminiContext,
                    "Could not open Gemini: ${e.message}",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Go further with Gemini")
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StartPage(
    weeks: List<String>,
    posValues: List<String>,
    noCardsDue: Boolean = false,
    onStartAll: () -> Unit,
    onStartWeek: (String) -> Unit,
    onStartPos: (String) -> Unit
) {
    var selectedWeek by remember { mutableStateOf<String?>(null) }
    var selectedPos by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))

        Text(
            text = "Time to tear into GASH Fraîche!",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.height(24.dp))

        // ── Option 1: General Review ─────────────────────────────
        val cardColor = MaterialTheme.colorScheme.primaryContainer
        val cardContentColor = MaterialTheme.colorScheme.onPrimaryContainer

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = cardColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "General Review",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = cardContentColor
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "Due cards + new cards based on your daily limits",
                    style = MaterialTheme.typography.bodySmall,
                    color = cardContentColor.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )

                if (noCardsDue) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "No cards due right now — check back later or adjust your daily limits in Settings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = onStartAll,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.School, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Start")
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── Option 2: Review by Week ─────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = cardColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Review by Week",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = cardContentColor
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "Focus on vocabulary from a specific week",
                    style = MaterialTheme.typography.bodySmall,
                    color = cardContentColor.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(12.dp))

                if (weeks.isEmpty()) {
                    Text(
                        text = "No weeks data available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = cardContentColor.copy(alpha = 0.4f)
                    )
                } else {
                    var weekExpanded by remember { mutableStateOf(false) }

                    ExposedDropdownMenuBox(
                        expanded = weekExpanded,
                        onExpandedChange = { weekExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedWeek ?: "Select a week...",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = weekExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            singleLine = true
                        )

                        ExposedDropdownMenu(
                            expanded = weekExpanded,
                            onDismissRequest = { weekExpanded = false }
                        ) {
                            weeks.forEach { week ->
                                DropdownMenuItem(
                                    text = { Text(week) },
                                    onClick = {
                                        selectedWeek = week
                                        weekExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Button(
                        onClick = { selectedWeek?.let { onStartWeek(it) } },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = selectedWeek != null
                    ) {
                        Icon(Icons.Default.School, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Start")
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── Option 3: Review by Part of Speech ───────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = cardColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Review by Part of Speech",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = cardContentColor
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "Focus on a specific grammatical category",
                    style = MaterialTheme.typography.bodySmall,
                    color = cardContentColor.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(12.dp))

                if (posValues.isEmpty()) {
                    Text(
                        text = "No part of speech data available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = cardContentColor.copy(alpha = 0.4f)
                    )
                } else {
                    var posExpanded by remember { mutableStateOf(false) }

                    ExposedDropdownMenuBox(
                        expanded = posExpanded,
                        onExpandedChange = { posExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedPos ?: "Select a part of speech...",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = posExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            singleLine = true
                        )

                        ExposedDropdownMenu(
                            expanded = posExpanded,
                            onDismissRequest = { posExpanded = false }
                        ) {
                            posValues.forEach { pos ->
                                DropdownMenuItem(
                                    text = { Text(pos) },
                                    onClick = {
                                        selectedPos = pos
                                        posExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Button(
                        onClick = { selectedPos?.let { onStartPos(it) } },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = selectedPos != null
                    ) {
                        Icon(Icons.Default.School, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Start")
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionComplete(
    onRestart: () -> Unit,
    onBackToStart: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Session Complete",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "You've reviewed all cards for this session.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(32.dp))

        Button(onClick = onRestart) {
            Text("Start New Session")
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(onClick = onBackToStart) {
            Text("Back to Start")
        }
    }
}
