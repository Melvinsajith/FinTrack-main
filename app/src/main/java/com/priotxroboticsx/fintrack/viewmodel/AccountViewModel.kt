package com.priotxroboticsx.fintrack.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.priotxroboticsx.fintrack.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AccountViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val accountDao = db.accountDao()

    val allAccounts: StateFlow<List<Account>> = accountDao.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addAccount(name: String, type: String, initialBalance: Double, currency: String) = viewModelScope.launch {
        val account = Account(name = name, type = type, balance = initialBalance, currency = currency)
        accountDao.insert(account)
    }

    fun updateAccount(account: Account) = viewModelScope.launch {
        accountDao.update(account)
    }

    fun deleteAccount(account: Account) = viewModelScope.launch {
        accountDao.delete(account)
    }
}
