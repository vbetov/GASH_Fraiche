package com.gash.vocab

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.gash.vocab.data.db.AppDatabase
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import android.app.Activity
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gash.vocab.ui.navigation.GashNavGraph
import com.gash.vocab.ui.navigation.Routes
import com.gash.vocab.ui.browse.BrowseViewModel
import com.gash.vocab.ui.review.ReviewMode
import com.gash.vocab.ui.review.ReviewViewModel
import com.gash.vocab.ui.settings.SettingsViewModel
import com.gash.vocab.ui.theme.GashTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GashTheme {
                GashScaffold()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        try {
            AppDatabase.backupToExternalStorage(this)
        } catch (e: Exception) {
            Log.w("GASH", "Auto-backup failed: ${e.message}")
        }
    }
}

private data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: @Composable () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GashScaffold() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val reviewViewModel: ReviewViewModel = viewModel()
    val browseViewModel: BrowseViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()
    val reviewState by reviewViewModel.state.collectAsState()
    val context = LocalContext.current

    // "Are you sure?" dialog state
    var pendingRoute by remember { mutableStateOf<String?>(null) }

    val isReviewActive = reviewState.mode != ReviewMode.START_PAGE && !reviewState.sessionComplete

    fun navigateTo(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    // Confirmation dialog
    if (pendingRoute != null) {
        AlertDialog(
            onDismissRequest = { pendingRoute = null },
            title = { Text("Leave review?") },
            text = { Text("Are you sure? Your responses so far have been saved, but your remaining cards in this session will be reset.") },
            confirmButton = {
                TextButton(onClick = {
                    val route = pendingRoute!!
                    pendingRoute = null
                    reviewViewModel.backToStart()
                    navigateTo(route)
                }) {
                    Text("Leave")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRoute = null }) {
                    Text("Stay")
                }
            }
        )
    }

    val items = listOf(
        BottomNavItem(Routes.REVIEW, "Review") {
            Icon(Icons.Default.School, contentDescription = "Review")
        },
        BottomNavItem(Routes.BROWSE, "Browse") {
            Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = "Browse")
        },
        BottomNavItem(Routes.SETTINGS, "Settings") {
            Icon(Icons.Default.Settings, contentDescription = "Settings")
        }
    )

    val currentRoute = currentDestination?.route
    val showBars = currentRoute != Routes.STATS_SPLASH && currentRoute != Routes.STATS

    Scaffold(
        topBar = {
            if (showBars) {
                TopAppBar(
                    title = {
                        Text(
                            "Guide d'Assimilation et de Syntaxe Heuristique",
                            maxLines = 1,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        },
        bottomBar = {
            if (showBars) {
                NavigationBar {
                    items.forEach { item ->
                        NavigationBarItem(
                            icon = item.icon,
                            label = { Text(item.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                            onClick = {
                                // If navigating away from an active review, confirm first
                                if (currentRoute == Routes.REVIEW && item.route != Routes.REVIEW && isReviewActive) {
                                    pendingRoute = item.route
                                } else {
                                    navigateTo(item.route)
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        GashNavGraph(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
            reviewViewModel = reviewViewModel,
            browseViewModel = browseViewModel,
            settingsViewModel = settingsViewModel,
            onCloseApp = {
                try {
                    AppDatabase.backupToExternalStorage(context)
                } catch (e: Exception) {
                    Log.w("GASH", "Backup on close failed: ${e.message}")
                }
                (context as? Activity)?.finishAffinity()
            }
        )
    }
}
