package com.priotxroboticsx.fintrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument // Correct import added
import com.priotxroboticsx.fintrack.ui.Routes
import com.priotxroboticsx.fintrack.ui.theme.FinTrackTheme
import com.priotxroboticsx.fintrack.ui.theme.screens.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FinTrackTheme {
                AppShell()
            }
        }
    }
}

data class NavItem(val label: String, val icon: ImageVector, val route: String)

@Composable
fun AppShell() {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val navItems = listOf(
        NavItem("Home", Icons.Default.Home, Routes.DASHBOARD),
        NavItem("Accounts", Icons.Default.AccountBalanceWallet, Routes.ACCOUNTS),
        NavItem("Reports", Icons.Default.Assessment, Routes.REPORTS),
        NavItem("Settings", Icons.Default.Settings, Routes.SETTINGS)
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Box(modifier = Modifier.fillMaxWidth()) {
                AppBottomBar(
                    navController = navController,
                    items = navItems,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
                FloatingActionButton(
                    onClick = { navController.navigate(Routes.ADD_TRANSACTION) },
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Transaction", tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.DASHBOARD,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.DASHBOARD) { DashboardScreen(navController = navController) }
            composable(Routes.ACCOUNTS) { AccountsScreen() } // Assuming AccountsScreen exists
            composable(Routes.ADD_TRANSACTION) {
                AddTransactionScreen(onTransactionAdded = { // Assuming AddTransactionScreen exists
                    navController.popBackStack()
                })
            }
            composable(
                route = "${Routes.REPORTS}?${Routes.REPORTS_ARG_TYPE}={${Routes.REPORTS_ARG_TYPE}}",
                arguments = listOf(navArgument(Routes.REPORTS_ARG_TYPE) {
                    type = NavType.StringType
                    defaultValue = "Expenses"
                })
            ) { backStackEntry ->
                ReportsScreen(
                    defaultTab = backStackEntry.arguments?.getString(Routes.REPORTS_ARG_TYPE) ?: "Expenses"
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(showSnackbar = { message -> // Assuming SettingsScreen exists
                    scope.launch {
                        snackbarHostState.showSnackbar(message = message)
                    }
                })
            }
        }
    }
}

@Composable
fun AppBottomBar(navController: NavController, items: List<NavItem>, modifier: Modifier = Modifier) {
    NavigationBar(
        modifier = modifier.height(80.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
        val middleIndex = items.size / 2

        items.forEachIndexed { index, item ->
            if (index == middleIndex) {
                // This is where the FAB sits, so we leave a gap.
                NavigationBarItem(
                    selected = false,
                    onClick = { /* No-op */ },
                    icon = {},
                    enabled = false
                )
            }

            val selected = currentRoute == item.route
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                selected = selected,
                onClick = {
                    if (!selected) {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    }
}
