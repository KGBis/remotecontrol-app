package com.example.remote.shutdown.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.Factory
import com.example.remote.shutdown.common.UserPreferences

class MainViewModelFactory(private val prefs: UserPreferences) : Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MainViewModel(prefs) as T
    }
}
