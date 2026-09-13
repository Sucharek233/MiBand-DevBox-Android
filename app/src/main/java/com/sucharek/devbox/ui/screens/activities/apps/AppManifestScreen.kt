package com.sucharek.devbox.ui.screens.activities.apps

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.hossain.highlight.ui.ExperimentalHighlightApi
import dev.hossain.highlight.ui.LocalHighlightTheme
import dev.hossain.highlight.ui.rememberSyntaxHighlightedEditorValue
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class, ExperimentalHighlightApi::class)
@Composable
fun AppManifestScreen(
    viewModel: AppManifestViewModel
) {
    val manifestContent by viewModel.manifestContent.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isAnyAppOperationActive by viewModel.isAnyAppOperationActive.collectAsState()
    val saveStatus by viewModel.saveStatus.collectAsState()

    var editableContent by remember { mutableStateOf(TextFieldValue(manifestContent)) }
    
    val theme = LocalHighlightTheme.current
    val displayValue = rememberSyntaxHighlightedEditorValue(
        value = editableContent,
        language = "json",
        theme = theme
    )

    // Update local state when remote content is fetched or loading finishes
    LaunchedEffect(manifestContent, isLoading) {
        if (!isLoading && manifestContent.isNotEmpty()) {
            editableContent = TextFieldValue(manifestContent)
        }
    }

    // Auto-clear "Saved" status after 3 seconds
    LaunchedEffect(saveStatus) {
        if (saveStatus == "Saved") {
            delay(3000.milliseconds)
            viewModel.clearSaveStatus()
        }
    }

    val isSaving = saveStatus == "Saving..."

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            // --- Header ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Manifest",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (saveStatus != null) {
                        Text(
                            text = saveStatus!!,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (saveStatus!!.contains("Error")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }

                    IconButton(
                        onClick = { 
                            editableContent = TextFieldValue("") // Clear local state to allow overwrite by incoming refresh
                            viewModel.refresh() 
                        },
                        enabled = !isAnyAppOperationActive && !isSaving
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    Button(
                        onClick = { viewModel.saveManifest(editableContent.text) },
                        enabled = !isAnyAppOperationActive && !isSaving && editableContent.text.isNotEmpty(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isSaving) "Saving" else "Save", fontSize = 12.sp)
                    }
                }
            }

            if (isLoading && manifestContent.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Using a Column with verticalScroll to allow proper scrolling when keyboard is open
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        TextField(
                            value = displayValue,
                            onValueChange = { if (!isSaving) editableContent = it },
                            enabled = !isSaving,
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            ),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent
                            ),
                            placeholder = { Text("Loading manifest...") }
                        )
                    }

                    if (isLoading) {
                        LoadingOverlay()
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingOverlay() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black.copy(alpha = 0.3f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.White)
        }
    }
}
