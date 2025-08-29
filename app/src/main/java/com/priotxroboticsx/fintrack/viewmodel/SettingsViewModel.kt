package com.priotxroboticsx.fintrack.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.priotxroboticsx.fintrack.data.AppDatabase
import com.priotxroboticsx.fintrack.data.User
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val userDao = AppDatabase.getDatabase(application).userDao()

    val user: StateFlow<User?> = userDao.getUser()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun updateUserName(name: String) = viewModelScope.launch {
        userDao.insert(User(name = name))
    }
}