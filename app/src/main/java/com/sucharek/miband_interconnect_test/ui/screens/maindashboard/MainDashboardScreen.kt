package com.sucharek.miband_interconnect_test.ui.screens.maindashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Javascript
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sucharek.miband_interconnect_test.ui.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboardScreen(
    viewModel: WatchViewModel,
    onNavigateToCategory: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val connectionState by viewModel.connectionState.collectAsState()
    val isAppInstalled by viewModel.isWatchAppInstalled.collectAsState()

    var selectedService by rememberSaveable { mutableStateOf(ServiceType.QUICKJS) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("MiBand Interconnect", style = MaterialTheme.typography.titleLarge)

                        when (val state = connectionState) {
                            is WatchConnectionState.Connected -> {
                                Text(
                                    text = "Connected to: ${state.nodeName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            else -> {
                                Text("No device paired", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                },
                actions = {
                    if (isAppInstalled == true) {
                        TextButton(
                            onClick = { viewModel.launchWatchApp(context) }
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Launch App",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedService == ServiceType.QUICKJS,
                    onClick = { selectedService = ServiceType.QUICKJS },
                    icon = { Icon(Icons.Default.Javascript, contentDescription = null) },
                    label = { Text("QuickJS") }
                )
                NavigationBarItem(
                    selected = selectedService == ServiceType.LUA,
                    onClick = { selectedService = ServiceType.LUA },
                    icon = { Icon(Icons.Default.Terminal, contentDescription = null) },
                    label = { Text("Lua") }
                )
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {

            if (isAppInstalled == false) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Column {
                            Text(
                                text = "Watch App Missing",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "Please install the companion module via your watch settings to enable interactive commands.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = { viewModel.checkCompanionApp(context) },
                        modifier = Modifier.align(Alignment.End),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Text("Check Again")
                    }
                }
            }

            val commonCategories = remember {
                DashboardCategory.entries.filter { it.serviceType == null }
            }
            val serviceCategories = remember(selectedService) {
                DashboardCategory.entries.filter { it.serviceType == selectedService }
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    Text(
                        text = "Management",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                items(commonCategories) { category ->
                    DashboardCategoryCard(
                        category = category,
                        selectedService = selectedService,
                        onNavigate = onNavigateToCategory
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = selectedService.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                items(serviceCategories) { category ->
                    DashboardCategoryCard(
                        category = category,
                        selectedService = selectedService,
                        onNavigate = onNavigateToCategory
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardCategoryCard(
    category: DashboardCategory,
    selectedService: ServiceType,
    onNavigate: (Screen) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                when (category) {
                    DashboardCategory.TERMINAL -> onNavigate(Screen.RemoteTerminal)
                    DashboardCategory.LUASHELL -> onNavigate(Screen.LuaShell)
                    DashboardCategory.LUASYSINFO -> onNavigate(Screen.LuaSysInfo)
                    DashboardCategory.FILES -> onNavigate(Screen.FileExplorer())
                    DashboardCategory.PING -> onNavigate(Screen.Ping(initialType = if (selectedService == ServiceType.QUICKJS) "qjs" else "lua"))
                    DashboardCategory.QJS -> onNavigate(Screen.QjsShell)
                    DashboardCategory.MODULES -> onNavigate(Screen.ModuleCompatibility)
                    DashboardCategory.DEVICE -> onNavigate(Screen.DeviceInfo)
                    DashboardCategory.SENSORS -> onNavigate(Screen.Sensors)
                    DashboardCategory.LUASENSORS -> onNavigate(Screen.LuaSensors)
                    DashboardCategory.APPS -> onNavigate(Screen.Apps)
                    DashboardCategory.SYSTEMLOGS -> onNavigate(Screen.SystemLogs)
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = category.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = category.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
