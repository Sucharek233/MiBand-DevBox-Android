package com.sucharek.miband_interconnect_test.ui.screens.activities.files

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharek.miband_interconnect_test.ui.screens.maindashboard.WatchViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class FileExplorerViewModel(
    private val globalWatchViewModel: WatchViewModel,
    initialPath: String
) : ViewModel() {

    private val _currentPath = MutableStateFlow(initialPath)
    val currentPath: StateFlow<String> = _currentPath.asStateFlow()

    private val _filesList = MutableStateFlow<List<FileItem>>(emptyList())
    val filesList: StateFlow<List<FileItem>> = _filesList.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val directoryCache = mutableMapOf<String, List<FileItem>>()

    init {
        viewModelScope.launch {
            globalWatchViewModel.ioMessages.collectLatest { rawJsonEnvelopeString ->
                try {
                    val baseJson = JSONObject(rawJsonEnvelopeString)
                    val state = baseJson.optString("state")
                    if (state == "error") {
                        _isLoading.value = false
                        return@collectLatest
                    }

                    val resultJson = baseJson.getJSONObject("res")
                    
                    // Try to get the path from the response if available, otherwise fallback to currentPath
                    val responsePath = resultJson.optString("path", _currentPath.value)
                    
                    val ioResult = resultJson.getJSONObject("result")

                    val parsedItems = mutableListOf<FileItem>()

                    // 1. Safe Folder Parsing
                    val foldersArray = ioResult.optJSONArray("folders")
                    if (foldersArray != null) {
                        for (i in 0 until foldersArray.length()) {
                            parsedItems.add(
                                FileItem(name = foldersArray.getString(i), isDirectory = true)
                            )
                        }
                    }

                    // 2. Bulletproof File Parsing
                    // optJSONObject returns null instead of crashing if "files" is missing or is an empty array []
                    val filesObject = ioResult.optJSONObject("files")
                    if (filesObject != null) {
                        val keys = filesObject.keys()
                        while (keys.hasNext()) {
                            val fileName = keys.next()

                            // Safe extraction of the inner attributes object
                            val attributes = filesObject.optJSONObject(fileName)
                            val sizeValue = if (attributes != null && attributes.has("size")) {
                                attributes.getLong("size")
                            } else {
                                -1L
                            }

                            parsedItems.add(
                                FileItem(
                                    name = fileName,
                                    isDirectory = false,
                                    sizeBytes = sizeValue
                                )
                            )
                        }
                    }

                    // 3. Sort structural content cleanly
                    val sortedList = parsedItems.sortedWith(
                        compareBy({ !it.isDirectory }, { it.name.lowercase() })
                    )
                    
                    directoryCache[responsePath] = sortedList
                    
                    // Only update the UI if the response is for the current path
                    if (responsePath == _currentPath.value) {
                        _filesList.value = sortedList
                        _isLoading.value = false
                    }

                } catch (e: Exception) {
                    // Emits blank canvas layout if data payload breaks parsing logic rules
                    _filesList.value = emptyList()
                    _isLoading.value = false
                }
            }
        }

        viewModelScope.launch {
            globalWatchViewModel.mailboxBusyEvents.collectLatest {
                _isLoading.value = false
            }
        }

        requestDirectoryListing(_currentPath.value)
    }

    fun requestDirectoryListing(path: String, forceRefresh: Boolean = false) {
        if (!forceRefresh && directoryCache.containsKey(path)) {
            _currentPath.value = path
            _filesList.value = directoryCache[path]!!
            _isLoading.value = false
            return
        }

        _isLoading.value = true
        _currentPath.value = path

        val fileArgs = JSONObject().apply {
            put("path", path)
            put("type", "list")
        }

        globalWatchViewModel.sendStructuredMessage(
            type = "io",
            args = fileArgs
        )
    }

    fun refresh() {
        requestDirectoryListing(_currentPath.value, forceRefresh = true)
    }

    fun navigateUp() {
        val current = _currentPath.value
        if (current == "/" || current.isBlank()) return

        // Simple string utility manipulation to drop the last sub-directory item
        val parentPath = current.substringBeforeLast("/").ifEmpty { "/" }
        requestDirectoryListing(parentPath)
    }
}