package com.priotxroboticsx.fintrack.ui.theme.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

@Composable
fun ReportsScreen(
    defaultTab: String,
    onClose: () -> Unit,
    transactionViewModel: TransactionViewModel = viewModel()
) {
    val allTransactions by transactionViewModel.allTransactions.collectAsState()
    val tabs = listOf("Expenses", "Income")
    var tabIndex by remember { mutableStateOf(if (defaultTab == "Income") 1 else 0) }

    // --- CHANGE: Use a start and end date for filtering ---
    val initialDate = Calendar.getInstance()
    var startDate by remember { mutableStateOf(initialDate.clone() as Calendar) }
    var endDate by remember { mutableStateOf(initialDate.clone() as Calendar) }

    val filteredTransactions = allTransactions.filter {
        val transactionCal = Calendar.getInstance().apply { time = it.date }

        // Normalize start date to the beginning of the selected month
        val startCal = (startDate.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }

        // Normalize end date to the end of the selected month
        val endCal = (endDate.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }

        !transactionCal.before(startCal) && !transactionCal.after(endCal)
    }

    Scaffold(
        topBar = {
            ReportTopBar(
                startDate = startDate,
                endDate = endDate,
                onDateChange = { newStart, newEnd ->
                    startDate = newStart
                    endDate = newEnd
                },
                onCloseClick = onClose
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportTopBar(
    startDate: Calendar,
    endDate: Calendar,
    onDateChange: (Calendar, Calendar) -> Unit,
    onCloseClick: () -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val formatter = SimpleDateFormat("MMM yyyy", Locale.getDefault())

    TopAppBar(
        title = { },
        navigationIcon = {
            IconButton(onClick = onCloseClick) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        },
        actions = {
            Button(onClick = { showDatePicker = true }) {
                Icon(Icons.Default.CalendarToday, contentDescription = "Select Date Range", modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("${formatter.format(startDate.time)} - ${formatter.format(endDate.time)}")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
    )

    if(showDatePicker) {
        MonthYearRangePickerDialog(
            initialStartDate = startDate,
            initialEndDate = endDate,
            onDismiss = { showDatePicker = false },
            onConfirm = { newStart, newEnd ->
                onDateChange(newStart, newEnd)
                showDatePicker = false
            }
        )
    }
}

@Composable
fun MonthYearRangePickerDialog(
    initialStartDate: Calendar,
    initialEndDate: Calendar,
    onDismiss: () -> Unit,
    onConfirm: (startDate: Calendar, endDate: Calendar) -> Unit
) {
    var startYear by remember { mutableStateOf(initialStartDate.get(Calendar.YEAR)) }
    var startMonth by remember { mutableStateOf(initialStartDate.get(Calendar.MONTH)) }
    var endYear by remember { mutableStateOf(initialEndDate.get(Calendar.YEAR)) }
    var endMonth by remember { mutableStateOf(initialEndDate.get(Calendar.MONTH)) }

    val months = (0..11).map {
        val cal = Calendar.getInstance()
        cal.set(Calendar.MONTH, it)
        cal.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault())!!
    }

    // Ensures the end date is always on or after the start date
    LaunchedEffect(startYear, startMonth) {
        val startCal = Calendar.getInstance().apply { set(startYear, startMonth, 1) }
        val endCal = Calendar.getInstance().apply { set(endYear, endMonth, 1) }
        if (startCal.after(endCal)) {
            endYear = startYear
            endMonth = startMonth
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Date Range") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                // Start Date Picker
                Column {
                    Text("Start Date", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    DateSelector(year = startYear, month = startMonth, months = months,
                        onYearChange = { startYear = it },
                        onMonthChange = { startMonth = it }
                    )
                }

                // End Date Picker
                Column {
                    Text("End Date", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    DateSelector(year = endYear, month = endMonth, months = months,
                        onYearChange = { endYear = it },
                        onMonthChange = { endMonth = it },
                        minDate = Calendar.getInstance().apply { set(startYear, startMonth, 1) }
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val finalStart = Calendar.getInstance().apply { set(startYear, startMonth, 1) }
                val finalEnd = Calendar.getInstance().apply { set(endYear, endMonth, 1) }
                onConfirm(finalStart, finalEnd)
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun DateSelector(
    year: Int,
    month: Int,
    months: List<String>,
    onYearChange: (Int) -> Unit,
    onMonthChange: (Int) -> Unit,
    minDate: Calendar? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Year Selector
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { onYearChange(year - 1) },
                enabled = minDate == null || (year - 1) >= minDate.get(Calendar.YEAR)
            ) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous Year") }
            Text(year.toString(), style = MaterialTheme.typography.bodyLarge)
            IconButton(onClick = { onYearChange(year + 1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next Year") }
        }

        // Month Selector
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { onMonthChange((month - 1 + 12) % 12) },
                enabled = minDate == null || year > minDate.get(Calendar.YEAR) || (year == minDate.get(Calendar.YEAR) && month > minDate.get(Calendar.MONTH))
            ) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous Month") }
            Text(months[month], style = MaterialTheme.typography.bodyLarge)
            IconButton(onClick = { onMonthChange((month + 1) % 12) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next Month") }
        }
    }
}


@Composable
fun ReportContent(reportType: String, transactions: List<Transaction>) {
    val total = transactions.sumOf { it.amount }
    val groupedData = transactions
        .groupBy { it.category }
        .mapValues { entry -> entry.value.sumOf { it.amount } }

    val categoryColors = remember(groupedData) {
        groupedData.keys.associateWith { randomColor() }
    }

    val pieSlices = groupedData.map { (category, sum) ->
        PieChartData.Slice(
            label = category,
            value = sum.toFloat(),
            color = (categoryColors[category] ?: Color.Gray).copy(alpha = 0.8f)
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
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    PieChart(
                        modifier = Modifier.size(200.dp),
                        pieChartData = PieChartData(slices = pieSlices, plotType = PlotType.Pie),
                        pieChartConfig = PieChartConfig(
                            isAnimationEnable = true,
                            showSliceLabels = true,
                            sliceLabelTextSize = 12.sp,
                            sliceLabelTextColor = MaterialTheme.colorScheme.onSurface,
                            backgroundColor = MaterialTheme.colorScheme.background
                        )
                    )
                }
                Spacer(Modifier.height(24.dp))
            } else {
                Box(
                    modifier = Modifier
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
                color = categoryColors[category] ?: Color.Gray
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

