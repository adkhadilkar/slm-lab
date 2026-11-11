package com.slmlab.app.ui.nav

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.slmlab.app.ui.glossary.GlossaryScreen
import com.slmlab.app.ui.lab.LabScreen
import com.slmlab.app.ui.lesson.LessonScreen
import com.slmlab.app.ui.playground.PlaygroundScreen
import com.slmlab.app.ui.roadmap.RoadmapScreen
import com.slmlab.app.ui.settings.SettingsScreen

private const val ROUTE_ROADMAP = "roadmap"
private const val ROUTE_PLAYGROUND = "playground"
private const val ROUTE_GLOSSARY = "glossary"
private const val ROUTE_SETTINGS = "settings"
private const val ROUTE_LESSON = "lesson/{id}"
private const val ROUTE_LAB = "lab/{id}"

private fun lessonRoute(id: String): String = "lesson/$id"

private fun labRoute(id: String): String = "lab/$id"

/** The four bottom-bar tabs, in order (DESIGN.md 5.1). */
private enum class TabItem(val route: String, val label: String, val icon: ImageVector) {
    PATH(ROUTE_ROADMAP, "Path", Icons.Outlined.Route),
    PLAYGROUND(ROUTE_PLAYGROUND, "Playground", Icons.Outlined.AutoAwesome),
    GLOSSARY(ROUTE_GLOSSARY, "Glossary", Icons.AutoMirrored.Outlined.MenuBook),
    SETTINGS(ROUTE_SETTINGS, "Settings", Icons.Outlined.Settings),
}

@Composable
fun AppNav() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val onTab = TabItem.entries.any { it.route == currentRoute }

    Scaffold(
        // Screens provide their own top bars and handle the status bar. This Scaffold only adds the bottom bar.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (onTab) {
                NavigationBar {
                    TabItem.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(ROUTE_ROADMAP) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(imageVector = tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = ROUTE_ROADMAP,
            modifier = if (onTab) Modifier.padding(innerPadding) else Modifier,
        ) {
            composable(ROUTE_ROADMAP) {
                RoadmapScreen(
                    onOpenLesson = { id -> navController.navigate(lessonRoute(id)) },
                )
            }

            composable(ROUTE_PLAYGROUND) {
                PlaygroundScreen(
                    onOpenLesson = { id -> navController.navigate(lessonRoute(id)) },
                )
            }

            composable(ROUTE_GLOSSARY) {
                GlossaryScreen()
            }

            composable(ROUTE_SETTINGS) {
                SettingsScreen()
            }

            composable(
                route = ROUTE_LESSON,
                arguments = listOf(navArgument("id") { type = NavType.StringType }),
            ) { entry ->
                LessonScreen(
                    lessonId = entry.arguments?.getString("id").orEmpty(),
                    onBack = { navController.popBackStack() },
                    onOpenLab = { id -> navController.navigate(labRoute(id)) },
                    // "Next" replaces the current lesson so the back stack does not grow.
                    onOpenLesson = { id ->
                        navController.navigate(lessonRoute(id)) {
                            popUpTo(ROUTE_LESSON) { inclusive = true }
                        }
                    },
                )
            }

            composable(
                route = ROUTE_LAB,
                arguments = listOf(navArgument("id") { type = NavType.StringType }),
            ) { entry ->
                LabScreen(
                    lessonId = entry.arguments?.getString("id").orEmpty(),
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
