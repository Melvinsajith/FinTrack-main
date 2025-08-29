// File: ui/screens/AccountsScreen.kt
package com.priotxroboticsx.fintrack.ui.theme.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.priotxroboticsx.fintrack.data.Account
import com.priotxroboticsx.fintrack.ui.theme.Red
import com.priotxroboticsx.fintrack.viewmodel.AccountViewModel

@Composable
fun AccountsScreen(accountViewModel: AccountViewModel = viewModel()) {
    val accounts by accountViewModel.allAccounts.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var accountToEdit by remember { mutableStateOf<Account?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(accounts) { account ->
                AccountItem(account, onEdit = { accountToEdit = it })
            }
        }
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Account", tint = MaterialTheme.colorScheme.onPrimary)
        }

        if (showAddDialog) {
            AddEditAccountDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { name, type, balance, currency ->
                    accountViewModel.addAccount(name, type, balance, currency)
                    showAddDialog = false
                }
            )
        }

        accountToEdit?.let { account ->
            AddEditAccountDialog(
                account = account,
                onDismiss = { accountToEdit = null },
                onConfirm = { name, type, _, currency -> // Balance is not editable here
                    accountViewModel.updateAccount(account.copy(name = name, type = type, currency = currency))
                    accountToEdit = null
                },
                onDelete = {
                    accountViewModel.deleteAccount(account)
                    accountToEdit = null
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountItem(account: Account, onEdit: (Account) -> Unit) {
    val icon = when(account.type.lowercase()){
        "bank" -> Icons.Default.AccountBalance
        "wallet" -> Icons.Default.AccountBalanceWallet
        "stocks" -> Icons.Default.TrendingUp
        else -> Icons.Default.CreditCard
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { onEdit(account) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = account.type, modifier = Modifier.size(40.dp))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(account.name, style = MaterialTheme.typography.titleMedium)
                Text(account.type, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            }
            Text(String.format("%s %.2f", account.currency, account.balance), style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
fun AddEditAccountDialog(
    account: Account? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Double, String) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(account?.name ?: "") }
    var type by remember { mutableStateOf(account?.type ?: "") }
    var balance by remember { mutableStateOf(account?.balance?.toString() ?: "") }
    var currency by remember { mutableStateOf(account?.currency ?: "USD") }
    val isEditMode = account != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditMode) "Edit Account" else "Add New Account") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Account Name") })
                OutlinedTextField(value = type, onValueChange = { type = it }, label = { Text("Type (e.g., Bank)") })
                OutlinedTextField(
                    value = balance,
                    onValueChange = { if(!isEditMode) balance = it },
                    label = { Text("Initial Balance") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    enabled = !isEditMode
                )
                OutlinedTextField(value = currency, onValueChange = { currency = it }, label = { Text("Currency (e.g., USD, INR)") })
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(name, type, balance.toDoubleOrNull() ?: 0.0, currency) }) {
                Text(if (isEditMode) "Save" else "Add")
            }
        },
        dismissButton = {
            Row {
                if (isEditMode && onDelete != null) {
                    TextButton(onClick = onDelete) { Text("Delete", color = Red) }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}
