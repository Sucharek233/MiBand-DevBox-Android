// Location: MainActivity.kt
package com.sucharek.miband_interconnect_test

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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

import com.sucharek.miband_interconnect_test.ui.screens.activities.files.FileExplorerScreen
import com.sucharek.miband_interconnect_test.ui.screens.activities.files.FileExplorerViewModel
import com.sucharek.miband_interconnect_test.ui.screens.activities.luashell.LuaShellScreen
import com.sucharek.miband_interconnect_test.ui.screens.activities.luashell.LuaShellViewModel
import com.sucharek.miband_interconnect_test.ui.screens.activities.moduleCompatibility.ModuleCompatibilityScreen
import com.sucharek.miband_interconnect_test.ui.screens.activities.moduleCompatibility.ModuleCompatibilityViewModel
import com.sucharek.miband_interconnect_test.ui.screens.activities.ping.PingScreen
import com.sucharek.miband_interconnect_test.ui.screens.activities.ping.PingViewModel
import com.sucharek.miband_interconnect_test.ui.screens.activities.qjsshell.QjsShellScreen
import com.sucharek.miband_interconnect_test.ui.screens.activities.qjsshell.QjsShellViewModel
import com.sucharek.miband_interconnect_test.ui.screens.activities.sensors.SensorScreen
import com.sucharek.miband_interconnect_test.ui.screens.activities.sensors.SensorViewModel
import com.sucharek.miband_interconnect_test.ui.screens.activities.sensors.infoScreen.SensorChartScreen
import com.sucharek.miband_interconnect_test.ui.screens.activities.terminal.TerminalScreen
import com.sucharek.miband_interconnect_test.ui.screens.activities.terminal.TerminalViewModel

import com.sucharek.miband_interconnect_test.ui.screens.deviceselection.DeviceSelectionScreen
import com.sucharek.miband_interconnect_test.ui.screens.maindashboard.MainDashboardScreen
import com.sucharek.miband_interconnect_test.ui.screens.maindashboard.WatchViewModel
import com.sucharek.miband_interconnect_test.ui.theme.Miband_interconnect_testTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Simple manual creation of DeviceManager to pass down.
        // (Later this can be handled via Dependency Injection like Hilt if preferred)
        val deviceManager = DeviceManager(applicationContext)

        setContent {
            Miband_interconnect_testTheme {
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
                        modifier = Modifier.padding(innerPadding)
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