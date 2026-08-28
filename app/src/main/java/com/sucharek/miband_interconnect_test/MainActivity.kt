// Location: MainActivity.kt
package com.sucharek.miband_interconnect_test

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.sucharek.miband_interconnect_test.interconnect.DeviceManager
import com.sucharek.miband_interconnect_test.ui.navigation.Screen

import com.sucharek.miband_interconnect_test.ui.screens.activities.apps.AppDetailScreen
import com.sucharek.miband_interconnect_test.ui.screens.activities.apps.AppDetailViewModel
import com.sucharek.miband_interconnect_test.ui.screens.activities.apps.AppListScreen
import com.sucharek.miband_interconnect_test.ui.screens.activities.apps.AppListViewModel
import com.sucharek.miband_interconnect_test.ui.screens.activities.apps.AppManifestScreen
import com.sucharek.miband_interconnect_test.ui.screens.activities.apps.AppManifestViewModel
import com.sucharek.miband_interconnect_test.ui.screens.activities.device.DeviceScreen
import com.sucharek.miband_interconnect_test.ui.screens.activities.device.DeviceViewModel
import com.sucharek.miband_interconnect_test.ui.screens.activities.files.FileExplorerScreen
import com.sucharek.miband_interconnect_test.ui.screens.activities.files.FileExplorerViewModel
import com.sucharek.miband_interconnect_test.ui.screens.activities.luashell.LuaShellScreen
import com.sucharek.miband_interconnect_test.ui.screens.activities.luashell.LuaShellViewModel
import com.sucharek.miband_interconnect_test.ui.screens.activities.luaSensors.LuaSensorScreen
import com.sucharek.miband_interconnect_test.ui.screens.activities.luaSensors.LuaSensorViewModel
import com.sucharek.miband_interconnect_test.ui.screens.activities.moduleCompatibility.ModuleCompatibilityScreen
import com.sucharek.miband_interconnect_test.ui.screens.activities.moduleCompatibility.ModuleCompatibilityViewModel
import com.sucharek.miband_interconnect_test.ui.screens.activities.ping.PingScreen
import com.sucharek.miband_interconnect_test.ui.screens.activities.ping.PingViewModel
import com.sucharek.miband_interconnect_test.ui.screens.activities.qjsshell.QjsShellScreen
import com.sucharek.miband_interconnect_test.ui.screens.activities.qjsshell.QjsShellViewModel
import com.sucharek.miband_interconnect_test.ui.screens.activities.sensors.SensorScreen
import com.sucharek.miband_interconnect_test.ui.screens.activities.sensors.SensorViewModel
import com.sucharek.miband_interconnect_test.ui.screens.activities.sensors.infoScreen.SensorChartScreen
import com.sucharek.miband_interconnect_test.ui.screens.activities.systemlogs.SystemLogsScreen
import com.sucharek.miband_interconnect_test.ui.screens.activities.systemlogs.SystemLogsViewModel
import com.sucharek.miband_interconnect_test.ui.screens.activities.terminal.TerminalScreen
import com.sucharek.miband_interconnect_test.ui.screens.activities.terminal.TerminalViewModel

