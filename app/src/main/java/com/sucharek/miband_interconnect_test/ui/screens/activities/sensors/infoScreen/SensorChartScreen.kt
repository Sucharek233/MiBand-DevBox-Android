package com.sucharek.miband_interconnect_test.ui.screens.activities.sensors.infoScreen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.patrykandpatrick.vico.compose.cartesian.AutoScrollCondition
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.Scroll
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.lineModel
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.common.Fill
import com.sucharek.miband_interconnect_test.ui.screens.activities.sensors.SensorSample
import com.sucharek.miband_interconnect_test.ui.screens.activities.sensors.BaseSensorViewModel
import com.sucharek.miband_interconnect_test.ui.screens.activities.sensors.SubscriptionState
import com.sucharek.miband_interconnect_test.ui.screens.activities.sensors.StreamStatusChip
import com.sucharek.miband_interconnect_test.ui.screens.maindashboard.WatchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensorChartScreen(
    viewModel: BaseSensorViewModel,
    watchViewModel: WatchViewModel,
    sensorName: String,
    onBack: () -> Unit
) {
    val subscriptionState by viewModel.subscriptionState.collectAsState()
    val incomingSamples by viewModel.incomingSamples.collectAsState()
    val lastError by viewModel.lastError.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Chart", "Raw Data")

    val currentSubscriptionState = rememberUpdatedState(subscriptionState)
    var isNavigatingBackDueToCancel by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        watchViewModel.operationCanceledEvents.collect {
            isNavigatingBackDueToCancel = true
            onBack()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            val state = currentSubscriptionState.value
            val isConnected = state == SubscriptionState.SUBSCRIBED || state == SubscriptionState.SUBSCRIBING
            
            if (isConnected && !isNavigatingBackDueToCancel) {
                viewModel.unsubscribeCurrent()
            }
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
                    text = sensorName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                StreamStatusChip(state = subscriptionState, activeSensor = sensorName)
            }

            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            if (subscriptionState == SubscriptionState.ERROR) {
                ErrorDisplay(lastError)
            } else {
                when (selectedTab) {
                    0 -> ChartTabContent(incomingSamples)
                    1 -> RawDataTabContent(incomingSamples)
                }
            }
        }
    }
}

@Composable
private fun ErrorDisplay(error: Pair<String, String?>?) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Subscription Error",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = error?.first ?: "Unknown error occurred",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            error?.second?.let { stack ->
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Stack Trace:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stack,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    ),
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChartTabContent(samples: List<SensorSample>) {
    var selectedKeys by remember { mutableStateOf<Set<String>>(emptySet()) }
    val allKeys = remember(samples) {
        samples.flatMap { it.values.keys }.distinct().sortedBy { it.toIntOrNull() ?: Int.MAX_VALUE }
    }

    // Auto-select first 3 keys initially
    LaunchedEffect(allKeys) {
        if (selectedKeys.isEmpty() && allKeys.isNotEmpty()) {
            selectedKeys = allKeys.take(3).toSet()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // --- Chart Section ---
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (samples.isEmpty() || selectedKeys.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (samples.isEmpty()) "Waiting for data..." else "Select values to plot",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                VicoSensorChart(samples, selectedKeys)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))

        // --- Value Selection ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Select values:",
                style = MaterialTheme.typography.titleSmall
            )
            
            Row {
                TextButton(
                    onClick = { selectedKeys = allKeys.toSet() },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Select All", fontSize = 12.sp)
                }
                TextButton(
                    onClick = { selectedKeys = emptySet() },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Deselect All", fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 160.dp)
                .verticalScroll(rememberScrollState())
        ) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                allKeys.forEach { key ->
                    val isSelected = selectedKeys.contains(key)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .toggleable(
                                value = isSelected,
                                role = Role.Checkbox,
                                onValueChange = {
                                    selectedKeys = if (it) selectedKeys + key else selectedKeys - key
                                }
                            )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = null, // Handled by surface click
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                text = key,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VicoSensorChart(samples: List<SensorSample>, selectedKeys: Set<String>) {
    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(samples, selectedKeys) {
        modelProducer.runTransaction {
            lineModel {
                selectedKeys.forEach { key ->
                    series(samples.map { it.values[key] ?: 0.0 })
                }
            }
        }
    }

    val scrollState = rememberVicoScrollState(
        initialScroll = Scroll.Absolute.End,
        autoScroll = Scroll.Absolute.End,
        autoScrollCondition = AutoScrollCondition.OnModelGrowth
    )

    val colors = listOf(Color.Red, Color.Green, Color.Blue, Color.Yellow, Color.Cyan, Color.Magenta)

    CartesianChartHost(
        modifier = Modifier.fillMaxSize().padding(8.dp),
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(
                lineProvider = LineCartesianLayer.LineProvider.series(
                    selectedKeys.indices.map { index ->
                        LineCartesianLayer.rememberLine(
                            fill = LineCartesianLayer.LineFill.single(Fill(colors[index % colors.size])),
                            stroke = LineCartesianLayer.LineStroke.Continuous(thickness = 2.dp)
                        )
                    }
                )
            ),
            startAxis = VerticalAxis.rememberStart(),
            bottomAxis = HorizontalAxis.rememberBottom(),
        ),
        modelProducer = modelProducer,
        scrollState = scrollState,
        animationSpec = null,
    )
}

@Composable
private fun RawDataTabContent(samples: List<SensorSample>) {
    val lastSample = samples.lastOrNull()

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (lastSample == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No data received yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Latest Sample (${lastSample.timestamp}):",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                lastSample.values.entries
                    .sortedBy { it.key.toIntOrNull() ?: Int.MAX_VALUE }
                    .forEach { (key, value) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = key,
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                            Text(
                                text = value.toString(),
                                style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 2.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                        )
                    }
            }
        }
    }
}
