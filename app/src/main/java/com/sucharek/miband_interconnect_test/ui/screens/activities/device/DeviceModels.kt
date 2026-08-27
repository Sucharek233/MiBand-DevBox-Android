package com.sucharek.miband_interconnect_test.ui.screens.activities.device

data class DeviceInfo(
    val model: String = "-",
    val brand: String = "-",
    val manufacturer: String = "-",
    val product: String = "-",
    val osType: String = "-",
    val osVersionName: String = "-",
    val osVersionCode: String = "-",
    val platformVersionName: String = "-",
    val platformVersionCode: String = "-",
    val apiLevel: String = "-",
    val language: String = "-",
    val region: String = "-",
    val screenWidth: Int = 0,
    val screenHeight: Int = 0,
    val screenDensity: Float = 0f,
    val screenShape: String = "-",
    val deviceType: String = "-",
    val deviceId: String = "-",
    val imei: String = "-",
    val serial: String = "-",
    val storage: StorageOverview? = null,
    val other: Map<String, String> = emptyMap()
)

data class StorageOverview(
    val total: Long = 0,
    val available: Long = 0,
    val used: Long = 0
)
