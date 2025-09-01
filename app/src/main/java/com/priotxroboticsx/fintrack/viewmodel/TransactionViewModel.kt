package com.priotxroboticsx.fintrack.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.priotxroboticsx.fintrack.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date

class TransactionViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val transactionDao = db.transactionDao()
    private val accountDao = db.accountDao()

    val allTransactions: StateFlow<List<Transaction>> = transactionDao.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addTransaction(accountId: Int, toAccountId: Int?, type: String, amount: Double, category: String, notes: String?) = viewModelScope.launch {
        val transaction = Transaction(
            accountId = accountId, toAccountId = toAccountId, type = type, amount = amount,
            category = category, date = Date(), notes = notes
        )
        transactionDao.insert(transaction)
        updateBalancesForNewTransaction(transaction)
    }

    private suspend fun updateBalancesForNewTransaction(transaction: Transaction) {
        when (transaction.type) {
            "Income" -> {
                accountDao.getAccount(transaction.accountId).firstOrNull()?.let { account ->
                    accountDao.update(account.copy(balance = account.balance + transaction.amount))
                }
            }
            "Expense" -> {
                accountDao.getAccount(transaction.accountId).firstOrNull()?.let { account ->
                    accountDao.update(account.copy(balance = account.balance - transaction.amount))
                }
            }
            "Transfer" -> {
                accountDao.getAccount(transaction.accountId).firstOrNull()?.let { fromAccount ->
                    accountDao.update(fromAccount.copy(balance = fromAccount.balance - transaction.amount))
                }
                transaction.toAccountId?.let {
                    accountDao.getAccount(it).firstOrNull()?.let { toAccount ->
                        accountDao.update(toAccount.copy(balance = toAccount.balance + transaction.amount))
                    }
                }
            }
        }
    }

    fun deleteTransaction(transaction: Transaction) = viewModelScope.launch {
        // First, revert the balance changes, then delete the transaction record
        updateBalancesForDeletedTransaction(transaction)
        transactionDao.delete(transaction)
    }

    private suspend fun updateBalancesForDeletedTransaction(transaction: Transaction) {
        when (transaction.type) {
            "Income" -> { // Revert income by subtracting the amount
                accountDao.getAccount(transaction.accountId).firstOrNull()?.let { account ->
                    accountDao.update(account.copy(balance = account.balance - transaction.amount))
                }
            }
            "Expense" -> { // Revert expense by adding the amount back
                accountDao.getAccount(transaction.accountId).firstOrNull()?.let { account ->
                    accountDao.update(account.copy(balance = account.balance + transaction.amount))
                }
            }
            "Transfer" -> { // Revert transfer by moving money back
                // Add money back to the 'from' account
                accountDao.getAccount(transaction.accountId).firstOrNull()?.let { fromAccount ->
                    accountDao.update(fromAccount.copy(balance = fromAccount.balance + transaction.amount))
                }
                // Subtract money from the 'to' account
                transaction.toAccountId?.let {
                    accountDao.getAccount(it).firstOrNull()?.let { toAccount ->
                        accountDao.update(toAccount.copy(balance = toAccount.balance - transaction.amount))
                    }
                }
            }
        }
    }
}

