package com.sucharek.miband_interconnect_test.ui.screens.activities.files

import android.app.Application
import android.net.Uri
import android.util.Base64
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharek.miband_interconnect_test.ui.screens.maindashboard.WatchViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import com.sucharek.miband_interconnect_test.models.MessageStates
import java.io.OutputStream

sealed class DownloadState {
    object Idle : DownloadState()
    data class RequestFolder(val item: FileItem) : DownloadState()
    data class Configure(val item: FileItem) : DownloadState()
    data class Connecting(val fileName: String) : DownloadState()
    data class Progress(
        val fileName: String,
        val overallBytes: Long,
        val totalBytes: Long,
        val luaPreparedBytes: Long,
        val jsChunkBytes: Long,
        val jsChunkTotal: Long
    ) : DownloadState()
    data class Success(val fileName: String, val localPath: String) : DownloadState()
    data class Error(val fileName: String, val message: String) : DownloadState()
}

class FileExplorerViewModel(
    private val globalWatchViewModel: WatchViewModel,
    private val application: Application,
    initialPath: String
) : ViewModel() {

    private val _currentPath = MutableStateFlow(initialPath)
    val currentPath: StateFlow<String> = _currentPath.asStateFlow()

    private val _filesList = MutableStateFlow<List<FileItem>>(emptyList())
    val filesList: StateFlow<List<FileItem>> = _filesList.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _operationStatus = MutableSharedFlow<String>()
    val operationStatus: SharedFlow<String> = _operationStatus.asSharedFlow()

    private val _activeDownload = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val activeDownload: StateFlow<DownloadState> = _activeDownload.asStateFlow()

    private val directoryCache = mutableMapOf<String, List<FileItem>>()
    
    private var downloadOutputStream: OutputStream? = null
    private var downloadItem: FileItem? = null
    private var downloadDirectoryUri: Uri? = null
    
    private var bytesDownloadedSoFar = 0L
    private var totalFileSize = 0L
    private var luaPreparedBytes = 0L
    private var lastLuaPos = 0L
    private var currentLuaChunkSize = 0L
    private var jsBytesInCurrentLuaChunk = 0L

    init {
        viewModelScope.launch {
            globalWatchViewModel.ioMessages.collectLatest { rawJsonEnvelopeString ->
                handleIoMessage(rawJsonEnvelopeString)
            }
        }

        viewModelScope.launch {
            globalWatchViewModel.mailboxBusyEvents.collectLatest {
                _isLoading.value = false
            }
        }

        requestDirectoryListing(_currentPath.value)
    }

    private fun handleIoMessage(rawJsonEnvelopeString: String) {
        try {
            val baseJson = JSONObject(rawJsonEnvelopeString)
            val state = baseJson.optString("state")
            val appState = baseJson.optString("appState", state)
            val res = baseJson.opt("res")

            if (state == MessageStates.ERROR) {
                handleGlobalError(baseJson.optString("msg", "Unknown transport error"))
                return
            }

            if (state == MessageStates.STREAM) {
                handleDownloadChunk(res, baseJson.optJSONObject("meta"))
                return
            }

            when (appState) {
                MessageStates.DONE -> handleOperationDone(res)
                MessageStates.ERROR -> handleOperationError(res?.toString() ?: "Unknown Lua error")
            }

        } catch (e: Exception) {
            e.printStackTrace()
            _isLoading.value = false
        }
    }

    private fun handleGlobalError(msg: String) {
        _isLoading.value = false
        viewModelScope.launch { _operationStatus.emit("Error: $msg") }
        val current = _activeDownload.value
        if (current !is DownloadState.Idle && current !is DownloadState.RequestFolder && current !is DownloadState.Configure) {
            val fileName = downloadItem?.name ?: "Unknown"
            _activeDownload.value = DownloadState.Error(fileName, msg)
            cleanupDownload()
        }
    }

    private fun handleOperationDone(res: Any?) {
        _isLoading.value = false
        
        if (_activeDownload.value is DownloadState.Progress) {
            val fileName = downloadItem?.name ?: "file"
            _activeDownload.value = DownloadState.Success(fileName, "Selected folder")
            cleanupDownload()
            return
        }

        if (res is JSONObject) {
            val hasListing = res.has("folders") || res.has("files") || res.has("result")
            if (hasListing) {
                parseAndShowDirectoryListing(res)
                return
            }
            
            if (res.has("fileSize")) {
                initializeDownload(res)
                return
            }
        }

        viewModelScope.launch { _operationStatus.emit("Operation successful") }
        refresh()
    }

    private fun handleOperationError(error: String) {
        _isLoading.value = false
        viewModelScope.launch { _operationStatus.emit("Error: $error") }
        
        val current = _activeDownload.value
        if (current !is DownloadState.Idle && current !is DownloadState.RequestFolder && current !is DownloadState.Configure) {
            val fileName = downloadItem?.name ?: "Unknown"
            _activeDownload.value = DownloadState.Error(fileName, error)
            cleanupDownload()
        }
    }

    private fun parseAndShowDirectoryListing(res: JSONObject) {
        val responsePath = res.optString("path", _currentPath.value)
        
        val ioResult = if (res.has("folders") || res.has("files")) {
            res
        } else {
            res.optJSONObject("result") ?: return
        }
        
        val parsedItems = mutableListOf<FileItem>()

        val foldersArray = ioResult.optJSONArray("folders")
        if (foldersArray != null) {
            for (i in 0 until foldersArray.length()) {
                parsedItems.add(FileItem(name = foldersArray.getString(i), isDirectory = true, parentPath = responsePath))
            }
        }

        val filesObject = ioResult.optJSONObject("files")
        if (filesObject != null) {
            val keys = filesObject.keys()
            while (keys.hasNext()) {
                val fileName = keys.next()
                val attributes = filesObject.optJSONObject(fileName)
                val sizeValue = attributes?.optLong("size", -1L) ?: -1L
                parsedItems.add(FileItem(name = fileName, isDirectory = false, sizeBytes = sizeValue, parentPath = responsePath))
            }
        }

        val sortedList = parsedItems.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
        directoryCache[responsePath] = sortedList

        if (responsePath == _currentPath.value) {
            _filesList.value = sortedList
        }
    }

    // --- File Operations ---

    fun requestDirectoryListing(path: String, forceRefresh: Boolean = false) {
        if (!forceRefresh && directoryCache.containsKey(path)) {
            _currentPath.value = path
            _filesList.value = directoryCache[path]!!
            _isLoading.value = false
            return
        }

        _isLoading.value = true
        _currentPath.value = path
        sendIoRequest(JSONObject().apply {
            put("type", "list")
            put("path", path)
        })
    }

    fun refresh() {
        requestDirectoryListing(_currentPath.value, forceRefresh = true)
    }

    fun copyFile(src: String, dst: String) {
        _isLoading.value = true
        sendIoRequest(JSONObject().apply {
            put("type", "cp")
            put("src", src)
            put("dst", dst)
        })
    }

    fun moveFile(src: String, dst: String) {
        _isLoading.value = true
        sendIoRequest(JSONObject().apply {
            put("type", "mv")
            put("src", src)
            put("dst", dst)
        })
    }

    fun deleteFile(path: String) {
        _isLoading.value = true
        sendIoRequest(JSONObject().apply {
            put("type", "rm")
            put("path", path)
        })
    }

    // --- Streaming / Download ---

    fun onDownloadClick(item: FileItem) {
        if (_activeDownload.value !is DownloadState.Idle) return
        
        if (downloadDirectoryUri == null) {
            _activeDownload.value = DownloadState.RequestFolder(item)
        } else {
            _activeDownload.value = DownloadState.Configure(item)
        }
    }

    fun onFolderSelected(uri: Uri) {
        downloadDirectoryUri = uri
        val current = _activeDownload.value
        if (current is DownloadState.RequestFolder) {
            _activeDownload.value = DownloadState.Configure(current.item)
        }
    }

    fun startDownload(item: FileItem, luaChunkSizeKB: Int, jsChunkSizeKB: Int) {
        downloadItem = item
        _activeDownload.value = DownloadState.Connecting(item.name)
        
        sendIoRequest(JSONObject().apply {
            put("type", "getStream")
            put("path", item.fullPath)
            put("lSize", luaChunkSizeKB * 1024)
            put("qSize", jsChunkSizeKB * 1024)
        })
    }

    private fun initializeDownload(res: JSONObject) {
        totalFileSize = res.optLong("fileSize")
        val fileName = downloadItem?.name ?: "downloaded_file"
        val directoryUri = downloadDirectoryUri ?: return
        
        try {
            val root = DocumentFile.fromTreeUri(application, directoryUri)
            val file = root?.createFile("*/*", fileName)
            val uri = file?.uri ?: throw Exception("Failed to create file")
            
            downloadOutputStream = application.contentResolver.openOutputStream(uri)
            
            bytesDownloadedSoFar = 0L
            luaPreparedBytes = 0L
            lastLuaPos = 0L
            currentLuaChunkSize = 0L
            jsBytesInCurrentLuaChunk = 0L

            _activeDownload.value = DownloadState.Progress(
                fileName = fileName,
                overallBytes = 0L,
                totalBytes = totalFileSize,
                luaPreparedBytes = 0L,
                jsChunkBytes = 0L,
                jsChunkTotal = 0L
            )
            requestNextChunk()
        } catch (e: Exception) {
            handleOperationError("Failed to create local file: ${e.message}")
        }
    }

    private fun handleDownloadChunk(res: Any?, meta: JSONObject?) {
        val dataBase64 = res?.toString() ?: return
        val item = downloadItem ?: return
        
        try {
            val bytes = Base64.decode(dataBase64, Base64.DEFAULT)
            downloadOutputStream?.write(bytes)
            bytesDownloadedSoFar += bytes.size
            jsBytesInCurrentLuaChunk += bytes.size
            
            if (meta != null) {
                lastLuaPos = luaPreparedBytes
                luaPreparedBytes = meta.optLong("currPos", luaPreparedBytes)
                currentLuaChunkSize = luaPreparedBytes - lastLuaPos
                jsBytesInCurrentLuaChunk = bytes.size.toLong()
            }

            _activeDownload.value = DownloadState.Progress(
                fileName = item.name,
                overallBytes = bytesDownloadedSoFar,
                totalBytes = totalFileSize,
                luaPreparedBytes = luaPreparedBytes,
                jsChunkBytes = jsBytesInCurrentLuaChunk,
                jsChunkTotal = if (currentLuaChunkSize > 0) currentLuaChunkSize else 1L // avoid div by zero
            )
            requestNextChunk()
        } catch (e: Exception) {
            _activeDownload.value = DownloadState.Error(item.name, "Write error: ${e.message}")
            cleanupDownload()
        }
    }

    private fun requestNextChunk() {
        sendIoRequest(JSONObject().apply {
            put("type", "chunk")
        })
    }

    fun stopDownload() {
        sendIoRequest(JSONObject().apply {
            put("type", "stop")
        })
        _activeDownload.value = DownloadState.Idle
        cleanupDownload()
    }

    fun cancelDownload() {
        _activeDownload.value = DownloadState.Idle
        cleanupDownload()
    }

    fun dismissDownloadResult() {
        _activeDownload.value = DownloadState.Idle
    }

    private fun cleanupDownload() {
        runCatching { downloadOutputStream?.close() }
        downloadOutputStream = null
        downloadItem = null
        bytesDownloadedSoFar = 0L
    }

    private fun sendIoRequest(args: JSONObject) {
        globalWatchViewModel.sendStructuredMessage(type = "io", args = args)
    }

    fun navigateUp() {
        val current = _currentPath.value
        if (current == "/" || current.isBlank()) return
        val parentPath = current.substringBeforeLast("/").ifEmpty { "/" }
        requestDirectoryListing(parentPath)
    }
}
