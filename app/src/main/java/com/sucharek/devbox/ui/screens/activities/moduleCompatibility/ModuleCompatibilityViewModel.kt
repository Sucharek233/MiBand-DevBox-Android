package com.sucharek.devbox.ui.screens.activities.moduleCompatibility

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharek.devbox.ui.screens.maindashboard.WatchViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class ModuleCompatibilityViewModel(
    private val globalWatchViewModel: WatchViewModel
) : ViewModel() {

    private val defaultModules = listOf(
        // Service
        "service.account",
        "service.alipay",
        "service.exchange",
        "service.health",
        "service.pay",
        "service.push",
        "service.qqaccount",
        "service.share",
        "service.stats",
        "service.wbaccount",
        "service.wxaccount",
        "service.wxpay",
        "service.texttoaudio",
        "service.ad",
        // System
        "system.alarm",
        "system.app",
        "system.audio",
        "system.barcode",
        "system.battery",
        "system.bluetooth",
        "system.brightness",
        "system.calendar",
        "system.cipher",
        "system.clipboard",
        "system.contact",
        "system.crypto",
        "system.configuration",
        "system.device",
        "system.fetch",
        "system.file",
        "system.geolocation",
        "system.image",
        "system.interconnect",
        "system.media",
        "system.network",
        "system.notification",
        "system.package",
        "system.prompt",
        "system.record",
        "system.request",
        "system.resident",
        "system.router",
        "system.sensor",
        "system.share",
        "system.shortcut",
        "system.sms",
        "system.storage",
        "system.vibrator",
        "system.volume",
        "system.websocketfactory",
        "system.webview",
        "system.wifi",
        "system.zip",
        "system.configuration",
        "system.telecom",
        "system.keyguard",
        "system.downloadtask",
        "system.uploadtask",
        "system.requesttask",
        "system.nfc",
        "system.screenshot",
        // Hap
        "hap.video"
    )

    private val _modules = MutableStateFlow(
        defaultModules.map { ModuleItem(name = it) }
    )
    val modules: StateFlow<List<ModuleItem>> = _modules.asStateFlow()

    private val _scrollToModule = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    val scrollToModule: SharedFlow<Int> = _scrollToModule.asSharedFlow()

    private var pendingFuncsModule: String? = null

    init {
        viewModelScope.launch {
            globalWatchViewModel.modulesMessages.collectLatest { payload ->
                handleResponse(payload)
            }
        }

        viewModelScope.launch {
            globalWatchViewModel.mailboxBusyEvents.collectLatest {
                _modules.value = _modules.value.map {
                    if (it.status == CompatStatus.CHECKING) it.copy(status = CompatStatus.UNKNOWN) else it
                }
            }
        }
    }

    private fun handleResponse(payload: String) {
        when (val response = ModuleResponse.parse(payload, pendingFuncsModule)) {
            is ModuleResponse.CompatResult -> {
                _modules.value = _modules.value.map { item ->
                    if (response.results.containsKey(item.name)) {
                        val isSupported = response.results[item.name] == true
                        item.copy(
                            status = if (isSupported) CompatStatus.SUPPORTED else CompatStatus.UNSUPPORTED
                        )
                    } else item
                }
            }

            is ModuleResponse.FuncsResult -> {
                _modules.value = _modules.value.map { item ->
                    if (item.name == response.moduleName) {
                        item.copy(
                            functionsResult = response.functions,
                            isLoadingFuncs = false,
                            isExpanded = true // Auto expand when functions return
                        )
                    } else item
                }
                pendingFuncsModule = null
            }

            is ModuleResponse.Error -> {
                pendingFuncsModule?.let { name ->
                    _modules.value = _modules.value.map {
                        if (it.name == name) it.copy(isLoadingFuncs = false) else it
                    }
                }
                pendingFuncsModule = null
            }
        }
    }

    fun toggleSelection(moduleName: String) {
        _modules.value = _modules.value.map {
            if (it.name == moduleName) it.copy(isSelected = !it.isSelected) else it
        }
    }


    fun addCustomModule(moduleName: String) {
        val trimmed = moduleName.trim()
        val existingIndex = _modules.value.indexOfFirst { it.name == trimmed }
        
        if (existingIndex != -1) {
            viewModelScope.launch { _scrollToModule.emit(existingIndex) }
            return
        }

        val newItem = ModuleItem(name = trimmed, isSelected = true)
        _modules.value += newItem
        val newIndex = _modules.value.size - 1
        
        viewModelScope.launch {
            _scrollToModule.emit(newIndex)
        }
        
        testCompatibility(listOf(trimmed))
    }

    fun testCompatibility(moduleNames: List<String>? = null) {
        val names = moduleNames ?: _modules.value.filter { it.isSelected }.map { it.name }
        if (names.isEmpty()) return

        _modules.value = _modules.value.map {
            if (it.name in names) it.copy(status = CompatStatus.CHECKING) else it
        }

        val jsonArgs = JSONObject().apply {
            put("type", "compat")
            val arr = JSONArray()
            names.forEach { arr.put(it) }
            put("modules", arr)
        }

        globalWatchViewModel.sendStructuredMessage(type = "modules", args = jsonArgs)
    }

    fun fetchFunctions(moduleName: String) {
        pendingFuncsModule = moduleName

        _modules.value = _modules.value.map {
            if (it.name == moduleName) it.copy(isLoadingFuncs = true) else it
        }

        val jsonArgs = JSONObject().apply {
            put("type", "funcs")
            put("module", moduleName)
        }

        globalWatchViewModel.sendStructuredMessage(type = "modules", args = jsonArgs)
    }
}