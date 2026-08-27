package com.sucharek.miband_interconnect_test.ui.screens.activities.sensors

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SensorScreen(
    viewModel: SensorViewModel = viewModel(),
    onSensorClick: (String) -> Unit
) {
    val sensorList by viewModel.sensorList.collectAsState()
    val isDiscovering by viewModel.isDiscovering.collectAsState()
    val scanProgress by viewModel.scanProgress.collectAsState()
    val activeSensor by viewModel.activeSensor.collectAsState()
    val subscriptionState by viewModel.subscriptionState.collectAsState()

    var pendingUnavailableSensor by remember { mutableStateOf<SensorInfo?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // --- Header ---
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sensors",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                if (activeSensor != null) {
                    StreamStatusChip(state = subscriptionState, activeSensor = activeSensor!!)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Button(
                    onClick = { viewModel.discoverSensors() },
                    enabled = !isDiscovering,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isDiscovering) "Scanning Sensors…" else "Verify Availability")
                }

                if (isDiscovering) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { scanProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- Device Sensors List ---
            Text(
                text = "Device Sensors",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp)
            ) {
                items(
                    items = sensorList,
                    key = { it.name }
                ) { sensor ->
                    SensorItemRow(
                        sensor = sensor,
                        isActive = activeSensor == sensor.name,
                        onClick = {
                            if (sensor.availability == SensorAvailability.UNAVAILABLE) {
                                pendingUnavailableSensor = sensor
                            } else {
                                viewModel.subscribeTo(sensor.name)
                                onSensorClick(sensor.name)
                            }
                        }
                    )
                }
            }
        }
    }

    // Dialog for unavailable sensors
    pendingUnavailableSensor?.let { sensor ->
        AlertDialog(
            onDismissRequest = { pendingUnavailableSensor = null },
            title = { Text("Sensor Flagged Unavailable") },
            text = {
                Text(
                    "'${sensor.name}' was reported as unavailable during discovery. " +
                            "The sensor stream may not respond, but you can attempt connection anyway."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.subscribeTo(sensor.name)
                        onSensorClick(sensor.name)
                        pendingUnavailableSensor = null
                    }
                ) {
                    Text("Proceed Anyway", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingUnavailableSensor = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SensorItemRow(
    sensor: SensorInfo,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = sensor.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )

                if (sensor.error != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = sensor.error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            AvailabilityBadge(status = sensor.availability)
        }
    }
}

@Composable
private fun AvailabilityBadge(status: SensorAvailability) {
    val (bgColor, textColor, text) = when (status) {
        SensorAvailability.UNKNOWN -> Triple(Color(0xFF383838), Color.LightGray, "Unknown")
        SensorAvailability.AVAILABLE -> Triple(Color(0xFF1B4D2E), Color(0xFF81C784), "Available")
        SensorAvailability.UNAVAILABLE -> Triple(Color(0xFF4D1B1B), Color(0xFFE57373), "Unavailable")
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
internal fun StreamStatusChip(
    state: SubscriptionState,
    activeSensor: String
) {
    val (color, label) = when (state) {
        SubscriptionState.SUBSCRIBING -> Pair(Color(0xFFFFC107), "Connecting…")
        SubscriptionState.SUBSCRIBED -> Pair(Color(0xFF4CAF50), "Live")
        SubscriptionState.UNSUBSCRIBING -> Pair(Color(0xFFFF5722), "Disconnecting…")
        SubscriptionState.DISCONNECTED -> Pair(Color.Gray, "Idle")
        SubscriptionState.ERROR -> Pair(MaterialTheme.colorScheme.error, "Error")
    }

    Surface(
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(color, CircleShape)
            )
            Spacer(modifier = Modifier.size(6.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}