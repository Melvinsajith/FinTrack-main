package com.priotxroboticsx.fintrack.ui.theme.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.priotxroboticsx.fintrack.data.Account
import com.priotxroboticsx.fintrack.data.Transaction
import com.priotxroboticsx.fintrack.ui.theme.Green
import com.priotxroboticsx.fintrack.ui.theme.Red
import com.priotxroboticsx.fintrack.viewmodel.AccountViewModel
import com.priotxroboticsx.fintrack.viewmodel.TransactionViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun DashboardScreen() {
    val accountViewModel: AccountViewModel = viewModel()
    val transactionViewModel: TransactionViewModel = viewModel()
    val accounts by accountViewModel.allAccounts.collectAsState()
    val transactions by transactionViewModel.allTransactions.collectAsState()
    val totalBalance = accounts.sumOf { it.balance }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp).animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Welcome Back!", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            SummaryCard(totalBalance)
            Spacer(Modifier.height(24.dp))
            Text("Recent Transactions", style = MaterialTheme.typography.titleLarge)
        }
        if (transactions.isEmpty()) {
            item {
                Text(
                    "No transactions yet. Tap the 'Add' button to get started.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
        } else {
            items(transactions.take(10)) { transaction ->
                TransactionItem(transaction, accounts)
            }
        }
    }
}

@Composable
fun SummaryCard(totalBalance: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(24.dp)) {
            Text("Total Balance", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            Text(String.format("$%.2f", totalBalance), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
fun TransactionItem(transaction: Transaction, accounts: List<Account>) {
    val accountName = accounts.find { it.id == transaction.accountId }?.name ?: "Unknown"
    val (icon, color) = when (transaction.type) {
        "Income" -> Icons.Default.ArrowUpward to Green
        "Expense" -> Icons.Default.ArrowDownward to Red
        else -> Icons.Default.SyncAlt to Color.Gray
    }
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = transaction.type, tint = color)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(transaction.category, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(accountName, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                Text(dateFormat.format(transaction.date), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            }
            Text(
                String.format("%.2f", transaction.amount),
                color = color,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}