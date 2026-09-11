package com.sucharek.devbox.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import dev.hossain.highlight.ui.LocalHighlightTheme
import dev.hossain.highlight.ui.rememberHighlightedCode

/**
 * Renders syntax-highlighted code as plain text without any surrounding UI (background, header, etc.).
 *
 * @param code The source code to highlight.
 * @param language The Highlight.js language identifier (e.g. "json", "lua", "javascript").
 * @param modifier Modifier for the text component.
 * @param textStyle Optional text style to override the default monospace look.
 */
@Composable
fun PureSyntaxHighlightedCode(
    code: String,
    language: String,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 12.sp
    )
) {
    val theme = LocalHighlightTheme.current
    val highlighted by rememberHighlightedCode(code, language, theme)
    
    val baseColor = theme.defaultTextColor.takeIf { it != Color.Unspecified } ?: Color.White
    
    Text(
        text = highlighted ?: AnnotatedString(code),
        style = textStyle.copy(color = baseColor),
        modifier = modifier
    )
}
