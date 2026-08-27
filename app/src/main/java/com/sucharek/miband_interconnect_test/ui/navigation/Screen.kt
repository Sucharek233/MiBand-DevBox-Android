package com.sucharek.miband_interconnect_test.ui.navigation

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
    object LuaSensors : Screen

    @Serializable
    data class LuaSensorChart(val sensorName: String) : Screen

    @Serializable
    data class SensorChart(val sensorName: String) : Screen

    @Serializable
    object Ping : Screen
}