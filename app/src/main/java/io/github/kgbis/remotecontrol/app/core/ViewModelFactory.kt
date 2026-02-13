package io.github.kgbis.remotecontrol.app.core

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class ViewModelFactory<T : ViewModel>(
    private val creator: () -> T
) : ViewModelProvider.Factory {

    override fun <R : ViewModel> create(modelClass: Class<R>): R {
        @Suppress("UNCHECKED_CAST")
        return creator() as R
    }
}
