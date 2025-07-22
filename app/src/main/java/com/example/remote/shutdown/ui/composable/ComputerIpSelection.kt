package com.example.remote.shutdown.ui.composable

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.remote.shutdown.viewmodel.MainViewModel

@Composable
fun ComputerIpSelection(viewModel: MainViewModel) {
    val computerIp by viewModel.computerIp.collectAsState()

}