package com.slmlab.app.ui.nav

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.slmlab.app.ui.components.popBox
import com.slmlab.app.ui.glossary.GlossaryScreen
import com.slmlab.app.ui.lab.LabScreen
import com.slmlab.app.ui.lesson.LessonScreen
import com.slmlab.app.ui.playground.PlaygroundScreen
import com.slmlab.app.ui.roadmap.RoadmapScreen
import com.slmlab.app.ui.settings.SettingsScreen
import com.slmlab.app.ui.theme.LocalPop

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
                PopNavBar(
                    currentRoute = currentRoute,
                    onSelect = { tab ->
                        navController.navigate(tab.route) {
                            popUpTo(ROUTE_ROADMAP) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
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

/** Floating outlined tab bar with a hard shadow; the selected tab is a yellow block. */
@Composable
private fun PopNavBar(currentRoute: String?, onSelect: (TabItem) -> Unit) {
    val pop = LocalPop.current
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(scheme.background)
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 11.dp, top = 6.dp, bottom = 7.dp)
            .popBox(fill = scheme.surface, ink = pop.ink, radius = 20.dp, border = 2.5.dp, shadow = 5.dp)
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        TabItem.entries.forEach { tab ->
            val selected = currentRoute == tab.route
            val tint = if (selected) pop.onBlock else scheme.onSurfaceVariant
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .popBox(
                        fill = if (selected) pop.yellow else scheme.surface,
                        ink = if (selected) pop.ink else scheme.surface,
                        radius = 13.dp,
                    )
                    .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(tab) }),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = tab.icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    text = tab.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = tint,
                    maxLines = 1,
                )
            }
        }
    }
}
