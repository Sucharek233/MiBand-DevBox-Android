package com.sucharek.miband_interconnect_test.ui.screens.activities.device

data class DeviceInfo(
    val model: String = "-",
    val brand: String = "-",
    val manufacturer: String = "-",
    val productName: String = "-",
    val osName: String = "-",
    val osVersionName: String = "-",
    val osVersionCode: String = "-",
    val platformVersionName: String = "-",
    val platformVersionCode: String = "-",
    val language: String = "-",
    val region: String = "-",
    val screenWidth: Int = 0,
    val screenHeight: Int = 0,
    val pixelRatio: Float = 0f,
    val statusBarHeight: Int = 0,
    val screenShape: String = "-",
    val deviceId: String = "-",
    val serial: String = "-",
    val storage: StorageOverview? = null
)

data class StorageOverview(
    val total: Long = 0,
    val available: Long = 0,
    val used: Long = 0
)
