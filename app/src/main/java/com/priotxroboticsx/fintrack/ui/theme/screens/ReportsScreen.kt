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
import co.yml.charts.common.model.PlotType
import co.yml.charts.ui.piechart.charts.PieChart
import co.yml.charts.ui.piechart.models.PieChartConfig
import co.yml.charts.ui.piechart.models.PieChartData
import com.priotxroboticsx.fintrack.data.Transaction
import com.priotxroboticsx.fintrack.viewmodel.TransactionViewModel
import java.util.*
import kotlin.random.Random

@Composable
fun ReportsScreen(
    defaultTab: String,
    transactionViewModel: TransactionViewModel = viewModel()
) {
    val allTransactions by transactionViewModel.allTransactions.collectAsState()
    val tabs = listOf("Expenses", "Income")
    var tabIndex by remember { mutableStateOf(if (defaultTab == "Income") 1 else 0) }

    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    var selectedYear by remember { mutableStateOf(currentYear) }
    var selectedMonth by remember { mutableStateOf(Calendar.getInstance().get(Calendar.MONTH)) }

    val filteredTransactions = allTransactions.filter {
        val cal = Calendar.getInstance().apply { time = it.date }
        cal.get(Calendar.YEAR) == selectedYear && cal.get(Calendar.MONTH) == selectedMonth
    }

    Scaffold(
        topBar = {
            ReportTopBar(
                selectedMonth = selectedMonth,
                selectedYear = selectedYear,
                onDateChange = { year, month ->
                    selectedYear = year
                    selectedMonth = month
                },
                onAddClick = { /* TODO */ }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = tabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        text = { Text(title) },
                        selected = tabIndex == index,
                        onClick = { tabIndex = index }
                    )
                }
            }
            val reportType = tabs[tabIndex]
            val transactionsForReport = filteredTransactions.filter { it.type == reportType.removeSuffix("s") }
            ReportContent(
                reportType = reportType,
                transactions = transactionsForReport
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class) // Opt-in for experimental APIs
@Composable
fun ReportTopBar(
    selectedMonth: Int,
    selectedYear: Int,
    onDateChange: (Int, Int) -> Unit,
    onAddClick: () -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val months = (0..11).map {
        val cal = Calendar.getInstance()
        cal.set(Calendar.MONTH, it)
        cal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())!!
    }

    TopAppBar(
        title = { },
        navigationIcon = {
            IconButton(onClick = { /* TODO: Handle back navigation */ }) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        },
        actions = {
            Button(onClick = { showDatePicker = true }) {
                Icon(Icons.Default.CalendarToday, contentDescription = "Select Month", modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("${months[selectedMonth]} $selectedYear")
            }
            IconButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = "Add Transaction")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
    )

    if(showDatePicker) {
        // A simple dialog to select month and year
        AlertDialog(
            onDismissRequest = { showDatePicker = false },
            title = { Text("Select Month and Year") },
            text = {
                // In a real app, you would use a proper date picker library for this
                Text("Date picker placeholder")
            },
            confirmButton = {
                Button(onClick = { showDatePicker = false}) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun ReportContent(reportType: String, transactions: List<Transaction>) {
    val total = transactions.sumOf { it.amount }
    val groupedData = transactions
        .groupBy { it.category }
        .mapValues { entry -> entry.value.sumOf { it.amount } }

    // --- IMPROVEMENT: Create a stable map of colors for each category ---
    val categoryColors = remember(groupedData) {
        groupedData.keys.associateWith { randomColor() }
    }

    val pieSlices = groupedData.map { (category, sum) ->
        PieChartData.Slice(
            category,
            sum.toFloat(),
            categoryColors[category] ?: Color.Gray // Use the stable color
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            Text(reportType, style = MaterialTheme.typography.headlineSmall)
            Text(
                String.format("%,.2f INR", total),
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold)
            )
            Spacer(Modifier.height(24.dp))
        }
        item {
            if (pieSlices.isNotEmpty()) {
                PieChart(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    pieChartData = PieChartData(slices = pieSlices, plotType = PlotType.Donut),
                    pieChartConfig = PieChartConfig(
                        isAnimationEnable = true,
                        showSliceLabels = false,
                        backgroundColor = MaterialTheme.colorScheme.background
                    )
                )
                Spacer(Modifier.height(24.dp))
            } else {
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp), contentAlignment = Alignment.Center) {
                    Text("No data for this period.")
                }
            }
        }

        items(groupedData.entries.toList()) { (category, sum) ->
            CategoryListItem(
                category = category,
                amount = sum,
                percentage = if (total > 0) (sum / total * 100) else 0.0,
                color = categoryColors[category] ?: Color.Gray // Use the same stable color
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun CategoryListItem(category: String, amount: Double, percentage: Double, color: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Wallet, contentDescription = null, tint = color)
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(category, fontWeight = FontWeight.SemiBold)
                Text(String.format("%,.2f INR", amount), fontSize = 14.sp)
            }
            Text(String.format("%.2f%%", percentage))
        }
    }
}

fun randomColor(): Color {
    val random = Random.Default
    return Color(
        red = random.nextInt(256),
        green = random.nextInt(256),
        blue = random.nextInt(256)
    )
}

