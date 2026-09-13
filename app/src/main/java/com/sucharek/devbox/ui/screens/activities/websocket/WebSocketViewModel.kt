package com.sucharek.devbox.ui.screens.activities.websocket

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharek.devbox.ui.screens.maindashboard.WatchViewModel
import com.sucharek.devbox.utils.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WebSocketViewModel(
    val watchViewModel: WatchViewModel
) : ViewModel() {

    private val _port = MutableStateFlow("8080")
    val port = _port.asStateFlow()

    private val _isServerRunning = MutableStateFlow(watchViewModel.webSocketManager.isRunning())
    val isServerRunning = _isServerRunning.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy = _isBusy.asStateFlow()

    private val _ipAddress = MutableStateFlow(NetworkUtils.getLocalIpAddress() ?: "Unknown")
    val ipAddress = _ipAddress.asStateFlow()

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs = _logs.asStateFlow()

    init {
        viewModelScope.launch {
            watchViewModel.webSocketLogs.collect { log ->
                _logs.value = (_logs.value + log).takeLast(100)
            }
        }
    }

    fun setPort(newPort: String) {
        if (newPort.all { it.isDigit() }) {
            _port.value = newPort
        }
    }

    fun toggleServer() {
        viewModelScope.launch {
            _isBusy.value = true
            try {
                if (_isServerRunning.value) {
                    withContext(Dispatchers.IO) {
                        watchViewModel.webSocketManager.stop()
                    }
                    _isServerRunning.value = false
                } else {
                    val portInt = _port.value.toIntOrNull() ?: 8080
                    withContext(Dispatchers.IO) {
                        watchViewModel.webSocketManager.start(portInt)
                    }
                    _isServerRunning.value = watchViewModel.webSocketManager.isRunning()
                }
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }

    fun refreshIp() {
        _ipAddress.value = NetworkUtils.getLocalIpAddress() ?: "Unknown"
    }
}
