package com.sucharek.devbox.ui.screens.maindashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharek.devbox.interconnect.Apps
import com.sucharek.devbox.interconnect.DeviceManager
import com.sucharek.devbox.interconnect.Messages
import com.sucharek.devbox.interconnect.Permissions
import com.sucharek.devbox.interconnect.Subscriptions
import com.sucharek.devbox.ui.screens.activities.apps.AppsRepository
import com.sucharek.devbox.models.LogType
import com.sucharek.devbox.models.MessageStates
import com.sucharek.devbox.models.SystemLogEntry
import com.xiaomi.xms.wearable.node.DataItem
import com.xiaomi.xms.wearable.node.DataSubscribeResult
import com.xiaomi.xms.wearable.node.Node
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

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

    // Lua SysInfo
    private val _luaSysInfoMessages = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val luaSysInfoMessages: SharedFlow<String> = _luaSysInfoMessages.asSharedFlow()

    // Apps
    private val _appsMessages = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val appsMessages: SharedFlow<String> = _appsMessages.asSharedFlow()

    // System Info
    private val _sysinfoMessages = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val sysinfoMessages: SharedFlow<String> = _sysinfoMessages.asSharedFlow()

    // System / Interconnect Logs
    private val _systemMessages = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val systemMessages: SharedFlow<String> = _systemMessages.asSharedFlow()

    private val _systemLogEntries = MutableStateFlow<List<SystemLogEntry>>(emptyList())
    val systemLogEntries: StateFlow<List<SystemLogEntry>> = _systemLogEntries.asStateFlow()

    // Mailbox Busy Events
    private val _mailboxBusyEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val mailboxBusyEvents: SharedFlow<Unit> = _mailboxBusyEvents.asSharedFlow()

    // Screen navigation events for cancellation
    private val _operationCanceledEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val operationCanceledEvents: SharedFlow<Unit> = _operationCanceledEvents.asSharedFlow()

    // Global Operation Timing
    private val _longRunningOperation = MutableStateFlow<String?>(null)
    val longRunningOperation: StateFlow<String?> = _longRunningOperation.asStateFlow()

    private val _isLuaBusy = MutableStateFlow(false)
    val isLuaBusy: StateFlow<Boolean> = _isLuaBusy.asStateFlow()

    private val _isJsBusy = MutableStateFlow(false)
    val isJsBusy: StateFlow<Boolean> = _isJsBusy.asStateFlow()

    @Deprecated("Use isLuaBusy or isJsBusy")
    val isAnyOperationActive: StateFlow<Boolean> = _isLuaBusy.asStateFlow()

    private val _lastTimeoutError = MutableStateFlow<String?>(null)
    val lastTimeoutError: StateFlow<String?> = _lastTimeoutError.asStateFlow()

    private val _isMailboxBusyError = MutableStateFlow(false)
    val isMailboxBusyError: StateFlow<Boolean> = _isMailboxBusyError.asStateFlow()

    private val activeOperations = mutableMapOf<String, Long>()
    private var timeoutJob: Job? = null

    val appsRepository = AppsRepository(this, viewModelScope)

    var messagesEngine: Messages? = null
        private set

    var subscriptionsEngine: Subscriptions? = null
        private set

    init {
        viewModelScope.launch {
            systemMessages.collectLatest { rawMessage ->
                parseAndAddLog(rawMessage)
            }
        }
    }

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

                // 1. Prepare permissions helper
                val permissions = Permissions(context, node)
                
                // We'll try to add the listener first. If it fails with permission denied,
                // we'll explicitly request them from the user/system.
                try {
                    messagesEngine = Messages(context, node) { rawString ->
                        handleIncomingMessage(rawString)
                    }
                    messagesEngine?.addIncomingMessageListener()
                } catch (e: Exception) {
                    if (e.message?.contains("permission denied") == true) {
                        // Request permissions and retry once
                        suspendCancellableCoroutine<List<String>> { cont ->
                            permissions.requestPermissions(
                                onSuccess = { cont.resume(it) },
                                onFailure = { cont.resumeWithException(it) }
                            )
                        }
                        messagesEngine?.addIncomingMessageListener()
                    } else {
                        throw e
                    }
                }

                subscriptionsEngine = Subscriptions(context, node) { item, result ->
                    handleSubscriptionUpdate(item, result)
                }
                subscriptionsEngine?.subConnection()

                _connectionState.value = WatchConnectionState.Connected(node.name ?: "Unknown Device")
                checkCompanionApp(context)
            } catch (e: Exception) {
                e.printStackTrace()
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
            _systemMessages.emit("RECV: $message")
            try {
                val json = JSONObject(message)
                val type = json.optString("type")
                val state = json.optString("state")
                val errorMsg = json.optString("msg").ifEmpty { json.optString("message") }

                // 1. Clear tracking for any terminal state (Done, Error, or Timeout)
                if (state == MessageStates.DONE || state == MessageStates.ERROR || state == MessageStates.TIMEOUT) {
                    val msg = errorMsg.lowercase().trim()
                    val isMailboxBusy = msg == "mailbox busy"
                    val isMailboxTimeout = msg == "mailbox timeout"
                    
                    if (isMailboxBusy || isMailboxTimeout) {
                        if (isMailboxBusy) {
                            _isMailboxBusyError.value = true
                        } else {
                            _lastTimeoutError.value = "The band reported a timeout: $errorMsg"
                        }
                        
                        // Clear ONLY Lua operations as these are mailbox specific
                        synchronized(activeOperations) {
                            val keysToRemove = activeOperations.keys.filter { isLuaType(it) }
                            keysToRemove.forEach { activeOperations.remove(it) }
                            _isLuaBusy.value = activeOperations.keys.any { isLuaType(it) }
                        }
                        _longRunningOperation.value = null
                        
                        // Notify ViewModels to stop loading states
                        _mailboxBusyEvents.emit(Unit)
                        
                        // Block emission of this global error to specific feature flows (Shells, etc.)
                        return@launch
                    } else {
                        clearOperationTracking(type)
                    }
                }

                // 2. Standard Error Handling for Logs
                if (state == MessageStates.ERROR) {
                    // Refined: Only log if it's NOT a global mailbox timeout
                    if (type != "interconnect" && errorMsg != "Mailbox timeout" && errorMsg != "Mailbox busy") {
                        _systemMessages.emit(message)
                    }
                }

                // 3. Dispatch to specific Flows
                when (type) {
                    "ping" -> {
                        val pingState = json.optString("state")
                        if (pingState == MessageStates.DONE) {
                            _luaServiceActive.value = true
                        } else if (pingState == MessageStates.TIMEOUT || pingState == MessageStates.ERROR) {
                            _luaServiceActive.value = false
                        }
                        _pingMessages.emit(message)
                    }
                    "luashell" -> _luaShellMessages.emit(message)
                    "sysInfoLua" -> _luaSysInfoMessages.emit(message)
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
            _systemMessages.emit("SENT: $text")
            try {
                messagesEngine?.sendMessage(text)
            } catch (e: Exception) {
                _systemMessages.emit("LOCAL ERROR: Failed to dispatch data payload")
            }
        }
    }

    fun sendRawMessage(byteArray: ByteArray) {
        viewModelScope.launch {
            _systemMessages.emit("SENT: [Raw Data ${byteArray.size} bytes]")
            try {
                messagesEngine?.sendRawMessage(byteArray)
            } catch (e: Exception) {
                _systemMessages.emit("LOCAL ERROR: Failed to dispatch raw data payload")
            }
        }
    }

    fun sendStructuredMessage(type: String, args: JSONObject = JSONObject()) {
        // Start tracking for all operations to ensure they are acknowledged
        // EXCEPT for download-related IO operations which can naturally take a long time
        val subType = args.optString("type")
        val isDownloadOp = type == "io" && (subType == "getStream" || subType == "chunk")
        
        if (!isDownloadOp) {
            startOperationTracking(type)
        }

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

    private fun startOperationTracking(type: String) {
        synchronized(activeOperations) {
            activeOperations[type] = System.currentTimeMillis()
            if (isLuaType(type)) _isLuaBusy.value = true
            else _isJsBusy.value = true
        }
        
        if (timeoutJob == null || timeoutJob?.isActive == false) {
            timeoutJob = viewModelScope.launch(Dispatchers.Default) {
                while (true) {
                    val now = System.currentTimeMillis()
                    val slowOp = synchronized(activeOperations) {
                        activeOperations.entries.find { now - it.value > 2000 }?.key
                    }
                    
                    _longRunningOperation.value = slowOp
                    
                    if (slowOp == null && synchronized(activeOperations) { activeOperations.isEmpty() }) {
                        break
                    }
                    delay(500)
                }
            }
        }
    }

    private fun clearOperationTracking(type: String) {
        synchronized(activeOperations) {
            activeOperations.remove(type)
            if (isLuaType(type)) {
                _isLuaBusy.value = activeOperations.keys.any { isLuaType(it) }
            } else {
                _isJsBusy.value = activeOperations.keys.any { !isLuaType(it) }
            }
        }
        if (_longRunningOperation.value == type) {
            _longRunningOperation.value = null
        }
    }

    fun cancelActiveOperation() {
        synchronized(activeOperations) {
            activeOperations.clear()
            _isLuaBusy.value = false
            _isJsBusy.value = false
        }
        _longRunningOperation.value = null
        
        // Also emit mailbox reset to let ViewModels know they should stop loading
        viewModelScope.launch {
            // Emit reset FIRST so ViewModels update their state before navigation occurs
            _mailboxBusyEvents.emit(Unit)
            _operationCanceledEvents.emit(Unit)
            _systemMessages.emit("LOCAL: All operations canceled by user due to timeout.")
        }
    }

    fun dismissTimeoutError() {
        _lastTimeoutError.value = null
    }

    fun dismissMailboxBusyError() {
        _isMailboxBusyError.value = false
    }

    private fun isLuaType(type: String): Boolean {
        return type == "cmd" || type == "luashell" || type == "io" || 
               type == "apps" || type == "sysInfoLua" || type == "sensorsLua"
    }

    fun clearLogs() {
        _systemLogEntries.value = emptyList()
    }

    private fun parseAndAddLog(rawMessage: String) {
        val entry = try {
            if (rawMessage.startsWith("SENT:") || rawMessage.startsWith("RECV:")) {
                val isSent = rawMessage.startsWith("SENT:")
                val jsonStr = rawMessage.substring(5).trim()
                val type = if (isSent) LogType.SENT else LogType.RECV

                if (jsonStr.startsWith("{")) {
                    val json = JSONObject(jsonStr)
                    val msgType = json.optString("type", "unknown")
                    val state = json.optString("state", "")

                    // Create a short summary for the message
                    val summary = if (isSent) {
                        "Sent $msgType"
                    } else {
                        when (state) {
                            MessageStates.ERROR -> "Error in $msgType"
                            MessageStates.STREAM -> "Stream $msgType"
                            else -> "Recv $msgType"
                        }
                    }

                    val finalType = if (!isSent && state == MessageStates.ERROR) LogType.LOCAL_ERROR else type

                    SystemLogEntry(
                        message = summary,
                        stack = if (json.has("stack")) json.optString("stack") else null,
                        type = finalType,
                        raw = jsonStr,
                        isStream = !isSent && state == MessageStates.STREAM
                    )
                } else {
                    SystemLogEntry(
                        message = if (isSent) "Sent Raw" else "Recv Raw",
                        type = type,
                        raw = jsonStr
                    )
                }
            } else if (rawMessage.startsWith("{")) {
                val json = JSONObject(rawMessage)
                val type = json.optString("type")

                if (type == "interconnect") {
                    SystemLogEntry(
                        message = json.optString("msg").ifEmpty { json.optString("message", "Unknown Error") },
                        stack = if (json.has("stack")) json.optString("stack") else null,
                        type = LogType.INTERCONNECT,
                        raw = rawMessage
                    )
                } else if (json.optString("state") == MessageStates.ERROR) {
                    val errorType = when (type) {
                        "luashell" -> LogType.LUA_ERROR
                        "qjs" -> LogType.JS_ERROR
                        else -> LogType.SYSTEM
                    }
                    SystemLogEntry(
                        message = json.optString("msg").ifEmpty { json.optString("message", "Unknown Error") },
                        stack = if (json.has("stack")) json.optString("stack") else null,
                        type = errorType,
                        raw = rawMessage
                    )
                } else {
                    SystemLogEntry(
                        message = "JSON Message ($type)",
                        type = LogType.SYSTEM,
                        raw = rawMessage
                    )
                }
            } else if (rawMessage.startsWith("LOCAL ERROR:")) {
                SystemLogEntry(
                    message = rawMessage.removePrefix("LOCAL ERROR:").trim(),
                    type = LogType.LOCAL_ERROR,
                    raw = rawMessage
                )
            } else if (rawMessage.startsWith("SYSTEM:")) {
                SystemLogEntry(
                    message = rawMessage.removePrefix("SYSTEM:").trim(),
                    type = LogType.SYSTEM,
                    raw = rawMessage
                )
            } else {
                SystemLogEntry(
                    message = rawMessage,
                    type = LogType.UNKNOWN,
                    raw = rawMessage
                )
            }
        } catch (e: Exception) {
            SystemLogEntry(
                message = "Failed to parse log",
                type = LogType.LOCAL_ERROR,
                raw = rawMessage
            )
        }

        _systemLogEntries.value = (_systemLogEntries.value + entry).takeLast(500)
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
