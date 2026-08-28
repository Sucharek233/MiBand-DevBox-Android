package com.sucharek.miband_interconnect_test.models

enum class LogType {
    SYSTEM,
    INTERCONNECT,
    LUA_ERROR,
    JS_ERROR,
    LOCAL_ERROR,
    SENT,
    RECV,
    UNKNOWN
}

data class SystemLogEntry(
    val message: String,
    val stack: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val type: LogType = LogType.UNKNOWN,
    val raw: String? = null,
    val isStream: Boolean = false
)
