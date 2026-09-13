package com.sucharek.devbox.ui.screens.activities.luasysinfo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LuaSysInfoScreen(
    viewModel: LuaSysInfoViewModel,
    modifier: Modifier = Modifier
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    
    // System Info
    val cpuInfo by viewModel.cpuInfo.collectAsState()
    val cpuLoad by viewModel.cpuLoad.collectAsState()
    val memInfo by viewModel.memInfo.collectAsState()
    val memPool by viewModel.memPool.collectAsState()
    val tcbInfo by viewModel.tcbInfo.collectAsState()
    val version by viewModel.version.collectAsState()
    val partitions by viewModel.partitions.collectAsState()

    // Disk Info
    val partitionsList by viewModel.partitionsList.collectAsState()
    val blocksInfo by viewModel.blocksInfo.collectAsState()

    // Props
    val propsList by viewModel.propsList.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("System", "Disk", "Props")

    Scaffold(
        modifier = modifier.fillMaxSize()
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
                    text = "System Info",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                IconButton(onClick = { viewModel.refreshAll() }, enabled = !isLoading) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh All")
                }
            }

            SecondaryTabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    error?.let {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Error: $it",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    when (selectedTab) {
                        0 -> {
                            InfoCard("Kernel Version", version)
                            InfoCard("CPU Load", cpuLoad)
                            InfoCard("CPU Info", cpuInfo)
                            InfoCard("Memory Info", memInfo)
                            InfoCard("Memory Pool", memPool)
                            InfoCard("TCB Info", tcbInfo)
                            InfoCard("Partitions", partitions)
                        }
                        1 -> {
                            partitionsList.forEach { partition ->
                                PartitionCard(partition)
                            }
                            InfoCard("Raw Block Info", blocksInfo)
                        }
                        2 -> {
                            PropsSection(
                                props = propsList,
                                onUpdateProp = { k, v -> viewModel.setProp(k, v) },
                                isLoading = isLoading
                            )
                        }
                    }
                }
                
                if (isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun InfoCard(title: String, content: String?) {
    if (content.isNullOrBlank()) return
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = content.trim(),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )
            )
        }
    }
}

@Composable
private fun PropsSection(
    props: List<PropItem>,
    onUpdateProp: (String, String) -> Unit,
    isLoading: Boolean
) {
    var editingProp by remember { mutableStateOf<PropItem?>(null) }
    var customPropKey by remember { mutableStateOf("") }
    var customPropValue by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Warning
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Warning: Modifying system properties may crash your band or cause unexpected behavior. Changes are lost on reboot.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        // Custom Prop Creation
        Card(
            modifier = Modifier.fillMaxWidth(),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Create / Set Custom Property", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = customPropKey,
                        onValueChange = { customPropKey = it },
                        label = { Text("Key", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = customPropValue,
                        onValueChange = { customPropValue = it },
                        label = { Text("Value", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    IconButton(
                        onClick = {
                            if (customPropKey.isNotBlank()) {
                                onUpdateProp(customPropKey, customPropValue)
                                customPropKey = ""
                                customPropValue = ""
                            }
                        },
                        enabled = !isLoading && customPropKey.isNotBlank(),
                        modifier = Modifier.background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }

        // Existing Props
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Current System Properties", style = MaterialTheme.typography.titleSmall)
            props.forEach { prop ->
                PropRow(prop, onEdit = { editingProp = it })
            }
        }
    }

    // Edit Dialog
    editingProp?.let { prop ->
        var currentValue by remember { mutableStateOf(prop.value) }
        AlertDialog(
            onDismissRequest = { editingProp = null },
            title = { Text("Edit Property") },
            text = {
                Column {
                    Text(prop.key, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = currentValue,
                        onValueChange = { currentValue = it },
                        label = { Text("Value") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateProp(prop.key, currentValue)
                        editingProp = null
                    },
                    enabled = !isLoading
                ) {
                    Text("Update")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingProp = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun PartitionCard(partition: DiskPartition) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = partition.mountPoint,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = partition.filesystem,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Used", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    Text(partition.used, style = MaterialTheme.typography.bodyLarge)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Total Size", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    Text(partition.size, style = MaterialTheme.typography.bodyLarge)
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Percentage Calculation (simplified)
            val usedValue = partition.used.replace(Regex("[^0-9.]"), "").toFloatOrNull() ?: 0f
            val totalValue = partition.size.replace(Regex("[^0-9.]"), "").toFloatOrNull() ?: 1f
            val progress = if (totalValue > 0) (usedValue / totalValue).coerceIn(0f, 1f) else 0f
            
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp).background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(3.dp)),
                color = if (progress > 0.9f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${partition.available} available",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@Composable
private fun PropRow(prop: PropItem, onEdit: (PropItem) -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().clickable { onEdit(prop) }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = prop.key,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = prop.value,
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Default.Edit,
                contentDescription = "Edit",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            )
        }
    }
}
