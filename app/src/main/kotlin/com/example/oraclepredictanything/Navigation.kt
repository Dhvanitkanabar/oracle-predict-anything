package com.example.oraclepredictanything

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.oraclepredictanything.data.model.PredictionCategory
import com.example.oraclepredictanything.ui.compare.CompareScreen
import com.example.oraclepredictanything.ui.dataentry.DataEntryScreen
import com.example.oraclepredictanything.ui.datasets.DatasetsScreen
import com.example.oraclepredictanything.ui.history.HistoryScreen
import com.example.oraclepredictanything.ui.home.HomeScreen
import com.example.oraclepredictanything.ui.onboarding.OnboardingScreen
import com.example.oraclepredictanything.ui.prediction.PredictionScreen
import com.example.oraclepredictanything.ui.settings.SettingsScreen

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    data object Onboarding : Screen("onboarding", "Onboarding")
    data object Home : Screen("home", "Home", Icons.Default.Home)
    data object Datasets : Screen("datasets", "Datasets", Icons.AutoMirrored.Filled.List)
    data object History : Screen("history", "History", Icons.Default.Refresh)
    data object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    data object DataEntry : Screen("data_entry/{datasetId}", "Data Entry") {
        fun createRoute(datasetId: Long) = "data_entry/$datasetId"
    }
    data object Prediction : Screen("prediction/{datasetId}", "Prediction") {
        fun createRoute(datasetId: Long) = "prediction/$datasetId"
    }
    data object Compare : Screen("compare", "Compare")
}

@Composable
fun MainNavigation(
    isOnboardingCompleted: Boolean,
    onCompleteOnboarding: () -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavItems = listOf(
        Screen.Home,
        Screen.Datasets,
        Screen.History,
        Screen.Settings
    )

    val showBottomBar = currentRoute in listOf(
        Screen.Home.route,
        Screen.Datasets.route,
        Screen.History.route,
        Screen.Settings.route
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                ) {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon!!, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = currentRoute == screen.route,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
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
            startDestination = if (isOnboardingCompleted) Screen.Home.route else Screen.Onboarding.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onFinished = {
                        onCompleteOnboarding()
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Home.route) {
                HomeScreen(
                    onCategorySelected = { cat ->
                        navController.navigate(Screen.Datasets.route)
                    },
                    onPredictionClicked = { datasetId ->
                        navController.navigate(Screen.Prediction.createRoute(datasetId))
                    },
                    onQuickStartClicked = {
                        navController.navigate(Screen.Datasets.route)
                    }
                )
            }

            composable(Screen.Datasets.route) {
                DatasetsScreen(
                    onDatasetSelected = { id ->
                        navController.navigate(Screen.DataEntry.createRoute(id))
                    }
                )
            }

            composable(
                route = Screen.DataEntry.route,
                arguments = listOf(navArgument("datasetId") { type = NavType.LongType })
            ) { backStackEntry ->
                val datasetId = backStackEntry.arguments?.getLong("datasetId") ?: 0L
                DataEntryScreen(
                    datasetId = datasetId,
                    onBack = { navController.popBackStack() },
                    onRunPrediction = { id ->
                        navController.navigate(Screen.Prediction.createRoute(id))
                    }
                )
            }

            composable(
                route = Screen.Prediction.route,
                arguments = listOf(navArgument("datasetId") { type = NavType.LongType })
            ) { backStackEntry ->
                val datasetId = backStackEntry.arguments?.getLong("datasetId") ?: 0L
                PredictionScreen(
                    datasetId = datasetId,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.History.route) {
                HistoryScreen(
                    onPredictionSelected = { datasetId ->
                        navController.navigate(Screen.Prediction.createRoute(datasetId))
                    }
                )
            }

            composable(Screen.Compare.route) {
                CompareScreen(
                    onBack = { navController.popBackStack() },
                    onCompareDatasets = { d1, d2 ->
                        navController.navigate(Screen.Prediction.createRoute(d1))
                    }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen()
            }
        }
    }
}
