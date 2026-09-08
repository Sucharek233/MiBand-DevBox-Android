package com.sucharek.miband_interconnect_test.ui.screens.activities.apps

import androidx.lifecycle.ViewModel
import com.sucharek.miband_interconnect_test.ui.screens.maindashboard.WatchViewModel
import kotlinx.coroutines.flow.StateFlow

class AppListViewModel(
    private val globalWatchViewModel: WatchViewModel
) : ViewModel() {

    private val repository = globalWatchViewModel.appsRepository
    
    val appList: StateFlow<List<AppItem>> = repository.appList
    val isLoading: StateFlow<Boolean> = repository.isListLoading
    val isAnyAppOperationActive: StateFlow<Boolean> = repository.isAnyAppOperationActive
    val isAnyIconLoading: StateFlow<Boolean> = repository.isAnyIconLoading

    init {
        repository.fetchAppList(force = false)
    }

    fun fetchAppList() {
        repository.fetchAppList(force = true)
    }

    fun fetchIcon(packageName: String) {
        repository.fetchIcon(packageName)
    }
}
