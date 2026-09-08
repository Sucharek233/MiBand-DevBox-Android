package com.sucharek.miband_interconnect_test.ui.screens.activities.apps

import com.sucharek.miband_interconnect_test.ui.screens.maindashboard.WatchViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import com.sucharek.miband_interconnect_test.models.MessageStates

/**
 * Centralized repository to handle caching and data synchronization for the Apps workspace.
 */
class AppsRepository(
    private val globalWatchViewModel: WatchViewModel,
    private val scope: CoroutineScope
) {
    private val _appList = MutableStateFlow<List<AppItem>>(emptyList())
    val appList: StateFlow<List<AppItem>> = _appList.asStateFlow()

    private val _appDetailsCache = MutableStateFlow<Map<String, AppDetails>>(emptyMap())
    val appDetailsCache: StateFlow<Map<String, AppDetails>> = _appDetailsCache.asStateFlow()

    private val _manifestCache = MutableStateFlow<Map<String, String>>(emptyMap())
    val manifestCache: StateFlow<Map<String, String>> = _manifestCache.asStateFlow()

    private val _isListLoading = MutableStateFlow(false)
    val isListLoading: StateFlow<Boolean> = _isListLoading.asStateFlow()

    // Operation Tracking
    private val _loadingInfo = MutableStateFlow<Set<String>>(emptySet())
    val loadingInfo: StateFlow<Set<String>> = _loadingInfo.asStateFlow()

    private val _loadingIcons = MutableStateFlow<Set<String>>(emptySet())
    val loadingIcons: StateFlow<Set<String>> = _loadingIcons.asStateFlow()

    private val _loadingManifests = MutableStateFlow<Set<String>>(emptySet())
    val loadingManifests: StateFlow<Set<String>> = _loadingManifests.asStateFlow()

    private val _saveEvents = MutableSharedFlow<Pair<String, Boolean>>(extraBufferCapacity = 1)
    val saveEvents: SharedFlow<Pair<String, Boolean>> = _saveEvents.asSharedFlow()

    private var lastRequestedIconPkg: String? = null
    private var lastRequestedManifestPkg: String? = null

    init {
        scope.launch {
            globalWatchViewModel.appsMessages.collectLatest { payload ->
                handleResponse(payload)
            }
        }

        scope.launch {
            globalWatchViewModel.mailboxBusyEvents.collectLatest {
                _isListLoading.value = false
                _loadingInfo.value = emptySet()
                _loadingIcons.value = emptySet()
                _loadingManifests.value = emptySet()
                // Reset all loading flags in list and details
                _appList.update { current -> current.map { it.copy(isIconLoading = false) } }
                _appDetailsCache.update { current -> 
                    current.mapValues { it.value.copy(isIconLoading = false) }
                }
            }
        }
    }

    fun fetchAppList(force: Boolean = false) {
        if (!force && _appList.value.isNotEmpty()) return

        _isListLoading.value = true
        globalWatchViewModel.sendStructuredMessage(
            type = "apps",
            args = JSONObject().apply { put("type", "listApps") }
        )
    }

    fun fetchDetails(packageName: String, force: Boolean = false) {
        if (!force && _appDetailsCache.value.containsKey(packageName)) return

        _loadingInfo.update { it + packageName }
        globalWatchViewModel.sendStructuredMessage(
            type = "apps",
            args = JSONObject().apply {
                put("type", "info")
                put("pkg", packageName)
            }
        )
    }

    fun fetchIcon(packageName: String) {
        lastRequestedIconPkg = packageName
        _loadingIcons.update { it + packageName }
        // Mark as loading in both list and cache
        _appList.update { list ->
            list.map { if (it.packageName == packageName) it.copy(isIconLoading = true) else it }
        }
        _appDetailsCache.update { cache ->
            val current = cache[packageName]
            if (current != null) {
                cache + (packageName to current.copy(isIconLoading = true))
            } else cache
        }

        globalWatchViewModel.sendStructuredMessage(
            type = "apps",
            args = JSONObject().apply {
                put("type", "icon")
                put("pkg", packageName)
            }
        )
    }

    fun fetchManifest(packageName: String, force: Boolean = false) {
        if (!force && _manifestCache.value.containsKey(packageName)) return

        lastRequestedManifestPkg = packageName
        _loadingManifests.update { it + packageName }
        globalWatchViewModel.sendStructuredMessage(
            type = "apps",
            args = JSONObject().apply {
                put("type", "manifest")
                put("pkg", packageName)
            }
        )
    }

    fun saveManifest(packageName: String, content: String) {
        globalWatchViewModel.sendStructuredMessage(
            type = "apps",
            args = JSONObject().apply {
                put("type", "writeManifest")
                put("pkg", packageName)
                put("content", content)
            }
        )
    }

    private fun handleResponse(payload: String) {
        try {
            val json = JSONObject(payload)
            val state = json.optString("state")
            val res = json.opt("res")

            // 1. List Result
            if (res is JSONObject && !res.has("package")) {
                val newList = mutableListOf<AppItem>()
                res.keys().forEach { pkg ->
                    // Check if we have an icon in cache already
                    val cachedIcon = _appDetailsCache.value[pkg]?.iconBase64 
                        ?: _appList.value.find { it.packageName == pkg }?.iconBase64
                    
                    newList.add(AppItem(
                        packageName = pkg, 
                        name = res.getString(pkg),
                        iconBase64 = cachedIcon
                    ))
                }
                _appList.value = newList.sortedBy { it.name.lowercase() }
                _isListLoading.value = false
            } 
            
            // 2. Info Result
            else if (res is JSONObject && res.has("package")) {
                val pkg = res.optString("package")
                _loadingInfo.update { it - pkg }
                val info = AppDetails(
                    packageName = pkg,
                    name = res.optJSONArray("names")?.optJSONObject(0)?.optString("value") ?: "",
                    versionCode = res.optInt("versionCode"),
                    versionName = res.optString("versionName"),
                    minAPILevel = res.optInt("minAPILevel"),
                    minPlatformVersion = res.optInt("minPlatformVersion"),
                    fingerprint = res.optString("fingerprint"),
                    installedTimestamp = res.optLong("installedTimestamp"),
                    iconPath = res.optString("icon"),
                    needNetwork = res.optBoolean("needNetwork"),
                    background = res.optBoolean("background"),
                    standalone = res.optBoolean("standalone"),
                    rawJson = res.toString(2)
                )

                _appDetailsCache.update { cache ->
                    val existing = cache[pkg]
                    // Preserve icon if we have it
                    val updated = if (existing != null) {
                        info.copy(iconBase64 = existing.iconBase64, isIconLoading = existing.isIconLoading)
                    } else {
                        // Also check app list for icon
                        val listIcon = _appList.value.find { it.packageName == pkg }?.iconBase64
                        info.copy(iconBase64 = listIcon)
                    }
                    cache + (pkg to updated)
                }
            }
            
            // 3. Status based results (Icon, Manifest, Write)
            else if (state == MessageStates.DONE) {
                if (res is String) {
                    if (res == "Written") {
                        val pkg = lastRequestedManifestPkg
                        if (pkg != null) {
                            scope.launch { _saveEvents.emit(pkg to true) }
                        }
                    } else if (res.length > 100 && !res.trim().startsWith("{") && !res.trim().startsWith("[")) {
                        // Base64 icon
                        val pkg = lastRequestedIconPkg
                        if (pkg != null) {
                            _loadingIcons.update { it - pkg }
                            _appList.update { list ->
                                list.map { if (it.packageName == pkg) it.copy(iconBase64 = res, isIconLoading = false) else it }
                            }
                            _appDetailsCache.update { cache ->
                                val current = cache[pkg]
                                if (current != null) {
                                    cache + (pkg to current.copy(iconBase64 = res, isIconLoading = false))
                                } else cache
                            }
                        }
                    } else {
                        // Likely manifest content
                        val pkg = lastRequestedManifestPkg
                        if (pkg != null) {
                            _loadingManifests.update { it - pkg }
                            // Pretty print manifest
                            val trimmedRes = res.trim()
                            val formatted = runCatching { 
                                if (trimmedRes.startsWith("{") || trimmedRes.startsWith("[")) {
                                    JSONObject(trimmedRes).toString(2)
                                } else res
                            }.getOrDefault(res)
                            
                            _manifestCache.update { it + (pkg to formatted) }
                        }
                    }
                }
            } else if (state == MessageStates.ERROR) {
                _isListLoading.value = false
                _loadingInfo.update { emptySet() }
                _loadingIcons.update { emptySet() }
                _loadingManifests.update { emptySet() }
                val pkg = lastRequestedManifestPkg
                if (pkg != null) {
                    scope.launch { _saveEvents.emit(pkg to false) }
                }
            }
        } catch (e: Exception) {
            _isListLoading.value = false
            _loadingInfo.update { emptySet() }
            _loadingIcons.update { emptySet() }
            _loadingManifests.update { emptySet() }
        }
    }

    fun updateManifestCache(packageName: String, content: String) {
        _manifestCache.update { it + (packageName to content) }
    }
}
