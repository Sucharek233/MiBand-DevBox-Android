package com.sucharek.devbox.ui.screens.activities.luasysinfo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharek.devbox.ui.screens.maindashboard.WatchViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import com.sucharek.devbox.models.MessageStates

data class PropItem(val key: String, val value: String)
data class DiskPartition(
    val filesystem: String,
    val size: String,
    val used: String,
    val available: String,
    val mountPoint: String
)

class LuaSysInfoViewModel(
    private val globalWatchViewModel: WatchViewModel
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // System Info Fields
    private val _cpuInfo = MutableStateFlow<String?>(null)
    val cpuInfo = _cpuInfo.asStateFlow()
    private val _cpuLoad = MutableStateFlow<String?>(null)
    val cpuLoad = _cpuLoad.asStateFlow()
    private val _memInfo = MutableStateFlow<String?>(null)
    val memInfo = _memInfo.asStateFlow()
    private val _memPool = MutableStateFlow<String?>(null)
    val memPool = _memPool.asStateFlow()
    private val _tcbInfo = MutableStateFlow<String?>(null)
    val tcbInfo = _tcbInfo.asStateFlow()
    private val _version = MutableStateFlow<String?>(null)
    val version = _version.asStateFlow()
    private val _partitions = MutableStateFlow<String?>(null)
    val partitions = _partitions.asStateFlow()

    // Disk Info Fields
    private val _partitionsList = MutableStateFlow<List<DiskPartition>>(emptyList())
    val partitionsList = _partitionsList.asStateFlow()

    private val _blocksInfo = MutableStateFlow<String?>(null)
    val blocksInfo = _blocksInfo.asStateFlow()

    // Props
    private val _propsList = MutableStateFlow<List<PropItem>>(emptyList())
    val propsList: StateFlow<List<PropItem>> = _propsList.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val requestQueue = mutableListOf<String>()

    init {
        viewModelScope.launch {
            globalWatchViewModel.luaSysInfoMessages.collectLatest { payload ->
                handleResponse(payload)
            }
        }

        viewModelScope.launch {
            globalWatchViewModel.mailboxBusyEvents.collectLatest {
                _isLoading.value = false
                requestQueue.clear()
            }
        }
        
        refreshAll()
    }

    private fun handleResponse(payload: String) {
        try {
            val json = JSONObject(payload)
            val appState = json.optString("appState")
            val res = json.opt("res")

            if (appState == MessageStates.ERROR || json.optString("state") == MessageStates.ERROR) {
                _error.value = res?.toString() ?: json.optString("msg", "Unknown error")
                _isLoading.value = false
                requestQueue.clear()
                return
            }

            if (res is JSONObject) {
                // Disk Info
                if (res.has("usage")) {
                    val usage = res.optString("usage")
                    _partitionsList.value = parseDiskUsage(usage)
                }
                if (res.has("blocks")) _blocksInfo.value = res.optString("blocks")

                // System Info
                if (res.has("cpuInfo")) _cpuInfo.value = res.optString("cpuInfo")
                if (res.has("cpuLoad")) _cpuLoad.value = res.optString("cpuLoad")
                if (res.has("memInfo")) _memInfo.value = res.optString("memInfo")
                if (res.has("memPool")) _memPool.value = res.optString("memPool")
                if (res.has("tcbInfo")) _tcbInfo.value = res.optString("tcbInfo")
                if (res.has("version")) _version.value = res.optString("version")
                if (res.has("partitions")) _partitions.value = res.optString("partitions")

            } else if (res is String) {
                // Props parsing
                val lines = res.split("\n")
                val parsed = lines.mapNotNull { line ->
                    val parts = line.split(":", limit = 2)
                    if (parts.size == 2) {
                        PropItem(parts[0].trim(), parts[1].trim())
                    } else null
                }.sortedBy { it.key }
                _propsList.value = parsed
            }

            if (_isLoading.value && requestQueue.isNotEmpty()) {
                val next = requestQueue.removeAt(0)
                sendSingleRequest(next)
            } else {
                _isLoading.value = false
            }

        } catch (e: Exception) {
            _error.value = "Parse error: ${e.message}"
            _isLoading.value = false
            requestQueue.clear()
        }
    }

    private fun parseDiskUsage(raw: String): List<DiskPartition> {
        val lines = raw.split("\n").filter { it.isNotBlank() }
        if (lines.size < 2) return emptyList()
        
        return lines.drop(1).mapNotNull { line ->
            val parts = line.trim().split(Regex("\\s+"))
            if (parts.size >= 5) {
                DiskPartition(
                    filesystem = parts[0],
                    size = parts[1],
                    used = parts[2],
                    available = parts[3],
                    mountPoint = parts[4]
                )
            } else null
        }
    }

    fun refreshAll() {
        if (_isLoading.value) return
        _isLoading.value = true
        _error.value = null
        requestQueue.clear()
        requestQueue.add("disk")
        requestQueue.add("props")
        sendSingleRequest("info")
    }

    fun setProp(key: String, value: String) {
        _isLoading.value = true
        val args = JSONObject().apply {
            put("type", "setProp")
            put("prop", key)
            put("value", value)
        }
        // We'll queue a refresh of props after setting
        requestQueue.add("props")
        globalWatchViewModel.sendStructuredMessage("sysInfoLua", args)
    }

    private fun sendSingleRequest(type: String) {
        val args = JSONObject().apply {
            put("type", type)
        }
        globalWatchViewModel.sendStructuredMessage("sysInfoLua", args)
    }
}
