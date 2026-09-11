package com.sucharek.devbox.ui.screens.activities.qjsshell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharek.devbox.ui.screens.maindashboard.WatchViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.json.JSONObject

class QjsShellViewModel(
    private val globalWatchViewModel: WatchViewModel
) : ViewModel() {

    private val _entries = MutableStateFlow<List<ConsoleEntry>>(emptyList())
    val entries: StateFlow<List<ConsoleEntry>> = _entries.asStateFlow()

    private val commandHistory = mutableListOf<String>()
    private var historyIndex = -1

    init {
        viewModelScope.launch {
            globalWatchViewModel.qjsMessages.collectLatest { payload ->
                val newEntries = ConsoleEntry.parsePayload(payload)
                _entries.value = _entries.value + newEntries
            }
        }
    }

    fun evaluateJsCode(codeSnippet: String) {
        if (codeSnippet.isBlank()) return

        if (commandHistory.lastOrNull() != codeSnippet) {
            commandHistory.add(codeSnippet)
        }
        historyIndex = commandHistory.size

        // Add input prompt entry
        _entries.value = _entries.value + ConsoleEntry.Input(codeSnippet)

        val jsArgs = JSONObject().apply {
            put("code", codeSnippet)
        }

        globalWatchViewModel.sendStructuredMessage(
            type = "qjs",
            args = jsArgs
        )
    }

    fun getPreviousCommand(): String? {
        if (commandHistory.isEmpty()) return null
        if (historyIndex > 0) historyIndex--
        return commandHistory.getOrNull(historyIndex)
    }

    fun getNextCommand(): String? {
        if (commandHistory.isEmpty()) return null
        if (historyIndex < commandHistory.size - 1) {
            historyIndex++
            return commandHistory[historyIndex]
        } else {
            historyIndex = commandHistory.size
            return ""
        }
    }

    fun clearConsole() {
        _entries.value = emptyList()
    }
}