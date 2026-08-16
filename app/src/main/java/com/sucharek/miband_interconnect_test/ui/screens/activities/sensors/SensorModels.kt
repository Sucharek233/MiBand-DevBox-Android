package com.sucharek.miband_interconnect_test.ui.screens.activities.sensors

enum class SensorAvailability {
    UNKNOWN,
    AVAILABLE,
    UNAVAILABLE
}

data class SensorInfo(
    val name: String,
    val availability: SensorAvailability = SensorAvailability.UNKNOWN,
    val properties: List<String> = emptyList(),
    val sampleData: Map<String, Any?>? = null,
    val subscribeFn: String? = null,
    val unsubscribeFn: String? = null,
    val error: String? = null
)

// Active subscription status
enum class SubscriptionState {
    DISCONNECTED,
    SUBSCRIBING,
    SUBSCRIBED,
    UNSUBSCRIBING,
    ERROR
}

// Stream data structure
data class SensorSample(
    val values: Map<String, Double>,
    val timestamp: Long = System.currentTimeMillis()
)