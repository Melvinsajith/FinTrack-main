package com.priotxroboticsx.fintrack.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.priotxroboticsx.fintrack.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date
import kotlin.math.abs

class AccountViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val accountDao = db.accountDao()
    private val transactionDao = db.transactionDao()

    val allAccounts: StateFlow<List<Account>> = accountDao.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addAccount(name: String, type: String, initialBalance: Double, currency: String) = viewModelScope.launch {
        val account = Account(name = name, type = type, balance = initialBalance, currency = currency)
        accountDao.insert(account)
    }

    fun updateAccount(account: Account) = viewModelScope.launch {
        accountDao.update(account)
    }

    fun updateBalanceAndLogTransaction(account: Account, newBalanceStr: String) = viewModelScope.launch {
        val newBalance = newBalanceStr.toDoubleOrNull() ?: return@launch
        val difference = newBalance - account.balance

        if (difference != 0.0) {
            val transactionType = if (difference > 0) "Income" else "Expense"
            val transaction = Transaction(
                accountId = account.id,
                type = transactionType,
                amount = abs(difference),
                category = "Balance Adjustment",
                date = Date(),
                notes = "Manual balance update"
            )
            transactionDao.insert(transaction)
        }
        // We directly update the account balance here.
        // This is simpler than trying to reconcile with the transaction viewmodel.
        accountDao.update(account.copy(balance = newBalance))
    }

    fun deleteAccount(account: Account) = viewModelScope.launch {
        accountDao.delete(account)
    }
}
