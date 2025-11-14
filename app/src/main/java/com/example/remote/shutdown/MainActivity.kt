package com.example.remote.shutdown

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.remote.shutdown.navigation.NavGraph
import com.example.remote.shutdown.ui.theme.RemoteShutdownTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RemoteShutdownTheme {
                NavGraph(this)
            }
        }
    }
}
