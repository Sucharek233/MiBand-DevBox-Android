package com.sucharek.devbox.ui.screens.activities.websocket

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharek.devbox.ui.screens.maindashboard.WatchViewModel
import com.sucharek.devbox.utils.NetworkUtils
import com.sucharek.devbox.websocket.WebSocketService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WebSocketViewModel(
    val watchViewModel: WatchViewModel,
    private val context: Context
) : ViewModel() {

    private val _port = MutableStateFlow("8080")
    val port = _port.asStateFlow()

    private val _isServerRunning = MutableStateFlow(false)
    val isServerRunning = _isServerRunning.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy = _isBusy.asStateFlow()

    private val _ipAddress = MutableStateFlow(NetworkUtils.getLocalIpAddress() ?: "Unknown")
    val ipAddress = _ipAddress.asStateFlow()

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs = _logs.asStateFlow()

    private var webSocketService: WebSocketService? = null
    private var isBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as WebSocketService.LocalBinder
            webSocketService = binder.getService()
            isBound = true
            _isServerRunning.value = webSocketService?.isRunning() ?: false
            
            viewModelScope.launch {
                webSocketService?.logs?.collect { log ->
                    _logs.value = (_logs.value + log).takeLast(100)
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            webSocketService = null
            isBound = false
            _isServerRunning.value = false
        }
    }

    init {
        val intent = Intent(context, WebSocketService::class.java)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)

        viewModelScope.launch {
            watchViewModel.rawIncomingMessages.collect { msg ->
                webSocketService?.broadcast(msg)
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
                    webSocketService?.stopServer()
                    _isServerRunning.value = false
                } else {
                    val portInt = _port.value.toIntOrNull() ?: 8080
                    val intent = Intent(context, WebSocketService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(intent)
                    } else {
                        context.startService(intent)
                    }
                    
                    // Wait a bit for service to start and bind if not already
                    webSocketService?.startServer(portInt) { msg ->
                        watchViewModel.sendMessage(msg)
                    }
                    _isServerRunning.value = webSocketService?.isRunning() ?: false
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

    override fun onCleared() {
        super.onCleared()
        if (isBound) {
            context.unbindService(connection)
            isBound = false
        }
    }
}
