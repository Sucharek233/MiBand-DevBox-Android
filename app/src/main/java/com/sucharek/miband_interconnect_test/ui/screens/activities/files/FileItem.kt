package com.sucharek.miband_interconnect_test.ui.screens.activities.files

import org.json.JSONObject

data class FileItem(
    val name: String,
    val isDirectory: Boolean,
    val sizeBytes: Long = 0L,
    val parentPath: String = ""
) {
    val fullPath: String
        get() = if (parentPath == "/") "/$name" else "$parentPath/$name"

    companion object {
        fun fromJson(json: JSONObject): FileItem {
            return FileItem(
                name = json.optString("name", "Unknown"),
                isDirectory = json.optBoolean("isDir", false),
                sizeBytes = json.optLong("size", 0L)
            )
        }
    }
}