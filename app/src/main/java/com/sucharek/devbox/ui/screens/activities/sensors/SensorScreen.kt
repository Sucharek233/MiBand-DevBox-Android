package com.sucharek.devbox.ui.screens.activities.sensors

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sucharek.devbox.ui.screens.maindashboard.WatchViewModel

@Composable
fun SensorScreen(
    viewModel: SensorViewModel = viewModel(),
    watchViewModel: WatchViewModel,
    onSensorClick: (String) -> Unit
) {
    val sensorList by viewModel.sensorList.collectAsState()
    val isDiscovering by viewModel.isDiscovering.collectAsState()
    val activeSensor by viewModel.activeSensor.collectAsState()
    val subscriptionState by viewModel.subscriptionState.collectAsState()
    val isBusy by watchViewModel.isJsBusy.collectAsState()

    var pendingConfigSensor by remember { mutableStateOf<SensorInfo?>(null) }
    var pendingUnavailableSensor by remember { mutableStateOf<SensorInfo?>(null) }
    
    // Automatically trigger a scan if needed
    LaunchedEffect(Unit) {
        viewModel.discoverIfNeeded()
    }

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = viewModel.scrollIndex,
        initialFirstVisibleItemScrollOffset = viewModel.scrollOffset
    )

    DisposableEffect(Unit) {
        onDispose {
            viewModel.scrollIndex = listState.firstVisibleItemIndex
            viewModel.scrollOffset = listState.firstVisibleItemScrollOffset
        }
    }

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

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (activeSensor != null) {
                        StreamStatusChip(state = subscriptionState)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = { viewModel.discoverSensors() },
                        enabled = !isBusy
                    ) {
                        if (isDiscovering) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                }
            }

            // --- Device Sensors List ---
            Text(
                text = "Device Sensors",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                state = listState,
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
                                pendingConfigSensor = sensor
                            }
                        },
                        enabled = !isBusy
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
                        pendingConfigSensor = sensor
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

    pendingConfigSensor?.let { sensor ->
        SensorConfigDialog(
            sensor = sensor,
            onSubscribe = { entries, interval ->
                viewModel.subscribeTo(sensor.name, entries, interval)
                onSensorClick(sensor.name)
                pendingConfigSensor = null
            },
            onDismiss = { pendingConfigSensor = null },
            isBusy = isBusy
        )
    }
}

@Composable
fun SensorConfigDialog(
    sensor: SensorInfo,
    onSubscribe: (entries: Int, interval: Int) -> Unit,
    onDismiss: () -> Unit,
    isBusy: Boolean
) {
    var streamEntries by remember { mutableFloatStateOf(10f) }
    var sendInterval by remember { mutableFloatStateOf(1000f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configure ${sensor.name}") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Stream Settings", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(16.dp))

                Text("Entries per Send: ${streamEntries.toInt()}", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = streamEntries,
                    onValueChange = { streamEntries = it },
                    valueRange = 1f..50f,
                    steps = 49,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isBusy
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                Text("Send Interval: ${sendInterval.toInt()}ms", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = sendInterval,
                    onValueChange = { sendInterval = it },
                    valueRange = 100f..5000f,
                    steps = 49,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isBusy
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSubscribe(streamEntries.toInt(), sendInterval.toInt()) }, enabled = !isBusy) {
                Text("Subscribe")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun SensorItemRow(
    sensor: SensorInfo,
    isActive: Boolean,
    onClick: () -> Unit,
    enabled: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() },
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
    state: SubscriptionState
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
