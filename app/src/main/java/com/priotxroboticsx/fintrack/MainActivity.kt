package com.priotxroboticsx.fintrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
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
        NavItem("Home", Icons.Default.Home, Routes.DASHBOARD),
        NavItem("Accounts", Icons.Default.AccountBalanceWallet, Routes.ACCOUNTS),
        NavItem("Reports", Icons.Default.Assessment, Routes.REPORTS),
        NavItem("Settings", Icons.Default.Settings, Routes.SETTINGS)
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Routes.ADD_TRANSACTION) },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Transaction", tint = MaterialTheme.colorScheme.onPrimary)
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
        bottomBar = { AppBottomBar(navController, navItems) }
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
                    navController.popBackStack()
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
fun AppBottomBar(navController: NavController, items: List<NavItem>) {
    BottomAppBar(
        containerColor = MaterialTheme.colorScheme.surface,
        actions = {
            val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
            val middleIndex = items.size / 2

            items.forEachIndexed { index, item ->
                if (index == middleIndex) {
                    // This is where the FAB sits, so we leave a gap.
                    // The weight here should be adjusted based on the number of items
                    // to ensure the FAB is centered nicely.
                    Spacer(Modifier.weight(1f))
                }

                val selected = currentRoute == item.route
                val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

                IconButton(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        if (!selected) {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                ) {
                    Icon(item.icon, contentDescription = item.label, tint = color)
                }
            }
        }
    )
}
