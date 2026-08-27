package com.sucharek.miband_interconnect_test.ui.screens.activities.terminal

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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Color constants
private val TerminalBg = Color(0xFF101010)
private val TerminalText = Color(0xFF00FF66)      // Green for stdout
private val TerminalPrompt = Color(0xFFFFD700)    // Gold for ap>
private val TerminalError = Color(0xFFFF5252)     // Red ONLY for actual failures
private val TerminalSystem = Color(0xFF888888)    // Gray for client info
private val ToolbarBg = Color(0xFF1E1E1E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(
    viewModel: TerminalViewModel,
    modifier: Modifier = Modifier
) {
    var inputCommand by remember { mutableStateOf("") }
    val logs by viewModel.terminalLogs.collectAsState()

    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }
    val interactionSource = remember { MutableInteractionSource() }

    LaunchedEffect(logs.size, inputCommand) {
        val itemCount = logs.size + 1
        if (itemCount > 0) {
            listState.animateScrollToItem(itemCount - 1)
        }
    }

    val onSend = {
        if (inputCommand.isNotBlank()) {
            val cmd = inputCommand
            inputCommand = ""
            viewModel.executeCommand(cmd)
        }
    }

    Scaffold(
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(TerminalBg)
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
                    color = Color.White
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
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(logs) { line ->
                        // Clean, explicit color mapping by LineType
                        val textColor = when (line.type) {
                            LineType.INPUT -> TerminalPrompt
                            LineType.OUTPUT -> TerminalText
                            LineType.ERROR -> TerminalError
                            LineType.SYSTEM -> TerminalSystem
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

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ap> ",
                                style = TextStyle(
                                    color = TerminalPrompt,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp
                                )
                            )

                            BasicTextField(
                                value = inputCommand,
                                onValueChange = { inputCommand = it },
                                textStyle = TextStyle(
                                    color = TerminalText,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp
                                ),
                                cursorBrush = SolidColor(TerminalText),
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
            }

            // Quick Toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ToolbarBg)
                    .navigationBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TerminalKeyButton("▲") {
                        viewModel.getPreviousCommand()?.let { inputCommand = it }
                    }
                    TerminalKeyButton("▼") {
                        viewModel.getNextCommand()?.let { inputCommand = it }
                    }
                    TerminalKeyButton("CLR") {
                        viewModel.clearScreen()
                    }
                }

                Button(
                    onClick = onSend,
                    colors = ButtonDefaults.buttonColors(containerColor = TerminalPrompt),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        text = "RUN",
                        color = Color.Black,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun TerminalKeyButton(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color(0xFF2C2C2C),
        shape = MaterialTheme.shapes.extraSmall,
        modifier = Modifier.height(32.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 10.dp)
        ) {
            Text(
                text = text,
                color = Color.White,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
        }
    }
}