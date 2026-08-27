package com.sucharek.miband_interconnect_test.ui.screens.activities.systemlogs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharek.miband_interconnect_test.ui.screens.maindashboard.WatchViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.json.JSONObject

class SystemLogsViewModel(
    private val globalWatchViewModel: WatchViewModel
) : ViewModel() {

    private val _logs = MutableStateFlow<List<SystemLogEntry>>(emptyList())
    val logs: StateFlow<List<SystemLogEntry>> = _logs.asStateFlow()

    init {
        viewModelScope.launch {
            globalWatchViewModel.systemMessages.collectLatest { rawMessage ->
                parseAndAddLog(rawMessage)
            }
        }
    }

    private fun parseAndAddLog(rawMessage: String) {
        val entry = try {
            if (rawMessage.startsWith("{")) {
                val json = JSONObject(rawMessage)
                val type = json.optString("type")
                
                if (type == "interconnect") {
                    SystemLogEntry(
                        message = json.optString("msg").ifEmpty { json.optString("message", "Unknown Error") },
                        stack = if (json.has("stack")) json.optString("stack") else null,
                        type = LogType.INTERCONNECT,
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

        _logs.value = _logs.value + entry
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }
}
