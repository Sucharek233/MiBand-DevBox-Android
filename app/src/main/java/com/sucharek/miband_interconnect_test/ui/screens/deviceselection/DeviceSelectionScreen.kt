package com.sucharek.miband_interconnect_test.ui.screens.deviceselection

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sucharek.miband_interconnect_test.ui.screens.maindashboard.WatchViewModel
import com.xiaomi.xms.wearable.node.Node

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceSelectionScreen(
    viewModel: WatchViewModel,
    onDeviceSelected: (Node) -> Unit,
    modifier: Modifier = Modifier
) {
    val availableDevices by viewModel.discoveredDevices.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val scanError by viewModel.scanError.collectAsState()

    // Automatically trigger a query when the screen opens
    LaunchedEffect(Unit) {
        viewModel.refreshDevices()
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Select Your Band") })
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Ensure your Mi Band is nearby and connected to your phone via the companion app.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Button(
                onClick = { viewModel.refreshDevices() },
                enabled = !isScanning,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Fetching Connected Devices...")
                } else {
                    Text("Refresh Devices")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Display error if present
            scanError?.let { error ->
                Text(
                    text = "Error: $error",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            if (availableDevices.isEmpty() && !isScanning) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No connected devices found.\nMake sure your band is paired in system settings.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(availableDevices) { node ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onDeviceSelected(node) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = node.name ?: "Unknown Device",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "ID: ${node.id}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Text("Select", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}