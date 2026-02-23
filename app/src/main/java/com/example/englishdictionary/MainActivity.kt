package com.example.englishdictionary

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.room.Room
import com.example.englishdictionary.data.DictionaryRepository
import com.example.englishdictionary.data.local.AppDatabase
import com.example.englishdictionary.ui.AppRoutes
import com.example.englishdictionary.ui.DictionaryViewModel
import com.example.englishdictionary.ui.screens.DeckListScreen
import com.example.englishdictionary.ui.screens.EntryDetailScreen
import com.example.englishdictionary.ui.screens.MatchingModeScreen
import com.example.englishdictionary.ui.screens.MemoryModeScreen
import com.example.englishdictionary.ui.screens.QuizScreen
import com.example.englishdictionary.ui.screens.SearchScreen
import com.example.englishdictionary.ui.screens.StartupSplashScreen
import com.example.englishdictionary.ui.theme.EnglishDictionaryAppTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private val database by lazy {
        Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "dictionary.db"
        ).build()
    }

    private val repository by lazy {
        DictionaryRepository(applicationContext, database)
    }

    private val viewModel by viewModels<DictionaryViewModel> {
        DictionaryViewModel.Factory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            EnglishDictionaryAppTheme {
                DictionaryApp(viewModel)
            }
        }
    }
}

@Composable
private fun DictionaryApp(viewModel: DictionaryViewModel) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val decks by viewModel.decks.collectAsStateWithLifecycle()
    val deckWallpaperUris by viewModel.deckWallpaperUris.collectAsStateWithLifecycle()
    val deckEntryCounts by viewModel.deckEntryCounts.collectAsStateWithLifecycle()
    var showSplash by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }
    LaunchedEffect(Unit) {
        delay(1200)
        showSplash = false
    }

    if (showSplash) {
        StartupSplashScreen()
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = AppRoutes.DeckList,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            composable(AppRoutes.DeckList) {
                DeckListScreen(
                    decks = decks,
                    deckWallpaperUris = deckWallpaperUris,
                    deckEntryCounts = deckEntryCounts,
                    onSetDeckWallpaper = { deckId, uriString ->
                        viewModel.setDeckWallpaper(deckId, uriString)
                    },
                    onOpenDeck = { deckId -> navController.navigate("${AppRoutes.Search}/$deckId") }
                )
            }

            composable(
                route = "${AppRoutes.Search}/{deckId}",
                arguments = listOf(navArgument("deckId") { type = NavType.StringType })
            ) { backStackEntry ->
                val deckId = backStackEntry.arguments?.getString("deckId").orEmpty()
                SearchScreen(
                    deckId = deckId,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onOpenEntry = { entryId -> navController.navigate("${AppRoutes.EntryDetail}/$deckId/$entryId") },
                    onOpenQuiz = { navController.navigate("${AppRoutes.Quiz}/$deckId") },
                    onOpenMemory = { navController.navigate("${AppRoutes.Memory}/$deckId") },
                    onOpenMatching = { navController.navigate("${AppRoutes.Matching}/$deckId") }
                )
            }

            composable(
                route = "${AppRoutes.EntryDetail}/{deckId}/{entryId}",
                arguments = listOf(
                    navArgument("deckId") { type = NavType.StringType },
                    navArgument("entryId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val deckId = backStackEntry.arguments?.getString("deckId").orEmpty()
                val entryId = backStackEntry.arguments?.getString("entryId").orEmpty()
                EntryDetailScreen(
                    deckId = deckId,
                    entryId = entryId,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = "${AppRoutes.Quiz}/{deckId}",
                arguments = listOf(navArgument("deckId") { type = NavType.StringType })
            ) { backStackEntry ->
                val deckId = backStackEntry.arguments?.getString("deckId").orEmpty()
                QuizScreen(
                    deckId = deckId,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onOpenEntry = { entryId -> navController.navigate("${AppRoutes.EntryDetail}/$deckId/$entryId") }
                )
            }

            composable(
                route = "${AppRoutes.Memory}/{deckId}",
                arguments = listOf(navArgument("deckId") { type = NavType.StringType })
            ) { backStackEntry ->
                val deckId = backStackEntry.arguments?.getString("deckId").orEmpty()
                MemoryModeScreen(
                    deckId = deckId,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onOpenEntry = { entryId -> navController.navigate("${AppRoutes.EntryDetail}/$deckId/$entryId") }
                )
            }

            composable(
                route = "${AppRoutes.Matching}/{deckId}",
                arguments = listOf(navArgument("deckId") { type = NavType.StringType })
            ) { backStackEntry ->
                val deckId = backStackEntry.arguments?.getString("deckId").orEmpty()
                MatchingModeScreen(
                    deckId = deckId,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
