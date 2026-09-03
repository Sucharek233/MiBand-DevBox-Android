package com.sucharek.miband_interconnect_test.ui.screens.activities.files

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileExplorerScreen(
    viewModel: FileExplorerViewModel,
    modifier: Modifier = Modifier
) {
    val currentPath by viewModel.currentPath.collectAsState()
    val files by viewModel.filesList.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val downloadState by viewModel.activeDownload.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }
    
    var pendingDelete by remember { mutableStateOf<FileItem?>(null) }
    var pendingOperation by remember { mutableStateOf<Pair<String, FileItem>?>(null) } // "cp" or "mv"
    var destinationPath by remember { mutableStateOf("") }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            viewModel.onFolderSelected(uri)
        } else {
            viewModel.cancelDownload()
        }
    }

    LaunchedEffect(downloadState) {
        if (downloadState is DownloadState.RequestFolder) {
            folderPickerLauncher.launch(null)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.operationStatus.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // --- Header ---
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Files",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = { viewModel.refresh() },
                    enabled = !isLoading
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
            }

            // Breadcrumbs
            Breadcrumbs(
                path = currentPath,
                onBreadcrumbClick = { path -> viewModel.requestDirectoryListing(path) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                enabled = !isLoading
            )

            HorizontalDivider()

            if (isLoading && files.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(files) { item ->
                            FileListItem(
                                item = item,
                                currentPath = currentPath,
                                isLoading = isLoading,
                                onNavigate = { viewModel.requestDirectoryListing(it) },
                                onDownload = { viewModel.onDownloadClick(item) },
                                onCopy = { 
                                    pendingOperation = "cp" to item
                                    destinationPath = item.fullPath + "_copy"
                                },
                                onMove = { 
                                    pendingOperation = "mv" to item
                                    destinationPath = item.fullPath
                                },
                                onDelete = { pendingDelete = item }
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                    
                    if (isLoading) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }

    // --- Dialogs ---

    pendingDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete File") },
            text = { Text("Are you sure you want to delete '${item.name}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteFile(item.fullPath)
                        pendingDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    pendingOperation?.let { (op, item) ->
        AlertDialog(
            onDismissRequest = { pendingOperation = null },
            title = { Text(if (op == "cp") "Copy File" else "Move File") },
            text = {
                Column {
                    Text("Source: ${item.fullPath}")
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = destinationPath,
                        onValueChange = { destinationPath = it },
                        label = { Text("Destination Path") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (op == "cp") viewModel.copyFile(item.fullPath, destinationPath)
                        else viewModel.moveFile(item.fullPath, destinationPath)
                        pendingOperation = null
                    },
                    enabled = destinationPath.isNotBlank() && destinationPath != item.fullPath
                ) {
                    Text(if (op == "cp") "Copy" else "Move")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingOperation = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    when (val state = downloadState) {
        is DownloadState.Configure -> {
            DownloadSettingsDialog(
                item = state.item,
                onStart = { l, q -> viewModel.startDownload(state.item, l, q) },
                onCancel = { viewModel.cancelDownload() }
            )
        }
        is DownloadState.Connecting, is DownloadState.Progress, is DownloadState.Success, is DownloadState.Error -> {
            DownloadProgressDialog(
                state = state,
                onStop = { viewModel.stopDownload() },
                onDismiss = { viewModel.dismissDownloadResult() }
            )
        }
        else -> {}
    }
}

@Composable
fun DownloadSettingsDialog(
    item: FileItem,
    onStart: (luaKB: Int, qjsKB: Int) -> Unit,
    onCancel: () -> Unit
) {
    var luaSize by remember { mutableFloatStateOf(500f) }
    var qjsSize by remember { mutableFloatStateOf(30f) }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Download Settings") },
        text = {
            Column {
                Text(text = item.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(text = "Lua Chunk Size: ${luaSize.toInt()} KB", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = luaSize,
                    onValueChange = { luaSize = it },
                    valueRange = 1f..1024f,
                    steps = 1023,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))

                Text(text = "QJS Chunk Size: ${qjsSize.toInt()} KB", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = qjsSize,
                    onValueChange = { qjsSize = it },
                    valueRange = 1f..32f,
                    steps = 31,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    onStart(luaSize.toInt(), qjsSize.toInt())
                }
            ) {
                Text("Start Download")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun FileListItem(
    item: FileItem,
    currentPath: String,
    isLoading: Boolean,
    onNavigate: (String) -> Unit,
    onDownload: () -> Unit,
    onCopy: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = { Text(item.name) },
        supportingContent = {
            val subtitleText = when {
                item.isDirectory -> "Folder"
                item.sizeBytes == -1L -> "?"
                else -> formatSize(item.sizeBytes)
            }
            Text(text = subtitleText)
        },
        leadingContent = {
            Icon(
                imageVector = if (item.isDirectory) Icons.Default.Folder else Icons.AutoMirrored.Filled.InsertDriveFile,
                contentDescription = null,
                tint = if (item.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
            )
        },
        trailingContent = {
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More")
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    if (!item.isDirectory) {
                        DropdownMenuItem(
                            text = { Text("Download") },
                            onClick = {
                                menuExpanded = false
                                onDownload()
                            },
                            leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Copy") },
                        onClick = {
                            menuExpanded = false
                            onCopy()
                        },
                        leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Move") },
                        onClick = {
                            menuExpanded = false
                            onMove()
                        },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                        colors = MenuDefaults.itemColors(
                            textColor = MaterialTheme.colorScheme.error,
                            leadingIconColor = MaterialTheme.colorScheme.error
                        )
                    )
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isLoading) {
                if (item.isDirectory) {
                    val subPath = if (currentPath == "/") "/${item.name}" else "$currentPath/${item.name}"
                    onNavigate(subPath)
                }
            }
    )
}

@Composable
fun DownloadProgressDialog(
    state: DownloadState,
    onStop: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (state is DownloadState.Success || state is DownloadState.Error) onDismiss() },
        title = {
            Text(when(state) {
                is DownloadState.Connecting -> "Connecting..."
                is DownloadState.Progress -> "Downloading..."
                is DownloadState.Success -> "Download Complete"
                is DownloadState.Error -> "Download Failed"
                else -> ""
            })
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                val fileName = when(state) {
                    is DownloadState.Connecting -> state.fileName
                    is DownloadState.Progress -> state.fileName
                    is DownloadState.Success -> state.fileName
                    is DownloadState.Error -> state.fileName
                    else -> ""
                }
                
                Text(text = fileName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                
                when(state) {
                    is DownloadState.Connecting -> {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    is DownloadState.Progress -> {
                        // Overall Bar
                        val overallProg = if (state.totalBytes > 0) state.overallBytes.toFloat() / state.totalBytes else 0f
                        Text(text = "Overall: ${formatSize(state.overallBytes)} / ${formatSize(state.totalBytes)}", style = MaterialTheme.typography.labelSmall)
                        LinearProgressIndicator(
                            progress = { overallProg },
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Lua Bar
                        val luaProg = if (state.totalBytes > 0) state.luaPreparedBytes.toFloat() / state.totalBytes else 0f
                        Text(text = "Lua Prepared: ${formatSize(state.luaPreparedBytes)}", style = MaterialTheme.typography.labelSmall)
                        LinearProgressIndicator(
                            progress = { luaProg },
                            modifier = Modifier.fillMaxWidth().height(4.dp),
                            color = Color(0xFFE91E63) // Lua Pink
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // QJS Bar
                        val qjsProg = if (state.jsChunkTotal > 0) state.jsChunkBytes.toFloat() / state.jsChunkTotal else 0f
                        Text(text = "Interconnect: ${formatSize(state.jsChunkBytes)} / ${formatSize(state.jsChunkTotal)}", style = MaterialTheme.typography.labelSmall)
                        LinearProgressIndicator(
                            progress = { qjsProg },
                            modifier = Modifier.fillMaxWidth().height(4.dp),
                            color = Color(0xFFFF9800) // QJS Orange
                        )
                    }
                    is DownloadState.Success -> {
                        Text("File has been successfully saved to your selected folder.")
                    }
                    is DownloadState.Error -> {
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                    }
                    else -> {}
                }
            }
        },
        confirmButton = {
            if (state is DownloadState.Success || state is DownloadState.Error) {
                Button(onClick = onDismiss) {
                    Text("Close")
                }
            } else {
                TextButton(
                    onClick = onStop,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Stop")
                }
            }
        }
    )
}

@Composable
fun Breadcrumbs(
    path: String,
    onBreadcrumbClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val breadcrumbs = remember(path) {
        if (path == "/" || path.isBlank()) {
            listOf("Root" to "/")
        } else {
            val parts = path.split("/").filter { it.isNotBlank() }
            val result = mutableListOf("Root" to "/")
            var current = ""
            parts.forEach { part ->
                current += "/$part"
                result.add(part to current)
            }
            result
        }
    }

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        itemsIndexed(breadcrumbs) { index, (name, fullPath) ->
            val isLast = index == breadcrumbs.size - 1
            
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isLast) FontWeight.Bold else FontWeight.Normal,
                color = if (isLast) {
                    MaterialTheme.colorScheme.primary
                } else {
                    if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                },
                modifier = Modifier
                    .clickable(enabled = enabled && !isLast) { onBreadcrumbClick(fullPath) }
                    .padding(vertical = 4.dp, horizontal = 2.dp)
            )
            
            if (!isLast) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                )
            }
        }
    }
}

private fun formatSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes.toDouble() / 1024.0
    return String.format(Locale.US, "%.1f KB", kb)
}
