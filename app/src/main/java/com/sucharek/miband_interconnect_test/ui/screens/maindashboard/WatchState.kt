package com.sucharek.miband_interconnect_test.ui.screens.maindashboard

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
    val serviceType: ServiceType
) {
    // Lua Service
    PING("Ping", "Test connection latency", ServiceType.LUA),
    TERMINAL("Terminal", "Send custom commands", ServiceType.LUA),
    LUASHELL("Lua Shell", "Run custom Lua code", ServiceType.LUA),
    FILES("File Explorer", "Explore the filesystem", ServiceType.LUA),
    
    // QuickJS Service
    QJS("VelaJS Shell", "Run custom Javascript code", ServiceType.QUICKJS),
    MODULES("Modules", "Check module compatibility", ServiceType.QUICKJS),
    SENSORS("Sensors", "Stream sensor data", ServiceType.QUICKJS)
}
