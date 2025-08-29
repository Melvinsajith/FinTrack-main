package com.priotxroboticsx.fintrack.ui.theme.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.priotxroboticsx.fintrack.viewmodel.AccountViewModel
import com.priotxroboticsx.fintrack.viewmodel.TransactionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(onTransactionAdded: () -> Unit) {
    val accountViewModel: AccountViewModel = viewModel()
    val transactionViewModel: TransactionViewModel = viewModel()
    val accounts by accountViewModel.allAccounts.collectAsState()

    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var transactionType by remember { mutableStateOf("Expense") }
    var fromAccountId by remember { mutableStateOf<Int?>(null) }
    var toAccountId by remember { mutableStateOf<Int?>(null) }
    var isFromAccountMenuExpanded by remember { mutableStateOf(false) }
    var isToAccountMenuExpanded by remember { mutableStateOf(false) }
    var selectedTypeIndex by remember { mutableStateOf(0) }
    val types = listOf("Expense", "Income", "Transfer")

    Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Add Transaction", style = MaterialTheme.typography.headlineSmall)

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            types.forEachIndexed { index, label ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = types.size),
                    onClick = {
                        selectedTypeIndex = index
                        transactionType = types[index]
                    },
                    selected = index == selectedTypeIndex
                ) {
                    Text(label)
                }
            }
        }

        OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())

        if (transactionType != "Transfer") {
            OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category (e.g., Food)") }, modifier = Modifier.fillMaxWidth())
        }

        AccountDropDown(
            label = "From Account",
            accounts = accounts,
            selectedAccountId = fromAccountId,
            onAccountSelected = { fromAccountId = it },
            isExpanded = isFromAccountMenuExpanded,
            onExpandedChange = { isFromAccountMenuExpanded = it }
        )

        if (transactionType == "Transfer") {
            AccountDropDown(
                label = "To Account",
                accounts = accounts.filter{ it.id != fromAccountId },
                selectedAccountId = toAccountId,
                onAccountSelected = { toAccountId = it },
                isExpanded = isToAccountMenuExpanded,
                onExpandedChange = { isToAccountMenuExpanded = it }
            )
        }

        OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes (Optional)") }, modifier = Modifier.fillMaxWidth())

        Button(
            onClick = {
                val amountValue = amount.toDoubleOrNull()
                if (amountValue != null && fromAccountId != null && (category.isNotBlank() || transactionType == "Transfer")) {
                    transactionViewModel.addTransaction(fromAccountId!!, toAccountId, transactionType, amountValue, if (transactionType == "Transfer") "Transfer" else category, notes)
                    onTransactionAdded()
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Save Transaction")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDropDown(
    label: String,
    accounts: List<com.priotxroboticsx.fintrack.data.Account>,
    selectedAccountId: Int?,
    onAccountSelected: (Int) -> Unit,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit
) {
    ExposedDropdownMenuBox(expanded = isExpanded, onExpandedChange = { onExpandedChange(!isExpanded) }) {
        OutlinedTextField(
            value = accounts.find { it.id == selectedAccountId }?.name ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = isExpanded, onDismissRequest = { onExpandedChange(false) }) {
            accounts.forEach { account ->
                DropdownMenuItem(text = { Text(account.name) }, onClick = {
                    onAccountSelected(account.id)
                    onExpandedChange(false)
                })
            }
        }
    }
}
