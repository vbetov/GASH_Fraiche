package com.gash.vocab.ui.stats

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Statistics screen. When [onDismiss] is provided, tapping anywhere dismisses it
 * (used for the splash on launch). When null, it behaves as a normal screen
 * (used from Settings).
 */
@Composable
fun StatsScreen(
    onDismiss: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    vm: StatsViewModel = viewModel()
) {
    val state by vm.state.collectAsState()

    val modifier = if (onDismiss != null) {
        Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() }
    } else {
        Modifier.fillMaxSize()
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.Start)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        Text(
            text = "🥖",
            fontSize = 48.sp
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "GASH Fraîche",
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(24.dp))

        // Overview card
        StatsCard("Overview") {
            StatRow("Total words", "${state.totalWords}")
            StatRow("Words seen", "${state.seenWords}")
            StatRow("Mature (21+ days)", "${state.matureWords}")
            StatRow("Due today", "${state.dueToday}")
            StatRow("Studied today", "${state.studiedToday}")
        }

        Spacer(Modifier.height(16.dp))

        // Performance card
        StatsCard("Performance") {
            StatRow("Total reviews", "${state.totalReviews}")
            StatRow("Accuracy", state.accuracy)
            StatRow("Avg ease factor", state.avgEaseFactor)
        }

        Spacer(Modifier.height(16.dp))

        // Mode breakdown card
        StatsCard("Reviews by Mode") {
            StatRow("Check ✓", "${state.checkCount}")
            StatRow("Cloze ✍", "${state.clozeCount}")
            StatRow("Choice 🔠", "${state.choiceCount}")
            StatRow("Don't know ✗", "${state.dontKnowCount}")
        }

        if (onDismiss != null) {
            Spacer(Modifier.height(24.dp))
            Text(
                text = "Tap anywhere to start",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun StatsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
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
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
