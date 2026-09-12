package com.sucharek.devbox.ui.screens.activities.moduleCompatibility

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharek.devbox.ui.screens.maindashboard.WatchViewModel
import com.sucharek.devbox.ui.screens.activities.qjsshell.JsonTreeItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModuleCompatibilityScreen(
    viewModel: ModuleCompatibilityViewModel,
    watchViewModel: WatchViewModel,
    modifier: Modifier = Modifier
) {
    val modules by viewModel.modules.collectAsState()
    val isBusy by watchViewModel.isAnyOperationActive.collectAsState()
    var customInput by remember { mutableStateOf("") }
    
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        viewModel.scrollToModule.collect { index ->
            listState.animateScrollToItem(index)
        }
    }

    // Automatically trigger a check if not done
    LaunchedEffect(Unit) {
        if (modules.all { it.status == CompatStatus.UNKNOWN }) {
            viewModel.testSelectedCompat()
        }
    }

    Scaffold(
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
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
                    text = "Modules",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Button(
                    onClick = { viewModel.testSelectedCompat() },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    enabled = !isBusy
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Check Selected", fontSize = 12.sp)
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Scrollable Module List
            SelectionContainer(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(modules) { item ->
                        ModuleCardRow(
                            item = item,
                            onToggleSelect = { viewModel.toggleSelection(item.name) },
                            onCheckCompat = { viewModel.testSingleCompat(item.name) },
                            onToggleExpand = { viewModel.fetchFunctions(item.name) },
                            isBusy = isBusy
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                    }
                }
            }

            // Bottom Add Module Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .navigationBarsPadding()
                    .padding(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = customInput,
                        onValueChange = { customInput = it },
                        placeholder = { Text("Custom module e.g. system.device", fontSize = 12.sp) },
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    IconButton(
                        onClick = {
                            if (customInput.isNotBlank()) {
                                viewModel.addCustomModule(customInput)
                                customInput = ""
                            }
                        },
                        modifier = Modifier.background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)),
                        enabled = !isBusy
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Module",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModuleCardRow(
    item: ModuleItem,
    onToggleSelect: () -> Unit,
    onCheckCompat: () -> Unit,
    onToggleExpand: () -> Unit,
    isBusy: Boolean
) {
    val isSupported = item.status == CompatStatus.SUPPORTED
    val isUnsupported = item.status == CompatStatus.UNSUPPORTED

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (item.isExpanded && !isUnsupported) MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp) 
                else Color.Transparent
            )
            .padding(vertical = 4.dp, horizontal = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = item.isSelected,
                onCheckedChange = { onToggleSelect() },
                enabled = !isBusy
            )

            Text(
                text = item.name,
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .weight(1f)
                    .clickable(enabled = !isBusy) { onCheckCompat() }
            )

            // Status Badge
            when (item.status) {
                CompatStatus.UNKNOWN -> {
                    TextButton(onClick = onCheckCompat, enabled = !isBusy) {
                        Text("Check", color = MaterialTheme.colorScheme.outline, fontSize = 11.sp)
                    }
                }
                CompatStatus.CHECKING -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                }
                CompatStatus.SUPPORTED -> {
                    Surface(
                        color = Color(0xFF1B4D2E), // Keeping standard green for "Supported" but could use TertiaryContainer
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Text(
                            text = "Supported",
                            color = Color(0xFF81C784),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                CompatStatus.UNSUPPORTED -> {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Text(
                            text = "Unsupported",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Inspect Functions Action
            IconButton(
                onClick = onToggleExpand,
                enabled = isSupported && !isBusy
            ) {
                if (item.isLoadingFuncs) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.List,
                        contentDescription = "Inspect Functions",
                        tint = when {
                            !isSupported -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            item.functionsResult != null -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Functions Expandable Panel - Completely hidden for unsupported modules
        AnimatedVisibility(visible = item.isExpanded && !isUnsupported) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 32.dp, end = 8.dp, bottom = 8.dp, top = 2.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                    .padding(8.dp)
            ) {
                if (item.functionsResult != null) {
                    // Fully expand all nested levels in Module Tester
                    JsonTreeItem(value = item.functionsResult, expandAll = true)
                } else if (isSupported && !item.isLoadingFuncs) {
                    Text(
                        text = "Tap list icon to fetch module exports...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                } else {
                    Text(
                        text = "Check compatibility first...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
