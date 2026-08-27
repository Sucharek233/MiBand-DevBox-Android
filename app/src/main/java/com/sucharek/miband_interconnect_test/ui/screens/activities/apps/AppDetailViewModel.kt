package com.sucharek.miband_interconnect_test.ui.screens.activities.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharek.miband_interconnect_test.ui.screens.maindashboard.WatchViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppDetailViewModel(
    private val globalWatchViewModel: WatchViewModel,
    private val packageName: String
) : ViewModel() {

    private val repository = globalWatchViewModel.appsRepository

    val details: StateFlow<AppDetails?> = repository.appDetailsCache.map { it[packageName] }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), repository.appDetailsCache.value[packageName])

    // Derive loading state from repository
    val isLoading: StateFlow<Boolean> = repository.loadingInfo.map { it.contains(packageName) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, repository.loadingInfo.value.contains(packageName))

    // Map rawJson directly from details flow
    val rawJson: StateFlow<String?> = details.map { it?.rawJson }
        .stateIn(viewModelScope, SharingStarted.Eagerly, details.value?.rawJson)

    init {
        viewModelScope.launch {
            globalWatchViewModel.mailboxBusyEvents.collectLatest {
                // Repository handles the set clearance
            }
        }
        
        // Ensure data is fetched if not in cache
        fetchDetails(force = false)
    }

    fun fetchDetails(force: Boolean = false) {
        if (!force && repository.appDetailsCache.value.containsKey(packageName)) return
        repository.fetchDetails(packageName, force = force)
    }

    fun fetchIcon() {
        repository.fetchIcon(packageName)
    }
}
