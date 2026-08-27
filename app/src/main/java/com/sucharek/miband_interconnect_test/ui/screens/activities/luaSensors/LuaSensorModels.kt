package com.sucharek.miband_interconnect_test.ui.screens.activities.luaSensors

import com.sucharek.miband_interconnect_test.ui.screens.activities.sensors.SensorAvailability

enum class LuaSensorProvider(val value: String) {
    FILE("file"),
    TOPIC("topic")
}

data class LuaSensorInfo(
    val id: String,
    val name: String = id,
    val path: String? = null,
    val availability: SensorAvailability = SensorAvailability.UNKNOWN,
    val properties: List<String> = emptyList(),
    val isPredefined: Boolean = false
)

