package com.sucharek.miband_interconnect_test.ui.screens.activities.luaSensors

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharek.miband_interconnect_test.ui.screens.activities.sensors.BaseSensorViewModel
import com.sucharek.miband_interconnect_test.ui.screens.activities.sensors.SensorAvailability
import com.sucharek.miband_interconnect_test.ui.screens.activities.sensors.SensorSample
import com.sucharek.miband_interconnect_test.ui.screens.activities.sensors.SubscriptionState
import com.sucharek.miband_interconnect_test.ui.screens.maindashboard.WatchViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import com.sucharek.miband_interconnect_test.models.MessageStates

class LuaSensorViewModel(
    private val globalWatchViewModel: WatchViewModel
) : ViewModel(), BaseSensorViewModel {

    // 1. Discovery State
    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    private val _predefinedSensors = MutableStateFlow<List<LuaSensorInfo>>(emptyList())
    val predefinedSensors: StateFlow<List<LuaSensorInfo>> = _predefinedSensors.asStateFlow()

    private val _allSensors = MutableStateFlow<List<LuaSensorInfo>>(emptyList())
    val allSensors: StateFlow<List<LuaSensorInfo>> = _allSensors.asStateFlow()

    // 2. Stream State
    private val _activeSensor = MutableStateFlow<LuaSensorInfo?>(null)
    val activeSensor: StateFlow<LuaSensorInfo?> = _activeSensor.asStateFlow()

    private val _pendingSensor = MutableStateFlow<LuaSensorInfo?>(null)
    val pendingSensor: StateFlow<LuaSensorInfo?> = _pendingSensor.asStateFlow()

    private val _subscriptionState = MutableStateFlow(SubscriptionState.DISCONNECTED)
    override val subscriptionState: StateFlow<SubscriptionState> = _subscriptionState.asStateFlow()

    private val _lastError = MutableStateFlow<Pair<String, String?>?>(null)
    override val lastError: StateFlow<Pair<String, String?>?> = _lastError.asStateFlow()

    private val _incomingSamples = MutableStateFlow<List<SensorSample>>(emptyList())
    override val incomingSamples: StateFlow<List<SensorSample>> = _incomingSamples.asStateFlow()

    // Configuration Options
    private val _provider = MutableStateFlow(LuaSensorProvider.FILE)
    val provider: StateFlow<LuaSensorProvider> = _provider.asStateFlow()

    private val _useKnown = MutableStateFlow(true)
    val useKnown: StateFlow<Boolean> = _useKnown.asStateFlow()

    private val _period = MutableStateFlow(50)
    val period: StateFlow<Int> = _period.asStateFlow()

    // Linear slider value (0.0 to 1.0)
    private val _sliderValue = MutableStateFlow(0f)
    val sliderValue: StateFlow<Float> = _sliderValue.asStateFlow()

    // UI Persistence
    private val _selectedTabIndex = MutableStateFlow(0)
    val selectedTabIndex: StateFlow<Int> = _selectedTabIndex.asStateFlow()

    var predefinedScrollIndex = 0
    var predefinedScrollOffset = 0
    var allScrollIndex = 0
    var allScrollOffset = 0

    private var discoveryPhase = 0 // 0: Idle, 1: listPre, 2: list

    init {
        viewModelScope.launch(Dispatchers.Default) {
            globalWatchViewModel.luaSensorsMessages.collectLatest { payload ->
                handleIncomingMessage(payload)
            }
        }

        viewModelScope.launch {
            globalWatchViewModel.mailboxBusyEvents.collectLatest {
                _isDiscovering.value = false
                discoveryPhase = 0
                // Reset connection state if mailbox times out
                if (_subscriptionState.value == SubscriptionState.SUBSCRIBING || 
                    _subscriptionState.value == SubscriptionState.UNSUBSCRIBING) {
                    _subscriptionState.value = SubscriptionState.DISCONNECTED
                    _activeSensor.value = null
                }
            }
        }

        discoverSensors()
    }

    fun setProvider(newProvider: LuaSensorProvider) {
        _provider.value = newProvider
    }

    fun setUseKnown(value: Boolean) {
        _useKnown.value = value
    }

    fun setPeriod(value: Int) {
        _period.value = value
        // Update slider value to match
        _sliderValue.value = periodToSlider(value)
    }

    fun setSliderValue(value: Float) {
        _sliderValue.value = value
        _period.value = sliderToPeriod(value)
    }

    fun setSelectedTabIndex(index: Int) {
        _selectedTabIndex.value = index
    }

    private fun sliderToPeriod(value: Float): Int {
        // Period = 50 * 20^value
        val period = 50.0 * Math.pow(20.0, value.toDouble())
        return period.toInt().coerceIn(50, 1000)
    }

    private fun periodToSlider(period: Int): Float {
        // value = log20(period / 50) = ln(period / 50) / ln(20)
        val value = Math.log(period.toDouble() / 50.0) / Math.log(20.0)
        return value.toFloat().coerceIn(0f, 1f)
    }

    fun setPendingSensor(sensor: LuaSensorInfo?) {
        _pendingSensor.value = sensor
    }

    fun discoverSensors() {
        viewModelScope.launch {
            _isDiscovering.value = true
            discoveryPhase = 1
            
            // Fetch predefined first
            globalWatchViewModel.sendStructuredMessage(
                type = "sensorsLua",
                args = JSONObject().apply { put("type", "listPre") }
            )
        }
    }

    fun subscribeTo(sensor: LuaSensorInfo) {
        _pendingSensor.value = null
        _activeSensor.value = sensor
        _subscriptionState.value = SubscriptionState.SUBSCRIBING
        _lastError.value = null
        _incomingSamples.value = emptyList()

        globalWatchViewModel.sendStructuredMessage(
            type = "sensorsLua",
            args = JSONObject().apply {
                put("type", "sub")
                put("sensor", sensor.id)
                put("provider", _provider.value.value)
                put("useKnown", _useKnown.value)
                put("period", _period.value)
            }
        )
    }

    override fun unsubscribeCurrent() {
        _activeSensor.value = null
        _subscriptionState.value = SubscriptionState.UNSUBSCRIBING
        globalWatchViewModel.sendStructuredMessage(
            type = "sensorsLua",
            args = JSONObject().apply { put("type", "unsub") }
        )
    }

    private fun handleIncomingMessage(rawJson: String) {
        runCatching {
            val json = JSONObject(rawJson)
            val state = json.optString("state")
            val res = if (json.has("res")) json.opt("res") else json.opt("result")

            when (state) {
                MessageStates.ERROR -> {
                    _lastError.value = (res?.toString() ?: "Unknown error") to null
                    _subscriptionState.value = SubscriptionState.ERROR
                    _isDiscovering.value = false
                    discoveryPhase = 0
                }
                MessageStates.DONE -> {
                    val resStr = res?.toString() ?: ""
                    if (resStr == "Unsubscribed" || resStr == "Not subscribed") {
                        _subscriptionState.value = SubscriptionState.DISCONNECTED
                        _activeSensor.value = null
                    } else if (resStr == "Subscribed" || resStr == "Already subscribed") {
                        _subscriptionState.value = SubscriptionState.SUBSCRIBED
                    }

                    if (res != null && res !is String) {
                        parseListResult(res)
                        
                        if (discoveryPhase == 1) {
                            discoveryPhase = 2
                            globalWatchViewModel.sendStructuredMessage(
                                type = "sensorsLua",
                                args = JSONObject().apply { put("type", "list") }
                            )
                        } else {
                            discoveryPhase = 0
                            _isDiscovering.value = false
                        }
                    } else if (discoveryPhase != 0) {
                        // Handle cases where list result might be empty or a string
                        discoveryPhase = 0
                        _isDiscovering.value = false
                    }
                }
                MessageStates.STREAM -> {
                    if (_subscriptionState.value != SubscriptionState.SUBSCRIBED) {
                        _subscriptionState.value = SubscriptionState.SUBSCRIBED
                    }

                    val samplesStr = json.optString("samples") ?: return
                    val samplesArray = runCatching { JSONArray(samplesStr) }.getOrNull() ?: return
                    val parsedList = mutableListOf<SensorSample>()

                    for (i in 0 until samplesArray.length()) {
                        val sampleItem = samplesArray.get(i)
                        val valueMap = mutableMapOf<String, Double>()

                        when (sampleItem) {
                            is JSONObject -> {
                                sampleItem.keys().forEach { key ->
                                    valueMap[key] = sampleItem.optDouble(key, 0.0)
                                }
                            }
                            is JSONArray -> {
                                for (j in 0 until sampleItem.length()) {
                                    valueMap[j.toString()] = sampleItem.optDouble(j, 0.0)
                                }
                            }
                        }

                        if (valueMap.isNotEmpty()) {
                            parsedList.add(SensorSample(values = valueMap))
                        }
                    }

                    _incomingSamples.update { current ->
                        (current + parsedList).takeLast(50)
                    }
                }
            }
        }
    }

    private fun parseListResult(res: Any) {
        if (res is JSONObject) {
            val newList = mutableListOf<LuaSensorInfo>()
            // listPre result
            res.keys().forEach { key ->
                val sObj = res.getJSONObject(key)
                val id = sObj.optString("path")
                val name = sObj.optString("name")
                val props = sObj.optJSONArray("props")?.let { arr ->
                    List(arr.length()) { arr.getString(it) }
                } ?: emptyList()
                val available = sObj.optBoolean("available", false)
                
                newList.add(LuaSensorInfo(
                    id = id,
                    name = name,
                    path = id,
                    availability = if (available) SensorAvailability.AVAILABLE else SensorAvailability.UNAVAILABLE,
                    properties = props,
                    isPredefined = true
                ))
            }
            _predefinedSensors.value = newList.sortedBy { it.name }
        } else if (res is JSONArray) {
            val newList = mutableListOf<LuaSensorInfo>()
            // list result
            for (i in 0 until res.length()) {
                val id = res.getString(i)
                newList.add(LuaSensorInfo(
                    id = id,
                    availability = SensorAvailability.AVAILABLE
                ))
            }
            _allSensors.value = newList.sortedBy { it.id }
        }
    }
}
