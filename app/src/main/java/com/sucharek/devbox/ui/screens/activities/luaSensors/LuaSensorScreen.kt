package com.sucharek.devbox.ui.screens.activities.luaSensors

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
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
import com.sucharek.devbox.ui.screens.activities.sensors.StreamStatusChip
import com.sucharek.devbox.ui.screens.activities.sensors.SubscriptionState
import com.sucharek.devbox.ui.screens.activities.sensors.SensorAvailability

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LuaSensorScreen(
    viewModel: LuaSensorViewModel = viewModel(),
    watchViewModel: WatchViewModel,
    onBack: () -> Unit,
    onSensorSubscribed: (String) -> Unit
) {
    val predefinedSensors by viewModel.predefinedSensors.collectAsState()
    val allSensors by viewModel.allSensors.collectAsState()
    val isDiscovering by viewModel.isDiscovering.collectAsState()
    val isBusy by watchViewModel.isAnyOperationActive.collectAsState()
    val activeSensor by viewModel.activeSensor.collectAsState()
    val pendingSensor by viewModel.pendingSensor.collectAsState()
    val subscriptionState by viewModel.subscriptionState.collectAsState()
    val lastError by viewModel.lastError.collectAsState()

    val provider by viewModel.provider.collectAsState()
    val useKnown by viewModel.useKnown.collectAsState()
    val period by viewModel.period.collectAsState()
    val sliderValue by viewModel.sliderValue.collectAsState()
    val selectedTabIndex by viewModel.selectedTabIndex.collectAsState()
    
    val predefinedListState = rememberLazyListState(
        initialFirstVisibleItemIndex = viewModel.predefinedScrollIndex,
        initialFirstVisibleItemScrollOffset = viewModel.predefinedScrollOffset
    )
    val allListState = rememberLazyListState(
        initialFirstVisibleItemIndex = viewModel.allScrollIndex,
        initialFirstVisibleItemScrollOffset = viewModel.allScrollOffset
    )

    DisposableEffect(Unit) {
        onDispose {
            viewModel.predefinedScrollIndex = predefinedListState.firstVisibleItemIndex
            viewModel.predefinedScrollOffset = predefinedListState.firstVisibleItemScrollOffset
            viewModel.allScrollIndex = allListState.firstVisibleItemIndex
            viewModel.allScrollOffset = allListState.firstVisibleItemScrollOffset
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
            // --- Header & Status ---
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
                    if (subscriptionState != SubscriptionState.DISCONNECTED) {
                        StreamStatusChip(
                            state = subscriptionState,
                            activeSensor = activeSensor?.name ?: "Unknown"
                        )
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

            if (lastError != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = lastError?.first.orEmpty(),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            TabRow(selectedTabIndex = selectedTabIndex) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { viewModel.setSelectedTabIndex(0) },
                    text = { Text("Predefined") }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { viewModel.setSelectedTabIndex(1) },
                    text = { Text("All") }
                )
            }

            if (selectedTabIndex == 1) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.warningContainer()),
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onWarningContainer())
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Raw nodes might not report data or could cause device instability.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onWarningContainer()
                        )
                    }
                }
            }

            if (isDiscovering && (if (selectedTabIndex == 0) predefinedSensors else allSensors).isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            val (displayList, currentListState) = if (selectedTabIndex == 0) {
                predefinedSensors to predefinedListState
            } else {
                allSensors to allListState
            }

            LazyColumn(
                state = currentListState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp)
            ) {
                items(
                    items = displayList,
                    key = { it.id }
                ) { sensor ->
                    LuaSensorItemRow(
                        sensor = sensor,
                        isActive = activeSensor?.id == sensor.id,
                        onClick = { viewModel.setPendingSensor(sensor) },
                        enabled = !isBusy
                    )
                }
            }
        }
    }

    // Config Dialog
    pendingSensor?.let { sensor ->
        LuaSensorConfigDialog(
            sensor = sensor,
            provider = provider,
            useKnown = useKnown,
            period = period,
            sliderValue = sliderValue,
            onProviderChange = viewModel::setProvider,
            onUseKnownChange = viewModel::setUseKnown,
            onSliderChange = viewModel::setSliderValue,
            onSubscribe = { 
                viewModel.subscribeTo(sensor)
                onSensorSubscribed(sensor.name)
            },
            onDismiss = { viewModel.setPendingSensor(null) },
            isBusy = isBusy
        )
    }
}

@Composable
fun LuaSensorConfigDialog(
    sensor: LuaSensorInfo,
    provider: LuaSensorProvider,
    useKnown: Boolean,
    period: Int,
    sliderValue: Float,
    onProviderChange: (LuaSensorProvider) -> Unit,
    onUseKnownChange: (Boolean) -> Unit,
    onSliderChange: (Float) -> Unit,
    onSubscribe: () -> Unit,
    onDismiss: () -> Unit,
    isBusy: Boolean
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configure ${sensor.name}") },
        text = {
            Column {
                LuaSensorSettings(
                    provider = provider,
                    useKnown = useKnown,
                    period = period,
                    sliderValue = sliderValue,
                    onProviderChange = onProviderChange,
                    onUseKnownChange = onUseKnownChange,
                    onSliderChange = onSliderChange,
                    isBusy = isBusy
                )
                
                if (provider == LuaSensorProvider.TOPIC) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Warning: Topic provider mostly causes device crashes (except for Accelerometer).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onSubscribe, enabled = !isBusy) {
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
fun LuaSensorSettings(
    provider: LuaSensorProvider,
    useKnown: Boolean,
    period: Int,
    sliderValue: Float,
    onProviderChange: (LuaSensorProvider) -> Unit,
    onUseKnownChange: (Boolean) -> Unit,
    onSliderChange: (Float) -> Unit,
    isBusy: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Stream Settings", style = MaterialTheme.typography.titleSmall)
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Provider:", modifier = Modifier.weight(1f))
            LuaSensorProvider.entries.forEach { p ->
                FilterChip(
                    selected = provider == p,
                    onClick = { onProviderChange(p) },
                    label = { Text(p.name) },
                    modifier = Modifier.padding(start = 4.dp),
                    enabled = !isBusy
                )
            }
        }
        
        if (provider == LuaSensorProvider.FILE) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Use Known Props:", modifier = Modifier.weight(1f))
                Switch(checked = useKnown, onCheckedChange = onUseKnownChange, enabled = !isBusy)
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Column {
            Text("Period: ${period}ms", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = sliderValue,
                onValueChange = onSliderChange,
                valueRange = 0f..1f,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isBusy
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("50ms", style = MaterialTheme.typography.labelSmall)
                Text("1000ms", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

// Extension to get warning colors easily
@Composable
fun ColorScheme.warningContainer() = Color(0xFFFFF4E5)
@Composable
fun ColorScheme.onWarningContainer() = Color(0xFF663C00)

@Composable
fun LuaSensorItemRow(
    sensor: LuaSensorInfo,
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = sensor.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = sensor.id,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            LuaAvailabilityBadge(status = sensor.availability)
        }
    }
}

@Composable
fun LuaAvailabilityBadge(status: SensorAvailability) {
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
