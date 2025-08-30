package com.priotxroboticsx.fintrack.ui.theme.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.priotxroboticsx.fintrack.data.Account
import com.priotxroboticsx.fintrack.data.Transaction
import com.priotxroboticsx.fintrack.ui.theme.Teal
import com.priotxroboticsx.fintrack.viewmodel.AccountViewModel
import com.priotxroboticsx.fintrack.viewmodel.SettingsViewModel
import com.priotxroboticsx.fintrack.viewmodel.TransactionViewModel
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel = viewModel(),
    transactionViewModel: TransactionViewModel = viewModel(),
    accountViewModel: AccountViewModel = viewModel(),
    showSnackbar: (String) -> Unit
) {
    val user by settingsViewModel.user.collectAsState()
    val transactions by transactionViewModel.allTransactions.collectAsState()
    val accounts by accountViewModel.allAccounts.collectAsState()
    var userName by remember(user) { mutableStateOf(user?.name ?: "") }

    val context = LocalContext.current
    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            exportToCsv(context, it, transactions, accounts, showSnackbar)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Settings",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        item {
            SettingsSection("ACCOUNT") {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Person, contentDescription = "User", modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(16.dp))
                        OutlinedTextField(
                            value = userName,
                            onValueChange = { userName = it },
                            label = { Text("Melvin") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            )
                        )
                        Button(
                            onClick = {
                                settingsViewModel.updateUserName(userName)
                                showSnackbar("Name Updated!")
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }

        item {
            SettingsSection("IMPORT & EXPORT") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SettingsRow(
                        icon = Icons.Default.Download,
                        title = "Export to CSV",
                        subtitle = "Not intended for backup use",
                        onClick = { csvLauncher.launch("fintrack_export.csv") }
                    )
                    SettingsRow(icon = Icons.Default.Backup, title = "Backup data", onClick = { /* TODO */ })
                    SettingsRow(
                        icon = Icons.Default.Input,
                        title = "Import data",
                        onClick = { /* TODO */ },
                        isHighlighted = true
                    )
                }
            }
        }

        item {
            SettingsSection("APP SETTINGS") {
                SettingsRow(
                    icon = Icons.Default.DarkMode,
                    title = "Dark mode",
                    subtitle = "Tap to toggle theme",
                    onClick = { /* TODO */ }
                )
            }
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )
        content()
    }
}

@Composable
fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    isHighlighted: Boolean = false
) {
    val containerColor = if (isHighlighted) Teal else MaterialTheme.colorScheme.surface
    val contentColor = if (isHighlighted) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier.clickable(onClick = onClick).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = title, tint = contentColor)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, color = contentColor)
                subtitle?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = contentColor.copy(alpha = 0.7f))
                }
            }
        }
    }
}


private fun exportToCsv(
    context: Context,
    uri: Uri,
    transactions: List<Transaction>,
    accounts: List<Account>,
    showSnackbar: (String) -> Unit
) {
    try {
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            outputStream.bufferedWriter().use { writer ->
                // Header
                writer.appendLine("Date,Type,Category,Amount,Currency,Account,Notes")

                // Data
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                transactions.forEach { transaction ->
                    val account = accounts.find { it.id == transaction.accountId }
                    writer.appendLine(
                        listOf(
                            dateFormat.format(transaction.date),
                            transaction.type,
                            transaction.category,
                            transaction.amount.toString(),
                            account?.currency ?: "N/A",
                            account?.name ?: "Unknown",
                            transaction.notes?.replace(",", "") ?: ""
                        ).joinToString(",")
                    )
                }
            }
        }
        showSnackbar("Exported successfully!")
    } catch (e: IOException) {
        e.printStackTrace()
        showSnackbar("Export failed.")
    }
}
