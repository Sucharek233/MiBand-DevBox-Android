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

class AppManifestViewModel(
    private val globalWatchViewModel: WatchViewModel,
    private val packageName: String
) : ViewModel() {

    private val repository = globalWatchViewModel.appsRepository

    val manifestContent: StateFlow<String> = repository.manifestCache.map { it[packageName] ?: "" }
        .stateIn(viewModelScope, SharingStarted.Eagerly, repository.manifestCache.value[packageName] ?: "")

    // Derive loading state from repository
    val isLoading: StateFlow<Boolean> = repository.loadingManifests.map { it.contains(packageName) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, repository.loadingManifests.value.contains(packageName))

    val isAnyAppOperationActive: StateFlow<Boolean> = repository.isAnyAppOperationActive

    private val _saveStatus = MutableStateFlow<String?>(null) // null, "Saving...", "Saved", "Error"
    val saveStatus: StateFlow<String?> = _saveStatus.asStateFlow()

    init {
        viewModelScope.launch {
            repository.saveEvents.collectLatest { (pkg, success) ->
                if (pkg == packageName) {
                    _saveStatus.value = if (success) "Saved" else "Error"
                }
            }
        }

        viewModelScope.launch {
            globalWatchViewModel.mailboxBusyEvents.collectLatest {
                if (_saveStatus.value == "Saving...") _saveStatus.value = "Error (Busy)"
            }
        }

        fetchManifest()
    }

    fun fetchManifest(force: Boolean = false) {
        if (!force && repository.manifestCache.value.containsKey(packageName)) return
        repository.fetchManifest(packageName, force = force)
    }

    fun refresh() {
        fetchManifest(force = true)
    }

    fun saveManifest(newContent: String) {
        _saveStatus.value = "Saving..."
        repository.saveManifest(packageName, newContent)
    }

    fun clearSaveStatus() {
        _saveStatus.value = null
    }
}
