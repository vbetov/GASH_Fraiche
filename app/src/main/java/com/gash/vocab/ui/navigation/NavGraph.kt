package com.gash.vocab.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.gash.vocab.ui.browse.BrowseScreen
import com.gash.vocab.ui.browse.BrowseViewModel
import com.gash.vocab.ui.review.ReviewScreen
import com.gash.vocab.ui.settings.SettingsScreen
import com.gash.vocab.ui.settings.SettingsViewModel
import com.gash.vocab.ui.stats.StatsScreen

object Routes {
    const val STATS_SPLASH = "stats_splash"
    const val REVIEW = "review"
    const val BROWSE = "browse"
    const val SETTINGS = "settings"
    const val STATS = "stats"
}

@Composable
fun GashNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    reviewViewModel: com.gash.vocab.ui.review.ReviewViewModel? = null,
    browseViewModel: BrowseViewModel? = null,
    settingsViewModel: SettingsViewModel? = null,
    onCloseApp: () -> Unit = {}
) {
    NavHost(navController = navController, startDestination = Routes.STATS_SPLASH, modifier = modifier) {
        composable(Routes.STATS_SPLASH) {
            StatsScreen(onDismiss = {
                navController.navigate(Routes.REVIEW) {
                    popUpTo(Routes.STATS_SPLASH) { inclusive = true }
                }
            })
        }
        composable(Routes.REVIEW) {
            if (reviewViewModel != null) {
                ReviewScreen(vm = reviewViewModel)
            } else {
                ReviewScreen()
            }
        }
        composable(Routes.BROWSE) {
            if (browseViewModel != null) {
                BrowseScreen(vm = browseViewModel)
            } else {
                BrowseScreen()
            }
        }
        composable(Routes.SETTINGS) {
            if (settingsViewModel != null) {
                SettingsScreen(
                    onShowStats = { navController.navigate(Routes.STATS) },
                    onCloseApp = onCloseApp,
                    vm = settingsViewModel
                )
            } else {
                SettingsScreen(
                    onShowStats = { navController.navigate(Routes.STATS) },
                    onCloseApp = onCloseApp
                )
            }
        }
        composable(Routes.STATS) {
            StatsScreen(
                onDismiss = null,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
