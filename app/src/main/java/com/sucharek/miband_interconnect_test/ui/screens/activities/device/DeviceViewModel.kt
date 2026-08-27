package com.sucharek.miband_interconnect_test.ui.screens.activities.device

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharek.miband_interconnect_test.ui.screens.maindashboard.WatchViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.json.JSONObject

class DeviceViewModel(
    private val globalWatchViewModel: WatchViewModel
) : ViewModel() {

    private val _deviceInfo = MutableStateFlow<DeviceInfo?>(null)
    val deviceInfo: StateFlow<DeviceInfo?> = _deviceInfo.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        viewModelScope.launch {
            globalWatchViewModel.sysinfoMessages.collectLatest { payload ->
                handleResponse(payload)
            }
        }

        viewModelScope.launch {
            globalWatchViewModel.mailboxBusyEvents.collectLatest {
                _isLoading.value = false
            }
        }

        refresh()
    }

    fun refresh() {
        _isLoading.value = true
        globalWatchViewModel.sendStructuredMessage(type = "sysinfo")
    }

    private fun handleResponse(payload: String) {
        try {
            val json = JSONObject(payload)
            val res = json.getJSONObject("res")
            
            val main = res.optJSONObject("main") ?: JSONObject()
            val storage = res.optJSONObject("storage")
            
            val info = DeviceInfo(
                model = main.optString("model", "-"),
                brand = main.optString("brand", "-"),
                manufacturer = main.optString("manufacturer", "-"),
                productName = main.optString("productName", "-"),
                osName = main.optString("osName", "-"),
                osVersionName = main.optString("osVersionName", "-"),
                osVersionCode = main.optString("osVersionCode", "-"),
                platformVersionName = main.optString("platformVersionName", "-"),
                platformVersionCode = main.optString("platformVersionCode", "-"),
                language = main.optString("language", "-"),
                region = main.optString("region", "-"),
                screenWidth = main.optInt("screenWidth", 0),
                screenHeight = main.optInt("screenHeight", 0),
                pixelRatio = main.optDouble("pixelRatio", 0.0).toFloat(),
                statusBarHeight = main.optInt("statusBarHeight", 0),
                screenShape = main.optString("screenShape", "-"),
                deviceId = res.optString("deviceId", "-"),
                serial = res.optString("serial", "-"),
                storage = storage?.let {
                    StorageOverview(
                        total = it.optLong("total", 0),
                        available = it.optLong("available", 0),
                        used = it.optLong("used", 0)
                    )
                }
            )
            
            _deviceInfo.value = info
        } catch (e: Exception) {
            // Error handled by loading state reset or system logs
        } finally {
            _isLoading.value = false
        }
    }
}
