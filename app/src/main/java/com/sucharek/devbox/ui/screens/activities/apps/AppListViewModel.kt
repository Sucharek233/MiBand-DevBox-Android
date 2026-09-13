package com.sucharek.devbox.ui.screens.activities.apps

import androidx.lifecycle.ViewModel
import com.sucharek.devbox.ui.screens.maindashboard.WatchViewModel
import kotlinx.coroutines.flow.StateFlow

class AppListViewModel(
    private val globalWatchViewModel: WatchViewModel
) : ViewModel() {

    private val repository = globalWatchViewModel.appsRepository
    
    val appList: StateFlow<List<AppItem>> = repository.appList
    val isLoading: StateFlow<Boolean> = repository.isListLoading
    val isAnyAppOperationActive: StateFlow<Boolean> = repository.isAnyAppOperationActive

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
