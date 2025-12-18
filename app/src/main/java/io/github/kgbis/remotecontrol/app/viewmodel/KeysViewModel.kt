package io.github.kgbis.remotecontrol.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import io.github.kgbis.remotecontrol.app.util.Utils

class KeysViewModel(application: Application) : AndroidViewModel(application) {

    val aboutKeys: Map<String, String> by lazy {
        Utils.loadAboutKeys(getApplication())
    }

}
