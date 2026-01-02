package io.github.kgbis.remotecontrol.app.features.about

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import io.github.kgbis.remotecontrol.app.core.util.Utils

class AboutViewModel(application: Application) : AndroidViewModel(application) {

    val aboutKeys: Map<String, String> by lazy {
        Utils.loadAboutKeys(getApplication())
    }

}
