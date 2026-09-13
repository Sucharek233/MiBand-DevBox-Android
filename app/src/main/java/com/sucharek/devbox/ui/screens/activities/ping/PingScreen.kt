package com.sucharek.devbox.ui.screens.activities.ping

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PingScreen(
    viewModel: PingViewModel,
    initialType: String? = null
) {
    val pings by viewModel.pings.collectAsState()
    val isPinging by viewModel.isPinging.collectAsState()
    val listState = rememberLazyListState()

    var selectedPingType by remember { mutableStateOf(initialType ?: "lua") }

    // Scroll to the latest ping whenever a new one is added OR an existing one is updated (e.g. finishes)
    LaunchedEffect(pings) {
        if (pings.isNotEmpty()) {
            listState.animateScrollToItem(0)
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ping",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Button(
                    onClick = { viewModel.sendPing(selectedPingType) },
                    enabled = !isPinging,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Test Latency", fontSize = 12.sp)
                }
            }

            SecondaryTabRow(selectedTabIndex = if (selectedPingType == "qjs") 0 else 1) {
                Tab(
                    selected = selectedPingType == "qjs",
                    onClick = { selectedPingType = "qjs" },
                    text = { Text("QuickJS") }
                )
                Tab(
                    selected = selectedPingType == "lua",
                    onClick = { selectedPingType = "lua" },
                    text = { Text("Lua") }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "History",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (pings.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No data recorded", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    val filteredPings = remember(pings, selectedPingType) {
                        pings.filter { it.type == selectedPingType }
                    }
                    
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredPings.reversed()) { ping ->
                            PingHistoryItem(ping)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PingHistoryItem(ping: PingResult) {
    val statusColor = when (ping.status) {
        PingStatus.PENDING -> Color.Gray
        PingStatus.SUCCESS -> Color(0xFF4CAF50)
        PingStatus.TIMEOUT -> Color(0xFFFF9800)
        PingStatus.ERROR -> Color(0xFFF44336)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(statusColor, RoundedCornerShape(5.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (ping.status == PingStatus.PENDING) "Pinging..." else "Ping #${ping.id}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = ping.type.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = String.format(Locale.US, "%tT", ping.androidStartTime),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (ping.status == PingStatus.SUCCESS) {
                Spacer(modifier = Modifier.height(8.dp))

                val totalRtt = if (ping.androidEndTime != null) ping.androidEndTime - ping.androidStartTime else 0L

                if (ping.type == "lua") {
                    val processing = if (ping.watchEndTime != null && ping.watchStartTime != null)
                        ping.watchEndTime - ping.watchStartTime else 0L
                    
                    val networkTime = (totalRtt - processing).coerceAtLeast(0L)
                    val estLatency = networkTime / 2

                    TimingRow("Watch processing", processing)
                    TimingRow("Est. One-way Latency", estLatency)
                } else {
                    val estLatency = totalRtt / 2
                    TimingRow("Est. One-way Latency", estLatency)
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Round-trip (Total)",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$totalRtt ms",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else if (ping.status == PingStatus.ERROR || ping.status == PingStatus.TIMEOUT) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (ping.status == PingStatus.TIMEOUT) "Request timed out" else (ping.errorMsg ?: "Unknown error"),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun TimingRow(label: String, value: Long?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = if (value != null) "$value ms" else "--",
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace
        )
    }
}
