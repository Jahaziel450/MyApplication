package com.example.myapplication.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

@Composable
fun NoteFlowApp(
    viewModel: NoteViewModel,
    windowWidthSizeClass: WindowWidthSizeClass
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    var isSearchActive by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            if (currentRoute == "list") {
                NavigationBar {
                    NavigationBarItem(
                        selected = !isSearchActive,
                        onClick = { isSearchActive = false },
                        icon = { Icon(Icons.Default.Home, contentDescription = null) },
                        label = { Text("Inicio") }
                    )
                    NavigationBarItem(
                        selected = isSearchActive,
                        onClick = { isSearchActive = true },
                        icon = { Icon(Icons.Default.Search, contentDescription = null) },
                        label = { Text("Buscar") }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "list",
            modifier = Modifier.padding(padding)
        ) {
            composable("list") {
                NoteListScreen(
                    viewModel = viewModel,
                    windowWidthSizeClass = windowWidthSizeClass,
                    isSearchActive = isSearchActive,
                    onNoteClick = { id -> navController.navigate("detail/$id") },
                    onAddNoteClick = { navController.navigate("add_edit/-1") }
                )
            }
            composable(
                route = "detail/{noteId}",
                arguments = listOf(navArgument("noteId") { type = NavType.LongType })
            ) { backStackEntry ->
                val noteId = backStackEntry.arguments?.getLong("noteId") ?: -1L
                NoteDetailScreen(
                    viewModel = viewModel,
                    noteId = noteId,
                    onBack = { navController.popBackStack() },
                    onEdit = { id -> navController.navigate("add_edit/$id") }
                )
            }
            composable(
                route = "add_edit/{noteId}",
                arguments = listOf(navArgument("noteId") { type = NavType.LongType })
            ) { backStackEntry ->
                val noteId = backStackEntry.arguments?.getLong("noteId")
                AddEditNoteScreen(
                    viewModel = viewModel,
                    noteId = if (noteId == -1L) null else noteId,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
