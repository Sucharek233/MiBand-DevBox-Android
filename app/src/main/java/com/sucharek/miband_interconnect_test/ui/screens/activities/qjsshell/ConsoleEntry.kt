package com.sucharek.miband_interconnect_test.ui.screens.activities.qjsshell

import org.json.JSONArray
import org.json.JSONObject

sealed class ConsoleEntry {
    data class Input(val code: String) : ConsoleEntry()

    data class Output(
        val rawResult: Any?
    ) : ConsoleEntry()

    data class Error(
        val message: String,
        val stack: String? = null
    ) : ConsoleEntry()

    companion object {
        fun parsePayload(payloadString: String): ConsoleEntry {
            return try {
                val outer = JSONObject(payloadString)
                val state = outer.optString("state", "")

                if (state == "error") {
                    val msg = outer.optString("msg", "Unknown error")
                    val stack = outer.optString("stack", null)
                    Error(message = msg, stack = stack)
                } else {
                    val resString = outer.optString("res", null)
                    if (resString != null) {
                        Output(rawResult = parseJsResult(resString))
                    } else {
                        Output(rawResult = outer)
                    }
                }
            } catch (e: Exception) {
                Output(rawResult = payloadString)
            }
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