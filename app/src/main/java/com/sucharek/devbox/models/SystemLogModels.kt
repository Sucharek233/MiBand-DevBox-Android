package com.sucharek.devbox.models

enum class LogType {
    SYSTEM,
    INTERCONNECT,
    ERROR,
    TIMEOUT,
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
