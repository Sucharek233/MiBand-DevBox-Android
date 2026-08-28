package com.sucharek.miband_interconnect_test.ui.screens.activities.systemlogs

import androidx.lifecycle.ViewModel
import com.sucharek.miband_interconnect_test.models.SystemLogEntry
import com.sucharek.miband_interconnect_test.ui.screens.maindashboard.WatchViewModel
import kotlinx.coroutines.flow.StateFlow

class SystemLogsViewModel(
    private val globalWatchViewModel: WatchViewModel
) : ViewModel() {

    val logs: StateFlow<List<SystemLogEntry>> = globalWatchViewModel.systemLogEntries

    fun clearLogs() {
        globalWatchViewModel.clearLogs()
    }
}
