package com.priotxroboticsx.fintrack.ui.theme.screens

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.priotxroboticsx.fintrack.data.Account
import com.priotxroboticsx.fintrack.data.Transaction
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
    var showEditNameDialog by remember { mutableStateOf(false) }

    // --- NEW: State for date range selection ---
    val initialDate = Calendar.getInstance()
    var startDate by remember { mutableStateOf(initialDate.clone() as Calendar) }
    var endDate by remember { mutableStateOf(initialDate.clone() as Calendar) }
    var showDatePicker by remember { mutableStateOf(false) }

    // --- NEW: Filter transactions based on the selected date range ---
    val filteredTransactions = remember(transactions, startDate, endDate) {
        val startCal = (startDate.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
        }
        val endCal = (endDate.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59)
        }
        transactions.filter {
            val transactionCal = Calendar.getInstance().apply { time = it.date }
            !transactionCal.before(startCal) && !transactionCal.after(endCal)
        }
    }

    val context = LocalContext.current
    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            exportToCsv(context, it, filteredTransactions, accounts, showSnackbar)
        }
    }
    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        uri?.let {
            exportToPdf(context, it, filteredTransactions, accounts, showSnackbar)
        }
    }

    if (showEditNameDialog) {
        EditUserNameDialog(
            currentName = user?.name ?: "",
            onDismiss = { showEditNameDialog = false },
            onConfirm = { newName ->
                settingsViewModel.updateUserName(newName)
                showSnackbar("Name Updated!")
                showEditNameDialog = false
            }
        )
    }

    if (showDatePicker) {
        MonthYearRangePickerDialog(
            initialStartDate = startDate,
            initialEndDate = endDate,
            onDismiss = { showDatePicker = false },
            onConfirm = { newStart, newEnd ->
                startDate = newStart
                endDate = newEnd
                showDatePicker = false
            }
        )
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Person, contentDescription = "User", modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(16.dp))
                        Text(user?.name ?: "Set Your Name", modifier = Modifier.weight(1f))
                        IconButton(onClick = { showEditNameDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Name")
                        }
                    }
                }
            }
        }

        item {
            SettingsSection("IMPORT & EXPORT") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val formatter = SimpleDateFormat("MMM yyyy", Locale.getDefault())
                    SettingsRow(
                        icon = Icons.Default.CalendarToday,
                        title = "Export Date Range",
                        subtitle = "${formatter.format(startDate.time)} - ${formatter.format(endDate.time)}",
                        onClick = { showDatePicker = true }
                    )
                    SettingsRow(
                        icon = Icons.Default.Download,
                        title = "Export to CSV",
                        subtitle = "Export transactions to a CSV file",
                        onClick = {
                            if (filteredTransactions.isNotEmpty()) {
                                csvLauncher.launch("fintrack_export_${System.currentTimeMillis()}.csv")
                            } else {
                                showSnackbar("No transactions in selected period")
                            }
                        }
                    )
                    SettingsRow(
                        icon = Icons.Default.PictureAsPdf,
                        title = "Export to PDF",
                        subtitle = "Save a PDF report of your transactions",
                        onClick = {
                            if (filteredTransactions.isNotEmpty()) {
                                pdfLauncher.launch("fintrack_report_${System.currentTimeMillis()}.pdf")
                            } else {
                                showSnackbar("No transactions in selected period")
                            }
                        }
                    )
                    SettingsRow(icon = Icons.Default.Backup, title = "Backup data", onClick = { /* TODO */ })
                }
            }
        }
    }
}


@Composable
fun EditUserNameDialog(currentName: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Your Name") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(name) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
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
    val containerColor = if (isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
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
                writer.appendLine("Date,Type,Category,Amount,Currency,Account,Notes")
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

private fun exportToPdf(
    context: Context,
    uri: Uri,
    transactions: List<Transaction>,
    accounts: List<Account>,
    showSnackbar: (String) -> Unit
) {
    val pdfDocument = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
    val page = pdfDocument.startPage(pageInfo)
    val canvas = page.canvas
    val paint = Paint()
    val titlePaint = Paint()

    var yPosition = 40f
    val leftMargin = 40f

    titlePaint.textSize = 18f
    titlePaint.isFakeBoldText = true
    canvas.drawText("FinTrack Transaction Report", leftMargin, yPosition, titlePaint)
    yPosition += 40f

    paint.textSize = 12f
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    transactions.forEach { transaction ->
        val account = accounts.find { it.id == transaction.accountId }
        val line =
            "${dateFormat.format(transaction.date)} - ${transaction.type}: ${transaction.category} " +
                    "(${String.format("%.2f", transaction.amount)} ${account?.currency}) " +
                    "on ${account?.name}"
        canvas.drawText(line, leftMargin, yPosition, paint)
        yPosition += 20f

        if (yPosition > 800f) { // Simple pagination
            pdfDocument.finishPage(page)
            return@forEach
        }
    }

    pdfDocument.finishPage(page)

    try {
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            pdfDocument.writeTo(outputStream)
        }
        showSnackbar("PDF exported successfully!")
    } catch (e: IOException) {
        e.printStackTrace()
        showSnackbar("PDF export failed.")
    } finally {
        pdfDocument.close()
    }
}

