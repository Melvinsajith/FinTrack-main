package com.priotxroboticsx.fintrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.compose.*
import com.priotxroboticsx.fintrack.ui.Routes
import com.priotxroboticsx.fintrack.ui.theme.screens.*
import com.priotxroboticsx.fintrack.ui.theme.FinTrackTheme
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
        NavItem("Dashboard", Icons.Default.Dashboard, Routes.DASHBOARD),
        NavItem("Accounts", Icons.Default.AccountBalanceWallet, Routes.ACCOUNTS),
        NavItem("Add", Icons.Default.AddCircle, Routes.ADD_TRANSACTION),
        NavItem("Reports", Icons.Default.Assessment, Routes.REPORTS),
        NavItem("Settings", Icons.Default.Settings, Routes.SETTINGS)
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = { AppBottomNavBar(navController, navItems) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.DASHBOARD,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.DASHBOARD) { DashboardScreen() }
            composable(Routes.ACCOUNTS) { AccountsScreen() }
            composable(Routes.ADD_TRANSACTION) {
                AddTransactionScreen(onTransactionAdded = {
                    navController.navigate(Routes.DASHBOARD) { popUpTo(Routes.DASHBOARD) { inclusive = true } }
                })
            }
            composable(Routes.REPORTS) { ReportsScreen() }
            composable(Routes.SETTINGS) {
                SettingsScreen(showSnackbar = { message ->
                    scope.launch {
                        snackbarHostState.showSnackbar(message = message)
                    }
                })
            }
        }
    }
}

@Composable
fun AppBottomNavBar(navController: NavController, items: List<NavItem>) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                selected = currentRoute == item.route,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    }
}