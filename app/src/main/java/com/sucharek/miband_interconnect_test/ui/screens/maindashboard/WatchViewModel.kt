package com.sucharek.miband_interconnect_test.ui.screens.maindashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharek.miband_interconnect_test.interconnect.Apps
import com.sucharek.miband_interconnect_test.interconnect.DeviceManager
import com.sucharek.miband_interconnect_test.interconnect.Messages
import com.sucharek.miband_interconnect_test.interconnect.Subscriptions
import com.sucharek.miband_interconnect_test.ui.screens.activities.apps.AppsRepository
import com.xiaomi.xms.wearable.node.DataItem
import com.xiaomi.xms.wearable.node.DataSubscribeResult
import com.xiaomi.xms.wearable.node.Node
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

class WatchViewModel(
    private val deviceManager: DeviceManager,
) : ViewModel() {

    private val _connectionState = MutableStateFlow<WatchConnectionState>(WatchConnectionState.Disconnected)
    val connectionState: StateFlow<WatchConnectionState> = _connectionState.asStateFlow()

    private val _isWatchAppInstalled = MutableStateFlow<Boolean?>(null)
    val isWatchAppInstalled: StateFlow<Boolean?> = _isWatchAppInstalled.asStateFlow()

    private val _luaServiceActive = MutableStateFlow(false)
    val luaServiceActive: StateFlow<Boolean> = _luaServiceActive.asStateFlow()

    // Device selection and scanning
    private val _discoveredDevices = MutableStateFlow<List<Node>>(emptyList())
    val discoveredDevices: StateFlow<List<Node>> = _discoveredDevices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanError = MutableStateFlow<String?>(null)
    val scanError: StateFlow<String?> = _scanError.asStateFlow()

    var selectedNode: Node? = null
        private set

    // Terminal
    private val _terminalMessages = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val terminalMessages: SharedFlow<String> = _terminalMessages.asSharedFlow()

    // File explorer
    private val _ioMessages = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val ioMessages: SharedFlow<String> = _ioMessages.asSharedFlow()

    // JS shell
    private val _qjsMessages = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val qjsMessages: SharedFlow<String> = _qjsMessages.asSharedFlow()

    // Module compatibility
    private val _modulesMessages = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val modulesMessages: SharedFlow<String> = _modulesMessages.asSharedFlow()
    
    // Sensors
    private val _sensorMessages = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val sensorMessages: SharedFlow<String> = _sensorMessages.asSharedFlow()

    // Lua Sensors
    private val _luaSensorsMessages = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val luaSensorsMessages: SharedFlow<String> = _luaSensorsMessages.asSharedFlow()

    // Ping
    private val _pingMessages = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val pingMessages: SharedFlow<String> = _pingMessages.asSharedFlow()

    // Lua Shell
    private val _luaShellMessages = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val luaShellMessages: SharedFlow<String> = _luaShellMessages.asSharedFlow()

    // Apps
    private val _appsMessages = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val appsMessages: SharedFlow<String> = _appsMessages.asSharedFlow()

    // System Info
    private val _sysinfoMessages = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val sysinfoMessages: SharedFlow<String> = _sysinfoMessages.asSharedFlow()

    // System / Interconnect Logs
    private val _systemMessages = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val systemMessages: SharedFlow<String> = _systemMessages.asSharedFlow()

    // Mailbox Busy Events
    private val _mailboxBusyEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val mailboxBusyEvents: SharedFlow<Unit> = _mailboxBusyEvents.asSharedFlow()

    val appsRepository = AppsRepository(this, viewModelScope)

    var messagesEngine: Messages? = null
        private set

    var subscriptionsEngine: Subscriptions? = null
        private set

    fun refreshDevices() {
        viewModelScope.launch {
            _isScanning.value = true
            _scanError.value = null
            try {
                val devices = deviceManager.getConnectedDevices()
                _discoveredDevices.value = devices
            } catch (e: Exception) {
                _scanError.value = e.message ?: "Failed to get connected devices"
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun connectToDevice(node: Node, context: android.content.Context) {
        viewModelScope.launch {
            try {
                selectedNode = node

                messagesEngine = Messages(context, node) { rawString ->
                    handleIncomingMessage(rawString)
                }

                messagesEngine?.addIncomingMessageListener()

                subscriptionsEngine = Subscriptions(context, node) { item, result ->
                    handleSubscriptionUpdate(item, result)
                }
                subscriptionsEngine?.subConnection()

                _connectionState.value = WatchConnectionState.Connected(node.name ?: "Unknown Device")
                checkCompanionApp(context)
            } catch (e: Exception) {
                _connectionState.value = WatchConnectionState.Error(e.message ?: "Connection failed")
            }
        }
    }

    fun checkCompanionApp(context: android.content.Context) {
        val node = selectedNode ?: return

        viewModelScope.launch {
            try {
                val appApi = Apps(context, node)
                _isWatchAppInstalled.value = appApi.isAppInstalled()
            } catch (e: Exception) {
                _isWatchAppInstalled.value = false
            }
        }
    }

    fun launchWatchApp(context: android.content.Context) {
        val node = selectedNode ?: return

        viewModelScope.launch {
            try {
                val appApi = Apps(context, node)
                appApi.launchApp()
            } catch (e: Exception) {
                // Log or handle error if needed
                _systemMessages.emit("Failed to launch watch app: ${e.message}")
            }
        }
    }

    fun pingDevice(type: String) {
        viewModelScope.launch {
            try {
                sendStructuredMessage("ping", JSONObject().apply { put("type", type) })
            } catch (e: Exception) {
                _systemMessages.emit("Failed to send Ping")
            }
        }
    }

    private fun handleSubscriptionUpdate(item: DataItem, result: Int) {
        viewModelScope.launch {
            when (item) {
                DataItem.ITEM_CONNECTION -> {
                    if (result == DataSubscribeResult.RESULT_CONNECTION_DISCONNECTED) {
                        _connectionState.value = WatchConnectionState.Disconnected
                        _luaServiceActive.value = false
                        _systemMessages.emit("SYSTEM: Watch Disconnected")
                    } else if (result == DataSubscribeResult.RESULT_CONNECTION_CONNECTED) {
                        val nodeName = selectedNode?.name ?: "Unknown Device"
                        _connectionState.value = WatchConnectionState.Connected(nodeName)
                        _systemMessages.emit("SYSTEM: Watch Connected")
                    }
                }
                else -> {
                    _systemMessages.emit("SYSTEM: Sub Update $item = $result")
                }
            }
        }
    }

    private fun handleIncomingMessage(rawMessage: ByteArray) {
        val message = String(rawMessage)
        println(message)
        viewModelScope.launch {
            try {
                val json = JSONObject(message)
                val type = json.optString("type")
                val state = json.optString("state")
                val errorMsg = json.optString("msg").ifEmpty { json.optString("message") }

                if (state == "error") {
                    if (type != "interconnect") {
                        _systemMessages.emit(message)
                    }
                    if (errorMsg.contains("Mailbox")) {
                        _mailboxBusyEvents.emit(Unit)
                    }
                }

                when (type) {
                    "ping" -> {
                        val state = json.optString("state")
                        if (state == "done") {
                            _luaServiceActive.value = true
                        } else if (state == "timeout" || state == "error") {
                            _luaServiceActive.value = false
                        }
                        _pingMessages.emit(message)
                    }
                    "luashell" -> _luaShellMessages.emit(message)
                    "cmd" -> _terminalMessages.emit(message)
                    "io" -> _ioMessages.emit(message)
                    "qjs" -> _qjsMessages.emit(message)
                    "modules" -> _modulesMessages.emit(message)
                    "sensors" -> _sensorMessages.emit(message)
                    "sensorsLua" -> _luaSensorsMessages.emit(message)
                    "sysinfo" -> _sysinfoMessages.emit(message)
                    "apps" -> _appsMessages.emit(message)
                    "interconnect" -> {
                        _systemMessages.emit(message)
                        if (json.optString("message") == "Mailbox busy") {
                            _mailboxBusyEvents.emit(Unit)
                        }
                    }
                    else -> {
                        _systemMessages.emit("RECV (Unknown Type: $type): $message")
                    }
                }
            } catch (e: Exception) {
                _systemMessages.emit("RECV (Raw/Parse Error): $message")
            }
        }
    }

    fun sendMessage(text: String) {
        println("Sending message: $text")
        viewModelScope.launch {
            try {
                messagesEngine?.sendMessage(text)
            } catch (e: Exception) {
                _systemMessages.emit("LOCAL ERROR: Failed to dispatch data payload")
            }
        }
    }

    fun sendRawMessage(byteArray: ByteArray) {
        viewModelScope.launch {
            try {
                messagesEngine?.sendRawMessage(byteArray)
            } catch (e: Exception) {
                _systemMessages.emit("LOCAL ERROR: Failed to dispatch raw data payload")
            }
        }
    }

    fun sendStructuredMessage(type: String, args: JSONObject = JSONObject()) {
        viewModelScope.launch {
            try {
                val envelope = JSONObject().apply {
                    put("type", type)
                    put("args", args)
                }

                sendMessage(envelope.toString())
            } catch (e: Exception) {
                _systemMessages.emit("LOCAL ERROR: Envelope packaging aborted due to exception")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            try {
                messagesEngine?.removeIncomingMessageListener()
                subscriptionsEngine?.unsubConnection()
            } catch (ignored: Exception) {}
        }
    }
}