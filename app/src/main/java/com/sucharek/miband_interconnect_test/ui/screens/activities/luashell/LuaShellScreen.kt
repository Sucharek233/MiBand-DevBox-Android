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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.hossain.highlight.ui.ExperimentalHighlightApi
import dev.hossain.highlight.ui.SyntaxHighlightedCode
import dev.hossain.highlight.ui.rememberHighlightEngine
import dev.hossain.highlight.ui.rememberHighlightedCode
import dev.hossain.highlight.ui.rememberSyntaxHighlightedEditorValue
import dev.hossain.highlight.ui.LocalHighlightTheme
import com.sucharek.miband_interconnect_test.ui.components.PureSyntaxHighlightedCode
import com.sucharek.miband_interconnect_test.ui.screens.activities.qjsshell.ConsoleEntry
import com.sucharek.miband_interconnect_test.ui.screens.activities.qjsshell.JsonTreeItem

@OptIn(ExperimentalMaterial3Api::class, ExperimentalHighlightApi::class)
@Composable
fun LuaShellScreen(
    viewModel: LuaShellViewModel,
    modifier: Modifier = Modifier
) {
    var codeInput by remember { mutableStateOf(TextFieldValue("")) }
    val entries by viewModel.entries.collectAsState()
    
    val theme = LocalHighlightTheme.current
    
    val displayValue = rememberSyntaxHighlightedEditorValue(
        value = codeInput,
        language = "lua",
        theme = theme
    )

    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }
    val interactionSource = remember { MutableInteractionSource() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(entries.size, codeInput.text) {
        val totalItems = entries.size + 1
        if (totalItems > 0) {
            listState.animateScrollToItem(totalItems - 1)
        }
    }

    val onSend = {
        if (codeInput.text.isNotBlank()) {
            val code = codeInput.text
            codeInput = TextFieldValue("")
            viewModel.executeLuaCode(code)
            
            // Keep the keyboard open
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
        ) {
            // --- Header ---
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Lua Shell",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

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
                                            Modifier.background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f))
                                        } else Modifier
                                    )
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                when (entry) {
                                    is ConsoleEntry.Input -> {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "lua> ",
                                                color = MaterialTheme.colorScheme.primary,
                                                fontSize = 12.sp,
                                                fontFamily = FontFamily.Monospace,
                                                modifier = Modifier.padding(top = 4.dp)
                                            )
                                            PureSyntaxHighlightedCode(
                                                code = entry.code,
                                                language = "lua",
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }

                                    is ConsoleEntry.Output -> {
                                        Row(verticalAlignment = Alignment.Top) {
                                            Text(
                                                text = "‹ ",
                                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                                                fontSize = 12.sp,
                                                fontFamily = FontFamily.Monospace,
                                                modifier = Modifier.padding(end = 2.dp)
                                            )
                                            Box(modifier = Modifier.weight(1f)) {
                                                JsonTreeItem(value = entry.rawResult)
                                            }
                                        }
                                    }

                                    is ConsoleEntry.Log -> {
                                        Row(verticalAlignment = Alignment.Top) {
                                            Text(
                                                text = "• ",
                                                color = Color.Gray,
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
                                                color = MaterialTheme.colorScheme.error,
                                                fontSize = 12.sp,
                                                fontFamily = FontFamily.Monospace,
                                                modifier = Modifier.padding(end = 4.dp)
                                            )
                                            Column {
                                                Text(
                                                    text = entry.message,
                                                    color = MaterialTheme.colorScheme.error,
                                                    fontSize = 12.sp,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                                if (entry.stack != null) {
                                                    Text(
                                                        text = entry.stack,
                                                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                                        fontSize = 10.sp,
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
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
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 16.sp
                                )

                                BasicTextField(
                                    value = displayValue,
                                    onValueChange = { codeInput = it },
                                    textStyle = TextStyle(
                                        color = MaterialTheme.colorScheme.onBackground,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    ),
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
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

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .navigationBarsPadding()
            ) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilledTonalButton(
                            onClick = { 
                                viewModel.getPreviousCommand()?.let { 
                                    codeInput = TextFieldValue(it, selection = androidx.compose.ui.text.TextRange(it.length))
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            modifier = Modifier.height(32.dp),
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text("▲", fontSize = 11.sp)
                        }
                        FilledTonalButton(
                            onClick = { 
                                viewModel.getNextCommand()?.let { 
                                    codeInput = TextFieldValue(it, selection = androidx.compose.ui.text.TextRange(it.length))
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            modifier = Modifier.height(32.dp),
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text("▼", fontSize = 11.sp)
                        }
                        FilledTonalButton(
                            onClick = { viewModel.clearConsole() },
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            modifier = Modifier.height(32.dp),
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text("CLR", fontSize = 11.sp)
                        }
                    }

                    Button(
                        onClick = onSend,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            text = "EXEC ↵",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

