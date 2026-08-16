package com.sucharek.miband_interconnect_test.ui.screens.activities.sensors

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharek.miband_interconnect_test.ui.screens.maindashboard.WatchViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject

class SensorViewModel(
    private val globalWatchViewModel: WatchViewModel
) : ViewModel() {

    // 1. Discovery State
    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    private val _scanProgress = MutableStateFlow(0f)
    val scanProgress: StateFlow<Float> = _scanProgress.asStateFlow()

    // Map of known sensors and their discovery info (Default: UNKNOWN)
    private val _sensorList = MutableStateFlow<List<SensorInfo>>(defaultKnownSensors)
    val sensorList: StateFlow<List<SensorInfo>> = _sensorList.asStateFlow()

    // 2. Stream State
    private val _activeSensor = MutableStateFlow<String?>(null)
    val activeSensor: StateFlow<String?> = _activeSensor.asStateFlow()

    private val _subscriptionState = MutableStateFlow(SubscriptionState.DISCONNECTED)
    val subscriptionState: StateFlow<SubscriptionState> = _subscriptionState.asStateFlow()

    private val _lastError = MutableStateFlow<Pair<String, String?>?>(null) // Pair(msg, stack)
    val lastError: StateFlow<Pair<String, String?>?> = _lastError.asStateFlow()

    private val _incomingSamples = MutableStateFlow<List<SensorSample>>(emptyList())
    val incomingSamples: StateFlow<List<SensorSample>> = _incomingSamples.asStateFlow()

    private var scanJob: Job? = null

    init {
        viewModelScope.launch(Dispatchers.Default) {
            globalWatchViewModel.sensorMessages.collectLatest { payload ->
                handleIncomingMessage(payload)
            }
        }
    }

    // --- Actions ---

    fun discoverSensors() {
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
//            _isDiscovering.value = true
//            _scanProgress.value = 0f

            val payload = JSONObject().apply {
                put("req", "listLite")
            }
            globalWatchViewModel.sendStructuredMessage(
                type = "sensors",
                args = payload
            )

//             Smooth 15-second fake progress bar timer
//            val totalDurationMs = 15_000L
//            val stepMs = 100L
//            val steps = totalDurationMs / stepMs
//
//            for (i in 1..steps) {
//                if (!_isDiscovering.value) break // Stop if response comes earlier
//                delay(stepMs)
//                _scanProgress.value = i.toFloat() / steps
//            }
//
//            _isDiscovering.value = false
        }
    }

    fun subscribeTo(sensorName: String) {
        _activeSensor.value = sensorName
        _subscriptionState.value = SubscriptionState.SUBSCRIBING
        _lastError.value = null
        _incomingSamples.value = emptyList()

        println("[SensorStream] ---> SUBSCRIBING TO: $sensorName")

        val payload = JSONObject().apply {
            put("req", "sub")
            put("sensor", sensorName)
        }
        globalWatchViewModel.sendStructuredMessage(
            type = "sensors",
            args = payload
        )
    }

    fun unsubscribeCurrent() {
        val current = _activeSensor.value
        println("[SensorStream] <--- UNSUBSCRIBING FROM: $current")

        _subscriptionState.value = SubscriptionState.UNSUBSCRIBING
        val payload = JSONObject().apply {
            put("req", "unsub")
        }
        globalWatchViewModel.sendStructuredMessage(
            type = "sensors",
            args = payload
        )
    }

    private fun handleIncomingMessage(rawJson: String) {
        println("[SensorStream] RAW INCOMING: $rawJson")

        runCatching {
            val json = JSONObject(rawJson)
            if (json.optString("type") != "sensors") return

            val state = json.optString("state")

            when (state) {
                "error" -> {
                    val msg = json.optString("msg", "Unknown error")
                    val stack = json.optString("stack", "")
                    _lastError.value = msg to if (stack.isNotEmpty()) stack else null
                    _subscriptionState.value = SubscriptionState.ERROR
                }

                "done" -> {
                    if (json.has("res")) {
                        parseListResult(json.getJSONObject("res"))
                        _isDiscovering.value = false
                    }

                    val msg = json.optString("msg").ifEmpty { json.optString("res") }
                    when (msg) {
                        "Subscribed", "Already subscribed" -> {
                            _subscriptionState.value = SubscriptionState.SUBSCRIBED
                        }
                        "Unsubscribed", "Not subscribed" -> {
                            _subscriptionState.value = SubscriptionState.DISCONNECTED
                            _activeSensor.value = null
                        }
                    }
                }

                "stream" -> {
                    if (_subscriptionState.value != SubscriptionState.SUBSCRIBED) {
                        _subscriptionState.value = SubscriptionState.SUBSCRIBED
                    }

                    val samplesArray = json.optJSONArray("samples") ?: return
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
                            is org.json.JSONArray -> {
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
                        (current + parsedList).takeLast(100)
                    }
                }
            }
        }
    }

    private fun parseListResult(resObj: JSONObject) {
        val currentList = _sensorList.value
        val processedNames = mutableSetOf<String>()

        // 1. Update existing sensors in the list
        val updatedList = currentList.map { sensor ->
            if (resObj.has(sensor.name)) {
                processedNames.add(sensor.name)
                val sObj = resObj.getJSONObject(sensor.name)
                val isAvailable = sObj.optBoolean("available", false)
                val availStatus = if (isAvailable) SensorAvailability.AVAILABLE else SensorAvailability.UNAVAILABLE

                sensor.copy(
                    availability = availStatus,
                    subscribeFn = sObj.optString("subscribeFn", ""),
                    unsubscribeFn = sObj.optString("unsubscribeFn", ""),
                    error = sObj.optString("error", "").ifEmpty { null }
                )
            } else {
                // Not found in current scan -> mark unavailable
                sensor.copy(availability = SensorAvailability.UNAVAILABLE)
            }
        }.toMutableList()

        // 2. Add any NEW sensors found in the response that weren't in our initial list
        resObj.keys().forEach { name ->
            if (name !in processedNames) {
                val sObj = resObj.getJSONObject(name)
                val isAvailable = sObj.optBoolean("available", false)
                val availStatus = if (isAvailable) SensorAvailability.AVAILABLE else SensorAvailability.UNAVAILABLE

                updatedList.add(
                    SensorInfo(
                        name = name,
                        availability = availStatus,
                        subscribeFn = sObj.optString("subscribeFn", ""),
                        unsubscribeFn = sObj.optString("unsubscribeFn", ""),
                        error = sObj.optString("error", "").ifEmpty { null }
                    )
                )
            }
        }

        _sensorList.value = updatedList
    }

    companion object {
        val defaultKnownSensors = listOf(
            SensorInfo("Accelerometer"),
            SensorInfo("AmbientTemperature"),
            SensorInfo("Barometer"),
            SensorInfo("Compass"),
            SensorInfo("Humidity"),
            SensorInfo("Light"),
            SensorInfo("Pressure"),
            SensorInfo("Proximity"),
            SensorInfo("StepCounter")
        )
    }
}