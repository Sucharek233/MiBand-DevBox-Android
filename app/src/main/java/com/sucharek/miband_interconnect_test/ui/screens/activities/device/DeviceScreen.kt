package com.sucharek.miband_interconnect_test.ui.screens.activities.device

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sucharek.miband_interconnect_test.ui.screens.maindashboard.WatchViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceScreen(
    viewModel: DeviceViewModel,
    watchViewModel: WatchViewModel
) {
    val deviceInfo by viewModel.deviceInfo.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isBusy by watchViewModel.isAnyOperationActive.collectAsState()

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
                    text = "Device Info",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = { viewModel.refresh() },
                    enabled = !isBusy
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            }

            if (deviceInfo == null && isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (deviceInfo != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    InfoSection("Identity") {
                        InfoRow("Device Type", deviceInfo!!.deviceType)
                        InfoRow("Device ID", deviceInfo!!.deviceId)
                        InfoRow("IMEI", deviceInfo!!.imei)
                        InfoRow("Serial", deviceInfo!!.serial)
                        InfoRow("Model", deviceInfo!!.model)
                        InfoRow("Product", deviceInfo!!.product)
                        InfoRow("Manufacturer", deviceInfo!!.manufacturer)
                        InfoRow("Brand", deviceInfo!!.brand)
                    }

                    InfoSection("Software") {
                        InfoRow("OS Type", deviceInfo!!.osType)
                        InfoRow("OS Version", "${deviceInfo!!.osVersionName} (${deviceInfo!!.osVersionCode})")
                        InfoRow("Platform", "${deviceInfo!!.platformVersionName} (${deviceInfo!!.platformVersionCode})")
                        InfoRow("API Level", deviceInfo!!.apiLevel)
                        InfoRow("Language", deviceInfo!!.language)
                        InfoRow("Region", deviceInfo!!.region)
                    }

                    InfoSection("Display") {
                        InfoRow("Resolution", "${deviceInfo!!.screenWidth}x${deviceInfo!!.screenHeight}")
                        InfoRow("Density", String.format(Locale.US, "%.1f", deviceInfo!!.screenDensity))
                        InfoRow("Shape", deviceInfo!!.screenShape)
                    }

                    deviceInfo!!.storage?.let { storage ->
                        val usedPercent = if (storage.total > 0) storage.used.toFloat() / storage.total else 0f
                        InfoSection("Storage") {
                            LinearProgressIndicator(
                                progress = { usedPercent },
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            )
                            InfoRow("Total", formatSize(storage.total))
                            InfoRow("Available", formatSize(storage.available))
                            InfoRow("Used", formatSize(storage.used))
                        }
                    }

                    if (deviceInfo!!.other.isNotEmpty()) {
                        InfoSection("Other") {
                            deviceInfo!!.other.forEach { (key, value) ->
                                InfoRow(key, value)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No data received", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun InfoSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            content()
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            fontWeight = FontWeight.Medium
        )
    }
}

private fun formatSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes.toDouble() / 1024.0
    return String.format(Locale.US, "%.1f KB", kb)
}
