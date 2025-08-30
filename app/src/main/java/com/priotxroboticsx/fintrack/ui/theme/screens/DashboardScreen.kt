package com.priotxroboticsx.fintrack.ui.theme.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.priotxroboticsx.fintrack.data.Account
import com.priotxroboticsx.fintrack.data.Transaction
import com.priotxroboticsx.fintrack.ui.theme.Green
import com.priotxroboticsx.fintrack.ui.theme.LightGray
import com.priotxroboticsx.fintrack.ui.theme.Red
import com.priotxroboticsx.fintrack.viewmodel.AccountViewModel
import com.priotxroboticsx.fintrack.viewmodel.SettingsViewModel
import com.priotxroboticsx.fintrack.viewmodel.TransactionViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen() {
    val accountViewModel: AccountViewModel = viewModel()
    val transactionViewModel: TransactionViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()

    val accounts by accountViewModel.allAccounts.collectAsState()
    val transactions by transactionViewModel.allTransactions.collectAsState()
    val user by settingsViewModel.user.collectAsState()

    val mainCurrency = accounts.firstOrNull()?.currency ?: "USD"
    val totalBalance = accounts.sumOf { it.balance }
    val totalIncome = transactions.filter { it.type == "Income" }.sumOf { it.amount }
    val totalExpenses = transactions.filter { it.type == "Expense" }.sumOf { it.amount }
    val cashflow = totalIncome - totalExpenses

    val groupedTransactions = transactions.groupBy {
        SimpleDateFormat("MMMM dd.", Locale.getDefault()).format(it.date)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            DashboardHeader(user?.name)
            Spacer(Modifier.height(24.dp))
            TotalBalanceDisplay(totalBalance, mainCurrency)
            Spacer(Modifier.height(16.dp))
            IncomeExpenseSummary(totalIncome, totalExpenses, cashflow, mainCurrency)
            Spacer(Modifier.height(24.dp))
        }

        groupedTransactions.forEach { (date, transactionsOnDate) ->
            item {
                Text(
                    text = date,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            items(transactionsOnDate) { transaction ->
                DashboardTransactionItem(transaction, accounts)
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun DashboardHeader(userName: String?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Hi ${userName ?: "User"}...", style = MaterialTheme.typography.headlineSmall)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(Icons.Default.CalendarToday, contentDescription = "Date Range", modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("Jan 1 - Aug 30", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun TotalBalanceDisplay(balance: Double, currency: String) {
    Text(
        text = "$currency ${String.format("%,.2f", balance)}",
        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold, fontSize = 36.sp),
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
fun IncomeExpenseSummary(income: Double, expense: Double, cashflow: Double, currency: String) {
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            SummaryBox(
                title = "Income",
                amount = income,
                currency = currency,
                color = Green,
                icon = Icons.Default.ArrowDownward,
                modifier = Modifier.weight(1f)
            )
            SummaryBox(
                title = "Expenses",
                amount = expense,
                currency = currency,
                color = MaterialTheme.colorScheme.onSurface,
                icon = Icons.Default.ArrowUpward,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Cashflow: +${String.format("%,.2f", cashflow)} $currency",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
        )
    }
}

@Composable
fun SummaryBox(title: String, amount: Double, currency: String, color: Color, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (title == "Income") color else LightGray)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = title, tint = if (title == "Income") Color.Black else Color.White)
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.bodyMedium, color = if (title == "Income") Color.Black else Color.White)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "${String.format("%,.2f", amount)} $currency",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = if (title == "Income") Color.Black else Color.White
            )
        }
    }
}

@Composable
fun DashboardTransactionItem(transaction: Transaction, accounts: List<Account>) {
    val account = accounts.find { it.id == transaction.accountId }
    val icon = if (transaction.type == "Income") Icons.Default.ArrowUpward else Icons.Default.ArrowDownward
    val color = if (transaction.type == "Income") Green else Red

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Tag(transaction.category, color = if (transaction.type == "Income") Green else Red)
                    account?.let { Tag(it.name, icon = Icons.Default.AccountBalance) }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = transaction.notes ?: transaction.category,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                )
            }
            Spacer(Modifier.width(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = transaction.type, tint = color, modifier = Modifier.size(20.dp))
                Text(
                    text = String.format("%,.2f", transaction.amount),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun Tag(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector? = null, color: Color = MaterialTheme.colorScheme.surfaceVariant) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.2f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon?.let {
            Icon(it, contentDescription = null, modifier = Modifier.size(14.dp), tint = color)
            Spacer(Modifier.width(4.dp))
        }
        Text(text, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
    }
}
