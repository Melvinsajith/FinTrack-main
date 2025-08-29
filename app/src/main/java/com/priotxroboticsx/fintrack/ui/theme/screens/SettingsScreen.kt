package com.priotxroboticsx.fintrack.ui.theme.screens


import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.priotxroboticsx.fintrack.data.Account
import com.priotxroboticsx.fintrack.data.Transaction
import com.priotxroboticsx.fintrack.viewmodel.AccountViewModel
import com.priotxroboticsx.fintrack.viewmodel.SettingsViewModel
import com.priotxroboticsx.fintrack.viewmodel.TransactionViewModel
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale

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
    var userName by remember { mutableStateOf(user?.name ?: "") }

    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            exportToCsv(context, it, transactions, accounts, showSnackbar)
        }
    }

    LaunchedEffect(user) {
        userName = user?.name ?: ""
    }

    Column(modifier = Modifier.padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall)

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(16.dp)) {
                Text("Profile", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = userName,
                    onValueChange = { userName = it },
                    label = { Text("Your Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = { settingsViewModel.updateUserName(userName); showSnackbar("Name Updated!") }) {
                    Text("Save Name")
                }
            }
        }


        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(16.dp)) {
                Text("Data Management", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Button(onClick = { launcher.launch("fintrack_export.csv") }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Download, contentDescription = "Export")
                    Spacer(Modifier.width(8.dp))
                    Text("Export Transactions to CSV")
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