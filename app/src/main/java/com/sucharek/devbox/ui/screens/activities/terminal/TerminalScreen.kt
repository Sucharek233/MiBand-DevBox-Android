package com.sucharek.devbox.ui.screens.activities.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharek.devbox.ui.screens.maindashboard.WatchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(
    viewModel: TerminalViewModel,
    watchViewModel: WatchViewModel,
    modifier: Modifier = Modifier
) {
    var inputCommand by remember { mutableStateOf(TextFieldValue("")) }
    val logs by viewModel.terminalLogs.collectAsState()
    val isBusy by watchViewModel.isLuaBusy.collectAsState()

    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }
    val interactionSource = remember { MutableInteractionSource() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Focus on start
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Scroll when logs update
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    val onSend = {
        if (inputCommand.text.isNotBlank() && !isBusy) {
            val cmd = inputCommand.text
            inputCommand = TextFieldValue("")
            viewModel.executeCommand(cmd)
            
            // Re-request focus and ensure keyboard stays open
            focusRequester.requestFocus()
            keyboardController?.show()
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
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) {
                    focusRequester.requestFocus()
                }
        ) {
            // --- Header ---
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Terminal",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            SelectionContainer(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(logs) { line ->
                        val textColor = when (line.type) {
                            LineType.INPUT -> MaterialTheme.colorScheme.primary
                            LineType.OUTPUT -> MaterialTheme.colorScheme.onSurface
                            LineType.ERROR -> MaterialTheme.colorScheme.error
                            LineType.SYSTEM -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        }

                        Text(
                            text = line.text,
                            style = TextStyle(
                                color = textColor,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                lineHeight = 17.sp
                            )
                        )
                    }
                }
            }

            // Fixed Input Field
            Surface(
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ap> ",
                            style = TextStyle(
                                color = MaterialTheme.colorScheme.primary,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp
                            )
                        )

                        BasicTextField(
                            value = inputCommand,
                            onValueChange = { inputCommand = it },
                            textStyle = TextStyle(
                                color = MaterialTheme.colorScheme.onBackground,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = { onSend() }),
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester)
                        )
                    }
                }
            }

            // Quick Toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .navigationBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilledTonalButton(
                        onClick = { 
                            viewModel.getPreviousCommand()?.let { 
                                inputCommand = TextFieldValue(it, selection = androidx.compose.ui.text.TextRange(it.length)) 
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        modifier = Modifier.height(32.dp).focusProperties { canFocus = false },
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text("▲", fontSize = 11.sp)
                    }
                    FilledTonalButton(
                        onClick = { 
                            viewModel.getNextCommand()?.let { 
                                inputCommand = TextFieldValue(it, selection = androidx.compose.ui.text.TextRange(it.length)) 
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        modifier = Modifier.height(32.dp).focusProperties { canFocus = false },
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text("▼", fontSize = 11.sp)
                    }
                    FilledTonalButton(
                        onClick = { 
                            viewModel.clearScreen()
                            focusRequester.requestFocus()
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        modifier = Modifier.height(32.dp).focusProperties { canFocus = false },
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text("CLR", fontSize = 11.sp)
                    }
                }

                Button(
                    onClick = onSend,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp).focusProperties { canFocus = false },
                    enabled = !isBusy
                ) {
                    Text(
                        text = "RUN",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
