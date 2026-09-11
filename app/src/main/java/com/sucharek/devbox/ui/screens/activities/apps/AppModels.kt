package com.sucharek.devbox.ui.screens.activities.apps

data class AppItem(
    val packageName: String,
    val name: String,
    val iconBase64: String? = null,
    val isIconLoading: Boolean = false
)

data class AppDetails(
    val packageName: String = "",
    val name: String = "",
    val versionCode: Int = 0,
    val versionName: String = "-",
    val minAPILevel: Int = 0,
    val minPlatformVersion: Int = 0,
    val fingerprint: String = "-",
    val installedTimestamp: Long = 0,
    val iconPath: String = "-",
    val needNetwork: Boolean = false,
    val background: Boolean = false,
    val standalone: Boolean = false,
    val iconBase64: String? = null,
    val isIconLoading: Boolean = false,
    val rawJson: String? = null
)
