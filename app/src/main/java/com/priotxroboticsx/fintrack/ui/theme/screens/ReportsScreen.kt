// File: ui/screens/ReportsScreen.kt
package com.priotxroboticsx.fintrack.ui.theme.screens

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import co.yml.charts.axis.AxisData
import co.yml.charts.common.model.PlotType
import co.yml.charts.common.model.Point
import co.yml.charts.ui.barchart.BarChart
import co.yml.charts.ui.barchart.models.BarChartData
import co.yml.charts.ui.barchart.models.BarData
import co.yml.charts.ui.piechart.charts.PieChart
import co.yml.charts.ui.piechart.models.PieChartConfig
import co.yml.charts.ui.piechart.models.PieChartData
import com.priotxroboticsx.fintrack.data.Transaction
import com.priotxroboticsx.fintrack.ui.theme.Green
import com.priotxroboticsx.fintrack.ui.theme.Red
import com.priotxroboticsx.fintrack.viewmodel.TransactionViewModel
import java.util.*
import kotlin.random.Random

@Composable
fun ReportsScreen(transactionViewModel: TransactionViewModel = viewModel()) {
    val allTransactions by transactionViewModel.allTransactions.collectAsState()
    var tabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Expenses", "Summary")

    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    var selectedYear by remember { mutableStateOf(currentYear) }
    var selectedMonth by remember { mutableStateOf(Calendar.getInstance().get(Calendar.MONTH)) }
    val years = (2020..currentYear).toList()
    val months = (0..11).map {
        val cal = Calendar.getInstance()
        cal.set(Calendar.MONTH, it)
        cal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())!!
    }

    val filteredTransactions = allTransactions.filter {
        val cal = Calendar.getInstance().apply { time = it.date }
        cal.get(Calendar.YEAR) == selectedYear && cal.get(Calendar.MONTH) == selectedMonth
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tabIndex) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    text = { Text(title) },
                    selected = tabIndex == index,
                    onClick = { tabIndex = index }
                )
            }
        }
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterDropDown(
                label = "Year",
                items = years.map { it.toString() },
                selectedValue = selectedYear.toString(),
                onItemSelected = { selectedYear = it.toInt() },
                modifier = Modifier.weight(1f)
            )
            FilterDropDown(
                label = "Month",
                items = months,
                selectedValue = months[selectedMonth],
                onItemSelected = { selectedMonth = months.indexOf(it) },
                modifier = Modifier.weight(1f)
            )
        }

        Crossfade(targetState = tabIndex, label = "report-chart") { currentTab ->
            when (currentTab) {
                0 -> ExpensePieChart(transactions = filteredTransactions)
                1 -> SummaryBarChart(transactions = filteredTransactions)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterDropDown(
    label: String,
    items: List<String>,
    selectedValue: String,
    onItemSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = isExpanded,
        onExpandedChange = { isExpanded = !isExpanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedValue,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = isExpanded, onDismissRequest = { isExpanded = false }) {
            items.forEach { item ->
                DropdownMenuItem(text = { Text(item) }, onClick = {
                    onItemSelected(item)
                    isExpanded = false
                })
            }
        }
    }
}

@Composable
fun ExpensePieChart(transactions: List<Transaction>) {
    val expenseData = transactions
        .filter { it.type == "Expense" }
        .groupBy { it.category }
        .mapValues { entry -> entry.value.sumOf { it.amount } }
        .map { (category, sum) -> PieChartData.Slice(category, sum.toFloat(), randomColor()) }

    Column(modifier = Modifier.padding(16.dp)) {
        if (expenseData.isNotEmpty()) {
            PieChart(
                modifier = Modifier.fillMaxWidth().height(300.dp),
                pieChartData = PieChartData(slices = expenseData, plotType = PlotType.Pie),
                pieChartConfig = PieChartConfig(
                    isAnimationEnable = true,
                    showSliceLabels = true,
                    sliceLabelTextSize = 16.sp,
                    backgroundColor = MaterialTheme.colorScheme.background
                )
            )
        } else {
            Text("No expense data for this period.")
        }
    }
}

@Composable
fun SummaryBarChart(transactions: List<Transaction>) {
    val totalIncome = transactions.filter { it.type == "Income" }.sumOf { it.amount }.toFloat()
    val totalExpense = transactions.filter { it.type == "Expense" }.sumOf { it.amount }.toFloat()

    val barData = listOf(
        BarData(point = Point(0f, totalIncome), color = Green, label = "Income"),
        BarData(point = Point(0f, totalExpense), color = Red, label = "Expense")
    )

    val xAxisData = AxisData.Builder()
        .axisStepSize(30.dp)
        .steps(barData.size - 1)
        .bottomPadding(40.dp)
        .axisLabelAngle(20f)
        .labelData { index -> barData[index].label }
        .axisLineColor(MaterialTheme.colorScheme.onBackground)
        .axisLabelColor(MaterialTheme.colorScheme.onBackground)
        .build()

    val yAxisData = AxisData.Builder()
        .steps(5)
        .labelAndAxisLinePadding(20.dp)
        .axisLineColor(MaterialTheme.colorScheme.onBackground)
        .axisLabelColor(MaterialTheme.colorScheme.onBackground)
        .build()

    val barChartData = BarChartData(
        chartData = barData,
        xAxisData = xAxisData,
        yAxisData = yAxisData,
        backgroundColor = MaterialTheme.colorScheme.background
    )

    Column(modifier = Modifier.padding(16.dp)) {
        if (barData.any { it.point.y > 0f }) {
            BarChart(modifier = Modifier.height(300.dp), barChartData = barChartData)
        } else {
            Text("No summary data for this period.")
        }
    }
}


fun randomColor(): Color {
    val random = Random.Default
    return Color(
        red = random.nextFloat(),
        green = random.nextFloat(),
        blue = random.nextFloat(),
        alpha = 1f
    )
}

