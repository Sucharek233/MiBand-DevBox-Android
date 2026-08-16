package com.sucharek.miband_interconnect_test.ui.screens.activities.luashell

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharek.miband_interconnect_test.ui.screens.activities.qjsshell.ConsoleEntry
import com.sucharek.miband_interconnect_test.ui.screens.activities.qjsshell.DevToolsBg
import com.sucharek.miband_interconnect_test.ui.screens.activities.qjsshell.DevToolsDimArrow
import com.sucharek.miband_interconnect_test.ui.screens.activities.qjsshell.DevToolsErrorBg
import com.sucharek.miband_interconnect_test.ui.screens.activities.qjsshell.DevToolsErrorText
import com.sucharek.miband_interconnect_test.ui.screens.activities.qjsshell.DevToolsLineDivider
import com.sucharek.miband_interconnect_test.ui.screens.activities.qjsshell.DevToolsPromptBlue
import com.sucharek.miband_interconnect_test.ui.screens.activities.qjsshell.JsonTreeItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LuaShellScreen(
    viewModel: LuaShellViewModel,
    modifier: Modifier = Modifier
) {
    var codeInput by remember { mutableStateOf("") }
    val entries by viewModel.entries.collectAsState()

    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }
    val interactionSource = remember { MutableInteractionSource() }

    LaunchedEffect(entries.size, codeInput) {
        val totalItems = entries.size + 1
        if (totalItems > 0) {
            listState.animateScrollToItem(totalItems - 1)
        }
    }

    val onSend = {
        if (codeInput.isNotBlank()) {
            val code = codeInput
            codeInput = ""
            viewModel.executeLuaCode(code)
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("VelaLua DevTools") }) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .background(DevToolsBg)
                .imePadding()
        ) {
            // Main Console View Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) {
                        focusRequester.requestFocus()
                    }
            ) {
                SelectionContainer {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(entries) { entry ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(
                                        if (entry is ConsoleEntry.Error) {
                                            Modifier.background(DevToolsErrorBg)
                                        } else Modifier
                                    )
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                when (entry) {
                                    is ConsoleEntry.Input -> {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "lua> ",
                                                color = DevToolsPromptBlue,
                                                fontSize = 12.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Text(
                                                text = entry.code,
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }

                                    is ConsoleEntry.Output -> {
                                        Row(verticalAlignment = Alignment.Top) {
                                            Text(
                                                text = "‹ ",
                                                color = DevToolsDimArrow,
                                                fontSize = 12.sp,
                                                fontFamily = FontFamily.Monospace,
                                                modifier = Modifier.padding(end = 2.dp)
                                            )
                                            Box(modifier = Modifier.weight(1f)) {
                                                JsonTreeItem(value = entry.rawResult)
                                            }
                                        }
                                    }

                                    is ConsoleEntry.Error -> {
                                        Row(verticalAlignment = Alignment.Top) {
                                            Text(
                                                text = "⊗ ",
                                                color = DevToolsErrorText,
                                                fontSize = 12.sp,
                                                fontFamily = FontFamily.Monospace,
                                                modifier = Modifier.padding(end = 4.dp)
                                            )
                                            Column {
                                                Text(
                                                    text = entry.message,
                                                    color = DevToolsErrorText,
                                                    fontSize = 12.sp,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                                if (entry.stack != null) {
                                                    Text(
                                                        text = entry.stack,
                                                        color = DevToolsErrorText.copy(alpha = 0.7f),
                                                        fontSize = 10.sp,
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            HorizontalDivider(color = DevToolsLineDivider, thickness = 0.5.dp)
                        }

                        // Interactive Input Prompt
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "lua> ",
                                    color = DevToolsPromptBlue,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 16.sp
                                )

                                BasicTextField(
                                    value = codeInput,
                                    onValueChange = { codeInput = it },
                                    textStyle = TextStyle(
                                        color = Color.White,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    ),
                                    cursorBrush = SolidColor(DevToolsPromptBlue),
                                    singleLine = false,
                                    maxLines = 8,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                                    modifier = Modifier
                                        .weight(1f)
                                        .focusRequester(focusRequester)
                                )
                            }
                        }
                    }
                }
            }

            // Quick Toolbar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E1E1E))
                    .navigationBarsPadding()
            ) {
                HorizontalDivider(color = DevToolsLineDivider, thickness = 1.dp)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        LuaShellKeyButton("▲") {
                            viewModel.getPreviousCommand()?.let { codeInput = it }
                        }
                        LuaShellKeyButton("▼") {
                            viewModel.getNextCommand()?.let { codeInput = it }
                        }
                        LuaShellKeyButton("TAB") {
                            codeInput += "  "
                        }
                        LuaShellKeyButton("CLR") {
                            viewModel.clearConsole()
                        }
                    }

                    Button(
                        onClick = onSend,
                        colors = ButtonDefaults.buttonColors(containerColor = DevToolsPromptBlue),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text(
                            text = "EXEC ↵",
                            color = Color.Black,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LuaShellKeyButton(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color(0xFF333333),
        shape = MaterialTheme.shapes.extraSmall,
        modifier = Modifier.height(30.dp)
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
