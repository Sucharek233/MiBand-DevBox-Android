package com.sucharek.devbox.ui.screens.activities.luashell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharek.devbox.ui.screens.activities.qjsshell.ConsoleEntry
import com.sucharek.devbox.ui.screens.maindashboard.WatchViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.json.JSONObject
import com.sucharek.devbox.models.MessageStates

class LuaShellViewModel(
    private val globalWatchViewModel: WatchViewModel
) : ViewModel() {

    private val _entries = MutableStateFlow<List<ConsoleEntry>>(emptyList())
    val entries: StateFlow<List<ConsoleEntry>> = _entries.asStateFlow()

    private val commandHistory = mutableListOf<String>()
    private var historyIndex = -1

    init {
        viewModelScope.launch {
            globalWatchViewModel.luaShellMessages.collectLatest { payload ->
                val entriesToAdd = mutableListOf<ConsoleEntry>()
                
                try {
                    val outer = JSONObject(payload)
                    val state = outer.optString("state", "")
                    
                    // 1. Handle Print Output first if present
                    val printOutput = outer.optString("print", "")
                    val hasPrints = printOutput.isNotEmpty() && printOutput != "null"
                    if (hasPrints) {
                        entriesToAdd.add(ConsoleEntry.Output(printOutput))
                    }

                    // 2. Handle Result or Error
                    if (state == MessageStates.ERROR) {
                        val msg = outer.optString("msg", "Unknown error")
                        val stack = outer.optString("stack", null)
                        entriesToAdd.add(ConsoleEntry.Error(message = msg, stack = stack))
                    } else if (state == MessageStates.DONE) {
                        val res = outer.opt("res")
                        val hasRes = res != null && res != JSONObject.NULL
                        if (hasRes) {
                            entriesToAdd.add(ConsoleEntry.Output(res))
                        }
                        
                        // If nothing was output and nothing was returned, show 'nil'
                        if (!hasPrints && !hasRes) {
                            val nilObj = JSONObject().apply { put("$", "nil") }
                            entriesToAdd.add(ConsoleEntry.Output(nilObj))
                        }
                    }
                } catch (e: Exception) {
                    entriesToAdd.add(ConsoleEntry.Output(payload))
                }

                _entries.value = _entries.value + entriesToAdd
            }
        }
    }

    fun executeLuaCode(codeSnippet: String) {
        if (codeSnippet.isBlank()) return

        if (commandHistory.lastOrNull() != codeSnippet) {
            commandHistory.add(codeSnippet)
        }
        historyIndex = commandHistory.size

        // Add input prompt entry
        _entries.value = _entries.value + ConsoleEntry.Input(codeSnippet)

        val luaArgs = JSONObject().apply {
            put("code", codeSnippet)
        }

        globalWatchViewModel.sendStructuredMessage(
            type = "luashell",
            args = luaArgs
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
