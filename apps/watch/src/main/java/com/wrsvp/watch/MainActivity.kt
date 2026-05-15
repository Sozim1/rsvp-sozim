package com.wrsvp.watch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.wrsvp.watch.chapters.ChaptersScreen
import com.wrsvp.watch.details.BookDetailsScreen
import com.wrsvp.watch.library.LibraryScreen
import com.wrsvp.watch.receive.ReceiveFromComputerScreen
import com.wrsvp.watch.reader.ReaderScreen
import com.wrsvp.watch.settings.SettingsScreen
import com.wrsvp.watch.settings.SpeedScreen
import com.wrsvp.watch.ui.LocalWatchPalette
import com.wrsvp.watch.ui.WatchAppearanceViewModel
import com.wrsvp.watch.ui.watchPalette
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WristRsvpWatchRoot()
        }
    }
}

@Composable
private fun WristRsvpWatchRoot() {
    val appearanceViewModel: WatchAppearanceViewModel = hiltViewModel()
    val theme by appearanceViewModel.theme.collectAsState()
    MaterialTheme {
        CompositionLocalProvider(LocalWatchPalette provides watchPalette(theme)) {
            val navController = rememberSwipeDismissableNavController()
            SwipeDismissableNavHost(
                navController = navController,
                startDestination = "library",
            ) {
            composable("library") {
                LibraryScreen(
                    onOpenBook = { bookId -> navController.navigate("details/$bookId") },
                    onOpenSettings = { navController.navigate("settings") },
                    onReceiveFromComputer = { navController.navigate("receive-computer") },
                )
            }
            composable("reader/{bookId}") {
                ReaderScreen(
                    onBackToLibrary = { navController.popBackStack("library", inclusive = false) },
                    onOpenChapters = { bookId -> navController.navigate("chapters/$bookId") },
                    onOpenDetails = { bookId -> navController.navigate("details/$bookId") },
                    onReceiveFromComputer = { navController.navigate("receive-computer") },
                    onOpenSpeed = { bookId -> navController.navigate("speed/$bookId") },
                )
            }
            composable("receive-computer") {
                ReceiveFromComputerScreen(
                    onOpenBook = { bookId -> navController.navigate("reader/$bookId") },
                    onBackToLibrary = { navController.popBackStack("library", inclusive = false) },
                )
            }
            composable("chapters/{bookId}") {
                ChaptersScreen(
                    onOpenReader = { bookId ->
                        navController.navigate("reader/$bookId") {
                            popUpTo("reader/$bookId") { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() },
                )
            }
            composable("details/{bookId}") {
                BookDetailsScreen(
                    onContinue = { bookId -> navController.navigate("reader/$bookId") },
                    onChapters = { bookId -> navController.navigate("chapters/$bookId") },
                    onDeleted = { navController.popBackStack("library", inclusive = false) },
                    onBack = { navController.popBackStack() },
                )
            }
            composable("settings") {
                SettingsScreen(
                    onOpenSpeed = { navController.navigate("speed") },
                )
            }
            composable("speed") {
                SpeedScreen(onBack = { navController.popBackStack() })
            }
            composable("speed/{bookId}") {
                SpeedScreen(onBack = { navController.popBackStack() })
            }
            }
        }
    }
}
