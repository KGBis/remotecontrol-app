package io.github.kgbis.remotecontrol.app.core.model

import androidx.annotation.StringRes
import io.github.kgbis.remotecontrol.app.R

enum class InterfaceType(@param:StringRes val labelRes: Int) {
    ETHERNET(R.string.iface_type_ethernet), WIFI(R.string.iface_type_wifi), UNKNOWN(R.string.iface_type_unknown)
}