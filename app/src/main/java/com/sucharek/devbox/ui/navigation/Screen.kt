package com.sucharek.devbox.ui.navigation

import kotlinx.serialization.Serializable

sealed interface Screen {
    @Serializable
    object DeviceSelection : Screen

    @Serializable
    object MainDashboard : Screen

    // Activities
    @Serializable
    object RemoteTerminal : Screen

    @Serializable
    object QjsShell : Screen

    @Serializable
    object DeviceInfo : Screen

    @Serializable
    object LuaShell : Screen

    @Serializable
    data class FileExplorer(val initialPath: String = "/") : Screen

    @Serializable
    object ModuleCompatibility : Screen

    @Serializable
    object Sensors : Screen

    @Serializable
    object SystemLogs : Screen

    @Serializable
    object Apps : Screen

    @Serializable
    data class AppDetail(val packageName: String, val appName: String) : Screen

    @Serializable
    data class AppManifest(val packageName: String) : Screen

    @Serializable
    object LuaSensors : Screen

    @Serializable
    object LuaSysInfo : Screen

    @Serializable
    data class LuaSensorChart(val sensorName: String) : Screen

    @Serializable
    data class SensorChart(val sensorName: String) : Screen

    @Serializable
    data class Ping(val initialType: String? = null) : Screen

    @Serializable
    object WebSocketApi : Screen
}
