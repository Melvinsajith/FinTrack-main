// File: ui/screens/AccountsScreen.kt
package com.priotxroboticsx.fintrack.ui.theme.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.priotxroboticsx.fintrack.data.Account
import com.priotxroboticsx.fintrack.data.Transaction
import com.priotxroboticsx.fintrack.ui.theme.Red
import com.priotxroboticsx.fintrack.viewmodel.AccountViewModel
import com.priotxroboticsx.fintrack.viewmodel.TransactionViewModel
import java.util.*

@Composable
fun AccountsScreen(
    accountViewModel: AccountViewModel = viewModel(),
    transactionViewModel: TransactionViewModel = viewModel()
) {
    val accounts by accountViewModel.allAccounts.collectAsState()
    val transactions by transactionViewModel.allTransactions.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var accountToEdit by remember { mutableStateOf<Account?>(null) }
    val totalBalance = accounts.sumOf { it.balance }

    val cardColors = listOf(
        Brush.horizontalGradient(listOf(Color(0xFF2196F3), Color(0xFF64B5F6))),
        Brush.horizontalGradient(listOf(Color(0xFF9C27B0), Color(0xFFBA68C8))),
        Brush.horizontalGradient(listOf(Color(0xFFF44336), Color(0xFFE57373))),
        Brush.horizontalGradient(listOf(Color(0xFF4CAF50), Color(0xFF81C784)))
    )

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Accounts", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Total Balance")
                        Text(
                            String.format("%,.2f INR", totalBalance),
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            itemsIndexed(accounts) { index, account ->
                AccountCard(
                    account = account,
                    transactions = transactions,
                    brush = cardColors[index % cardColors.size],
                    onEdit = { accountToEdit = it }
                )
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
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
                onConfirm = { name, type, balance, currency ->
                    // This now calls the new function to handle balance changes
                    accountViewModel.updateBalanceAndLogTransaction(account.copy(name = name, type = type, currency = currency), balance.toString())
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
fun AccountCard(
    account: Account,
    transactions: List<Transaction>,
    brush: Brush,
    onEdit: (Account) -> Unit
) {
    val calendar = Calendar.getInstance()
    val currentMonth = calendar.get(Calendar.MONTH)
    val currentYear = calendar.get(Calendar.YEAR)

    val monthlyTransactions = transactions.filter {
        val transCal = Calendar.getInstance().apply { time = it.date }
        it.accountId == account.id &&
                transCal.get(Calendar.MONTH) == currentMonth &&
                transCal.get(Calendar.YEAR) == currentYear
    }

    val incomeThisMonth = monthlyTransactions.filter { it.type == "Income" }.sumOf { it.amount }
    val expensesThisMonth = monthlyTransactions.filter { it.type == "Expense" }.sumOf { it.amount }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { onEdit(account) },
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .background(brush)
                .padding(16.dp)
        ) {
            Text(account.name, fontWeight = FontWeight.Bold, color = Color.White)
            Text(
                String.format("%,.2f %s", account.balance, account.currency),
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = Color.White
            )
            Spacer(Modifier.height(16.dp))
            Row {
                Column(Modifier.weight(1f)) {
                    Text("INCOME THIS MONTH", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                    Text(
                        String.format("%,.2f", incomeThisMonth),
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text("EXPENSES THIS MONTH", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                    Text(
                        String.format("%,.2f", expensesThisMonth),
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
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
                    onValueChange = { balance = it },
                    label = { Text(if (isEditMode) "New Balance" else "Initial Balance") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                OutlinedTextField(
                    value = currency,
                    onValueChange = { currency = it },
                    label = { Text("Currency (e.g., USD, INR)") },
                    enabled = !isEditMode // Currency is not editable after creation for simplicity
                )
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