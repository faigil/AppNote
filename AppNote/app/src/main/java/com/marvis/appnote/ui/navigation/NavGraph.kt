package com.marvis.appnote.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.marvis.appnote.ui.screen.AppInfoScreen
import com.marvis.appnote.ui.screen.NoteEditScreen
import com.marvis.appnote.ui.screen.NoteListScreen

sealed class Screen(val route: String, val label: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector) {
    data object Notes : Screen("notes", "笔记", Icons.Filled.EditNote, Icons.Outlined.EditNote)
    data object Apps : Screen("apps", "应用", Icons.Filled.Apps, Icons.Outlined.Apps)
    data object NoteEdit : Screen("note_edit/{noteId}", "编辑", Icons.Filled.EditNote, Icons.Outlined.EditNote)
}

val bottomNavItems = listOf(Screen.Notes, Screen.Apps)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if (currentRoute in bottomNavItems.map { it.route }) {
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    if (currentRoute == screen.route) screen.selectedIcon else screen.unselectedIcon,
                                    contentDescription = screen.label
                                )
                            },
                            label = { Text(screen.label) },
                            selected = currentRoute == screen.route,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(Screen.Notes.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Notes.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Notes.route) {
                NoteListScreen(
                    onNoteClick = { noteId ->
                        navController.navigate("note_edit/$noteId")
                    },
                    onNewNote = {
                        navController.navigate("note_edit/-1")
                    }
                )
            }
            composable("note_edit/{noteId}") { backStackEntry ->
                val noteId = backStackEntry.arguments?.getString("noteId")?.toLongOrNull() ?: -1L
                NoteEditScreen(
                    noteId = noteId,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Apps.route) {
                AppInfoScreen()
            }
        }
    }
}