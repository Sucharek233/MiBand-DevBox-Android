package com.sucharek.devbox.ui.screens.activities.terminal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharek.devbox.ui.screens.maindashboard.WatchViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.json.JSONObject

enum class LineType {
    INPUT,   // User-entered command (ap> command)
    OUTPUT,  // Normal stdout from the watch
    ERROR,   // Execution failure / Non-zero exit code
    SYSTEM   // Local client info (e.g., cleared screen, connection notes)
}

data class TerminalLine(
    val text: String,
    val type: LineType
)

class TerminalViewModel(
    private val globalWatchViewModel: WatchViewModel
) : ViewModel() {

    private val _terminalLogs = MutableStateFlow<List<TerminalLine>>(emptyList())
    val terminalLogs: StateFlow<List<TerminalLine>> = _terminalLogs.asStateFlow()

    private val commandHistory = mutableListOf<String>()
    private var historyIndex = -1

    init {
        viewModelScope.launch {
            globalWatchViewModel.terminalMessages.collectLatest { payload ->
                try {
                    val result = JSONObject(payload)
                    val exitCode = result.optInt("code", 0)
                    val output = result.optString("res", payload)

                    if (output.isNotEmpty()) {
                        // Standard output receives normal formatting, regardless of text contents
                        appendLine(output, LineType.OUTPUT)
                    } else {
                        appendLine("Empty response", LineType.SYSTEM)
                    }

                    // Only paint lines red if the exit code explicitly signals a process failure
                    if (exitCode != 0) {
                        appendLine("Command exited with code: $exitCode", LineType.ERROR)
                    }
                } catch (e: Exception) {
                    appendLine(payload, LineType.OUTPUT)
                }
            }
        }
    }

    fun executeCommand(command: String) {
        if (command.isBlank()) return

        if (commandHistory.lastOrNull() != command) {
            commandHistory.add(command)
        }
        historyIndex = commandHistory.size

        // Explicitly mark typed command line
        appendLine("ap> $command", LineType.INPUT)

        if (command.trim().lowercase() == "clear") {
            _terminalLogs.value = emptyList()
            return
        }

        val terminalArgs = JSONObject().apply {
            put("cmd", command)
        }

        globalWatchViewModel.sendStructuredMessage(
            type = "cmd",
            args = terminalArgs
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

    fun clearScreen() {
        _terminalLogs.value = emptyList()
    }

    private fun appendLine(text: String, type: LineType) {
        _terminalLogs.value = _terminalLogs.value + TerminalLine(text, type)
    }
}