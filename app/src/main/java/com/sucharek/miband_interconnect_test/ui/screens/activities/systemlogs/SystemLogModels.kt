package com.sucharek.miband_interconnect_test.ui.screens.activities.systemlogs

import java.util.Date

enum class LogType {
    SYSTEM,
    INTERCONNECT,
    LUA_ERROR,
    JS_ERROR,
    LOCAL_ERROR,
    UNKNOWN
}

data class SystemLogEntry(
    val message: String,
    val stack: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val type: LogType = LogType.UNKNOWN,
    val raw: String? = null
)
