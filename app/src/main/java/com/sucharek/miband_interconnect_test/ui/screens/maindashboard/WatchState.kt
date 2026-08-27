package com.sucharek.miband_interconnect_test.ui.screens.maindashboard

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.ui.graphics.vector.ImageVector

sealed interface WatchConnectionState {
    object Disconnected : WatchConnectionState
    data class Connected(val nodeName: String) : WatchConnectionState
    data class Error(val message: String) : WatchConnectionState
}

enum class ServiceType(val title: String) {
    LUA("Lua Service"),
    QUICKJS("QuickJS Service")
}

enum class DashboardCategory(
    val title: String, 
    val description: String, 
    val serviceType: ServiceType,
    val icon: ImageVector
) {
    // Lua Service
    PING("Ping", "Test connection latency", ServiceType.LUA, Icons.Default.Speed),
    TERMINAL("Terminal", "Send custom commands", ServiceType.LUA, Icons.Default.Terminal),
    LUASHELL("Lua Shell", "Run custom Lua code", ServiceType.LUA, Icons.Default.Code),
    FILES("File Explorer", "Explore the filesystem", ServiceType.LUA, Icons.Default.Folder),
    LUASENSORS("Lua Sensors", "Stream sensor data (Lua)", ServiceType.LUA, Icons.Default.Sensors),
    SYSTEMLOGS("System Logs", "View global errors and events", ServiceType.LUA, Icons.Default.BugReport),
    
    // QuickJS Service
    QJS("VelaJS Shell", "Run custom Javascript code", ServiceType.QUICKJS, Icons.Default.Code),
    MODULES("Modules", "Check module compatibility", ServiceType.QUICKJS, Icons.Default.Extension),
    SENSORS("Sensors", "Stream sensor data", ServiceType.QUICKJS, Icons.Default.Sensors)
}