import com.sucharek.miband_interconnect_test.ui.screens.deviceselection.DeviceSelectionScreen
import com.sucharek.miband_interconnect_test.ui.screens.maindashboard.MainDashboardScreen
import com.sucharek.miband_interconnect_test.ui.screens.maindashboard.WatchViewModel
import com.sucharek.miband_interconnect_test.ui.theme.Miband_interconnect_testTheme
import dev.hossain.highlight.ui.HighlightThemeProvider

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Simple manual creation of DeviceManager to pass down.
        // (Later this can be handled via Dependency Injection like Hilt if preferred)
        val deviceManager = DeviceManager(applicationContext)

        setContent {
            Miband_interconnect_testTheme {
                HighlightThemeProvider {
                    val navController = rememberNavController()

                    // By instantiating the ViewModel here at the graph root, 
                    // it persists cleanly across screen swaps.
                    val watchViewModel: WatchViewModel = viewModel {
                        WatchViewModel(deviceManager)
                    }

                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = Screen.DeviceSelection,
                            modifier = Modifier.padding(innerPadding),
                            enterTransition = { fadeIn(animationSpec = tween(150)) },
                            exitTransition = { fadeOut(animationSpec = tween(150)) },
                            popEnterTransition = { fadeIn(animationSpec = tween(150)) },
                            popExitTransition = { fadeOut(animationSpec = tween(150)) }
                        ) {
                            composable<Screen.DeviceSelection> {
                                DeviceSelectionScreen(
                                    viewModel = watchViewModel,
                                    onDeviceSelected = { selectedNode ->
                                        watchViewModel.connectToDevice(selectedNode, applicationContext)
                                        navController.navigate(Screen.MainDashboard) {
                                            popUpTo<Screen.DeviceSelection> { inclusive = true }
                                        }
                                    }
                                )
                            }

                            composable<Screen.MainDashboard> {
                                MainDashboardScreen(
                                    viewModel = watchViewModel,
                                    onNavigateToCategory = { categoryScreen ->
                                        navController.navigate(categoryScreen)
                                    }
                                )
                            }

                            // Terminal
                            composable<Screen.RemoteTerminal> {
                                val activity = LocalActivity.current as ComponentActivity
                                val terminalViewModel: TerminalViewModel = viewModel(viewModelStoreOwner = activity) {
                                    TerminalViewModel(watchViewModel)
                                }
                                TerminalScreen(viewModel = terminalViewModel)
                            }

                            // File explorer
                            composable<Screen.FileExplorer> {
                                val activity = LocalActivity.current as ComponentActivity
                                val explorerViewModel: FileExplorerViewModel = viewModel(viewModelStoreOwner = activity) {
                                    FileExplorerViewModel(watchViewModel, initialPath = "/")
                                }
                                FileExplorerScreen(viewModel = explorerViewModel)
                            }

                            // JS shell
                            composable<Screen.QjsShell> {
                                val activity = LocalActivity.current as ComponentActivity
                                val jsViewModel: QjsShellViewModel = viewModel(viewModelStoreOwner = activity) {
                                    QjsShellViewModel(watchViewModel)
                                }
                                QjsShellScreen(viewModel = jsViewModel)
                            }

                            // Device Info
                            composable<Screen.DeviceInfo> {
                                val activity = LocalActivity.current as ComponentActivity
                                val deviceViewModel: DeviceViewModel = viewModel(viewModelStoreOwner = activity) {
                                    DeviceViewModel(watchViewModel)
                                }
                                DeviceScreen(viewModel = deviceViewModel)
                            }

                            // Lua shell
                            composable<Screen.LuaShell> {
                                val activity = LocalActivity.current as ComponentActivity
                                val luaViewModel: LuaShellViewModel = viewModel(viewModelStoreOwner = activity) {
                                    LuaShellViewModel(watchViewModel)
                                }
                                LuaShellScreen(viewModel = luaViewModel)
                            }

                            // Module compatibility
                            composable<Screen.ModuleCompatibility> {
                                val activity = LocalActivity.current as ComponentActivity
                                val modulesViewModel: ModuleCompatibilityViewModel = viewModel(viewModelStoreOwner = activity) {
                                    ModuleCompatibilityViewModel(watchViewModel)
                                }
                                ModuleCompatibilityScreen(viewModel = modulesViewModel)
                            }

                            // Sensors
                            composable<Screen.Sensors> {
                                val activity = LocalActivity.current as ComponentActivity
                                val sensorsViewModel: SensorViewModel = viewModel(viewModelStoreOwner = activity) {
                                    SensorViewModel(watchViewModel)
                                }
                                SensorScreen(
                                    viewModel = sensorsViewModel,
                                    onSensorClick = { sensorName ->
                                        navController.navigate(Screen.SensorChart(sensorName))
                                    }
                                )
                            }

                            composable<Screen.SystemLogs> {
                                val activity = LocalActivity.current as ComponentActivity
                                val logsViewModel: SystemLogsViewModel = viewModel(viewModelStoreOwner = activity) {
                                    SystemLogsViewModel(watchViewModel)
                                }
                                SystemLogsScreen(viewModel = logsViewModel)
                            }

                            composable<Screen.LuaSensors> {
                                val activity = LocalActivity.current as ComponentActivity
                                val sensorsViewModel: LuaSensorViewModel = viewModel(viewModelStoreOwner = activity) {
                                    LuaSensorViewModel(watchViewModel)
                                }
                                LuaSensorScreen(
                                    viewModel = sensorsViewModel,
                                    onBack = { navController.popBackStack() },
                                    onSensorSubscribed = { sensorName ->
                                        navController.navigate(Screen.LuaSensorChart(sensorName))
                                    }
                                )
                            }

                            // Apps
                            composable<Screen.Apps> {
                                val activity = LocalActivity.current as ComponentActivity
                                val appsViewModel: AppListViewModel = viewModel(viewModelStoreOwner = activity) {
                                    AppListViewModel(watchViewModel)
                                }
                                AppListScreen(
                                    viewModel = appsViewModel,
                                    onAppClick = { app ->
                                        navController.navigate(Screen.AppDetail(app.packageName, app.name))
                                    }
                                )
                            }

                            composable<Screen.AppDetail> { backStackEntry ->
                                val appRoute = backStackEntry.toRoute<Screen.AppDetail>()
                                val detailViewModel: AppDetailViewModel = viewModel {
                                    AppDetailViewModel(watchViewModel, appRoute.packageName)
                                }
                                AppDetailScreen(
                                    viewModel = detailViewModel,
                                    appName = appRoute.appName,
                                    onEditManifest = {
                                        navController.navigate(Screen.AppManifest(appRoute.packageName))
                                    }
                                )
                            }

                            composable<Screen.AppManifest> { backStackEntry ->
                                val appRoute = backStackEntry.toRoute<Screen.AppManifest>()
                                val manifestViewModel: AppManifestViewModel = viewModel {
                                    AppManifestViewModel(watchViewModel, appRoute.packageName)
                                }
                                AppManifestScreen(viewModel = manifestViewModel)
                            }

                            composable<Screen.LuaSensorChart> { backStackEntry ->
                                val sensorChart = backStackEntry.toRoute<Screen.LuaSensorChart>()
                                val activity = LocalActivity.current as ComponentActivity
                                val sensorsViewModel: LuaSensorViewModel = viewModel(viewModelStoreOwner = activity) {
                                    LuaSensorViewModel(watchViewModel)
                                }
                                SensorChartScreen(
                                    viewModel = sensorsViewModel,
                                    sensorName = sensorChart.sensorName,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable<Screen.SensorChart> { backStackEntry ->
                                val sensorChart = backStackEntry.toRoute<Screen.SensorChart>()
                                val activity = LocalActivity.current as ComponentActivity
                                val sensorsViewModel: SensorViewModel = viewModel(viewModelStoreOwner = activity) {
                                    SensorViewModel(watchViewModel)
                                }
                                SensorChartScreen(
                                    viewModel = sensorsViewModel,
                                    sensorName = sensorChart.sensorName,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable<Screen.Ping> {
                                val activity = LocalActivity.current as ComponentActivity
                                val pingViewModel: PingViewModel = viewModel(viewModelStoreOwner = activity) {
                                    PingViewModel(watchViewModel)
                                }
                                PingScreen(
                                    viewModel = pingViewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
