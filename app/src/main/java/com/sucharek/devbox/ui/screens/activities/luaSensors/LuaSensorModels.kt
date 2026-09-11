package com.sucharek.devbox.ui.screens.activities.luaSensors

import com.sucharek.devbox.ui.screens.activities.sensors.SensorAvailability

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

