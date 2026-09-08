package com.sucharek.miband_interconnect_test.ui.screens.activities.qjsshell

import com.sucharek.miband_interconnect_test.models.MessageStates
import org.json.JSONArray
import org.json.JSONObject

sealed class ConsoleEntry {
    data class Input(val code: String) : ConsoleEntry()

    data class Output(
        val rawResult: Any?
    ) : ConsoleEntry()

    data class Log(
        val rawResult: Any?
    ) : ConsoleEntry()

    data class Error(
        val message: String,
        val stack: String? = null
    ) : ConsoleEntry()

    companion object {
        fun parsePayload(payloadString: String): List<ConsoleEntry> {
            val entries = mutableListOf<ConsoleEntry>()
            try {
                val outer = JSONObject(payloadString)
                val state = outer.optString("state", "")

                // 1. Check for logs
                val logsArray = outer.optJSONArray("logs")
                if (logsArray != null) {
                    for (i in 0 until logsArray.length()) {
                        val logStr = logsArray.optString(i, null)
                        if (logStr != null) {
                            entries.add(Log(rawResult = parseJsResult(logStr)))
                        }
                    }
                }

                // 2. Check for error or result
                if (state == MessageStates.ERROR) {
                    val msg = outer.optString("msg", "Unknown error")
                    val stack = outer.optString("stack", null)
                    entries.add(Error(message = msg, stack = stack))
                } else {
                    val resString = outer.optString("res", null)
                    if (resString != null) {
                        entries.add(Output(rawResult = parseJsResult(resString)))
                    } else if (!outer.has("logs")) {
                        // If no res and no logs, just output the whole object (unless it's just a log container)
                        entries.add(Output(rawResult = outer))
                    }
                }
            } catch (e: Exception) {
                entries.add(Output(rawResult = payloadString))
            }
            return entries
        }

        private fun parseJsResult(res: String): Any? {
            val trimmed = res.trim()

            // 1. Double-quoted string from JSON.stringify (e.g. "\"hi\"")
            if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length >= 2) {
                // Unwrap outer quotes and unescape
                return trimmed.substring(1, trimmed.length - 1)
                    .replace("\\\"", "\"")
                    .replace("\\n", "\n")
            }

            // 2. Objects or Arrays
            if (trimmed.startsWith("{")) {
                return try { JSONObject(trimmed) } catch (e: Exception) { trimmed }
            }
            if (trimmed.startsWith("[")) {
                return try { JSONArray(trimmed) } catch (e: Exception) { trimmed }
            }

            // 3. Booleans
            if (trimmed == "true") return true
            if (trimmed == "false") return false

            // 4. Numbers (e.g. 2.2, 100, -5)
            val doubleVal = trimmed.toDoubleOrNull()
            if (doubleVal != null) return doubleVal

            // Fallback
            return trimmed
        }
    }
}