package com.sucharek.miband_interconnect_test.ui.screens.maindashboard

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Devices
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
    val serviceType: ServiceType?, 
    val icon: ImageVector
) {
    // Shared / Management
    PING("Ping", "Test connection latency", null, Icons.Default.Speed),
    SYSTEMLOGS("System Logs", "View global errors and events", null, Icons.Default.BugReport),

    // Lua Service
    TERMINAL("Terminal", "Send custom commands", ServiceType.LUA, Icons.Default.Terminal),
    LUASHELL("Lua Shell", "Run custom Lua code", ServiceType.LUA, Icons.Default.Code),
    FILES("File Explorer", "Explore the filesystem", ServiceType.LUA, Icons.Default.Folder),
    LUASENSORS("Lua Sensors", "Stream sensor data", ServiceType.LUA, Icons.Default.Sensors),
    APPS("Apps", "View installed apps", ServiceType.LUA, Icons.Default.Apps),

    // QuickJS Service
    QJS("VelaJS Shell", "Run custom Javascript code", ServiceType.QUICKJS, Icons.Default.Code),
    MODULES("Modules", "Check module compatibility", ServiceType.QUICKJS, Icons.Default.Extension),
    DEVICE("Device Info", "View hardware and software details", ServiceType.QUICKJS, Icons.Default.Devices),
    SENSORS("Sensors", "Stream sensor data", ServiceType.QUICKJS, Icons.Default.Sensors)
}
