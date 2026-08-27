package com.sucharek.miband_interconnect_test.ui.screens.activities.moduleCompatibility

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
import com.sucharek.miband_interconnect_test.ui.screens.activities.qjsshell.DevToolsBg
import com.sucharek.miband_interconnect_test.ui.screens.activities.qjsshell.DevToolsLineDivider
import com.sucharek.miband_interconnect_test.ui.screens.activities.qjsshell.DevToolsPromptBlue
import com.sucharek.miband_interconnect_test.ui.screens.activities.qjsshell.JsonTreeItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModuleCompatibilityScreen(
    viewModel: ModuleCompatibilityViewModel,
    modifier: Modifier = Modifier
) {
    val modules by viewModel.modules.collectAsState()
    var customInput by remember { mutableStateOf("") }
    
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        viewModel.scrollToModule.collect { index ->
            listState.animateScrollToItem(index)
        }
    }

    Scaffold(
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DevToolsBg)
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
                    color = Color.White
                )

                Button(
                    onClick = { viewModel.testSelectedCompat() },
                    colors = ButtonDefaults.buttonColors(containerColor = DevToolsPromptBlue),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Check Selected", color = Color.Black, fontSize = 12.sp)
                }
            }

            HorizontalDivider(color = DevToolsLineDivider)

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
                            onToggleExpand = { viewModel.fetchFunctions(item.name) }
                        )
                        HorizontalDivider(color = DevToolsLineDivider, thickness = 0.5.dp)
                    }
                }
            }

            // Bottom Add Module Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E1E1E))
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
                            color = Color.White
                        ),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DevToolsPromptBlue,
                            unfocusedBorderColor = DevToolsLineDivider
                        )
                    )

                    IconButton(
                        onClick = {
                            if (customInput.isNotBlank()) {
                                viewModel.addCustomModule(customInput)
                                customInput = ""
                            }
                        },
                        modifier = Modifier.background(DevToolsPromptBlue, RoundedCornerShape(8.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Module",
                            tint = Color.Black
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
    onToggleExpand: () -> Unit
) {
    val isSupported = item.status == CompatStatus.SUPPORTED
    val isUnsupported = item.status == CompatStatus.UNSUPPORTED

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (item.isExpanded && !isUnsupported) Color(0xFF2A2A2A) else Color.Transparent)
            .padding(vertical = 4.dp, horizontal = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = item.isSelected,
                onCheckedChange = { onToggleSelect() },
                colors = CheckboxDefaults.colors(checkedColor = DevToolsPromptBlue)
            )

            Text(
                text = item.name,
                color = Color.White,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onCheckCompat() }
            )

            // Status Badge
            when (item.status) {
                CompatStatus.UNKNOWN -> {
                    TextButton(onClick = onCheckCompat) {
                        Text("Check", color = Color.Gray, fontSize = 11.sp)
                    }
                }
                CompatStatus.CHECKING -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = DevToolsPromptBlue
                    )
                }
                CompatStatus.SUPPORTED -> {
                    Surface(
                        color = Color(0xFF1B4D2E),
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
                        color = Color(0xFF4D1B1B),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Text(
                            text = "Unsupported",
                            color = Color(0xFFE57373),
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
                enabled = isSupported
            ) {
                if (item.isLoadingFuncs) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = DevToolsPromptBlue
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.List,
                        contentDescription = "Inspect Functions",
                        tint = when {
                            !isSupported -> Color.Gray.copy(alpha = 0.38f)
                            item.functionsResult != null -> DevToolsPromptBlue
                            else -> Color.White
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
                    .background(Color(0xFF1E1E1E), RoundedCornerShape(6.dp))
                    .padding(8.dp)
            ) {
                if (item.functionsResult != null) {
                    // Fully expand all nested levels in Module Tester
                    JsonTreeItem(value = item.functionsResult, expandAll = true)
                } else if (isSupported && !item.isLoadingFuncs) {
                    Text(
                        text = "Tap list icon to fetch module exports...",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                } else {
                    Text(
                        text = "Check compatibility first...",
                        color = Color.Gray.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}