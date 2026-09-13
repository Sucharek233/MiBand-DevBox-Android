package com.sucharek.devbox.ui.screens.activities.files

data class FileItem(
    val name: String,
    val isDirectory: Boolean,
    val sizeBytes: Long = 0L,
    val parentPath: String = ""
) {
    val fullPath: String
        get() = if (parentPath == "/") "/$name" else "$parentPath/$name"
}