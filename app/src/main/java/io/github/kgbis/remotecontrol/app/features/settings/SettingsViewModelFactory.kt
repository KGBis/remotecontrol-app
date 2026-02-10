package io.github.kgbis.remotecontrol.app.features.settings

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.github.kgbis.remotecontrol.app.core.repository.SettingsRepositoryContract

class SettingsViewModelFactory(
    private val app: Application,
    private val repo: SettingsRepositoryContract
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(app, repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
