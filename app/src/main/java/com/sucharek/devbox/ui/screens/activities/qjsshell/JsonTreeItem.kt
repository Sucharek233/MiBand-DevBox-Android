package com.sucharek.devbox.ui.screens.activities.qjsshell

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject

// DevTools Dark Color Palette
val DevToolsBg = Color(0xFF242424)
val DevToolsLineDivider = Color(0xFF383838)
val DevToolsPromptBlue = Color(0xFF91B4FF)
val DevToolsDimArrow = Color(0xFF7F7F7F)
val DevToolsStringVal = Color(0xFFA5D6FF)
val DevToolsNumberVal = Color(0xFF79C0FF)
val DevToolsKeyword = Color(0xFFFF7B72)
val DevToolsKeyName = Color(0xFFD2A8FF)
val DevToolsErrorBg = Color(0xFF2C1517)
val DevToolsErrorText = Color(0xFFFF8182)

@Composable
fun JsonTreeItem(
    keyName: String? = null,
    value: Any?,
    depth: Int = 0,
    expandAll: Boolean = false // <--- Allows screens like ModuleTester to expand all sub-levels
) {
    var isExpanded by rememberSaveable(depth, expandAll) {
        mutableStateOf(expandAll || depth == 0)
    }
    val indent = (depth * 14).dp

    Column(modifier = Modifier.padding(start = indent)) {
        when (value) {
            is JSONObject -> {
                val dollarTag = value.optString("$", "")
                when (dollarTag) {
                    "fn" -> LeafValueRow(keyName, "f ()", DevToolsKeyword)
                    "ref" -> LeafValueRow(keyName, "Ref(${value.optString("to", "root")})", DevToolsDimArrow)
                    "undef" -> LeafValueRow(keyName, "undefined", DevToolsDimArrow)
                    "null" -> LeafValueRow(keyName, "null", DevToolsDimArrow)
                    "nil" -> LeafValueRow(keyName, "nil", DevToolsDimArrow)
                    "userdata" -> LeafValueRow(keyName, value.optString("repr", "userdata"), DevToolsKeyName)
                    "nan" -> LeafValueRow(keyName, "NaN", DevToolsNumberVal)
                    "inf" -> LeafValueRow(keyName, "Infinity", DevToolsNumberVal)
                    "ninf" -> LeafValueRow(keyName, "-Infinity", DevToolsNumberVal)
                    else -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isExpanded = !isExpanded }
                                .padding(vertical = 1.dp)
                        ) {
                            Text(
                                text = if (isExpanded) "▼ " else "▶ ",
                                color = DevToolsDimArrow,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )

                            Text(
                                text = buildAnnotatedString {
                                    if (keyName != null) {
                                        withStyle(SpanStyle(color = DevToolsKeyName)) { append("$keyName: ") }
                                    }
                                    withStyle(SpanStyle(color = Color.LightGray)) {
                                        append("Object { ... }")
                                    }
                                },
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        if (isExpanded) {
                            val keys = value.keys()
                            while (keys.hasNext()) {
                                val k = keys.next()
                                JsonTreeItem(
                                    keyName = k,
                                    value = value.opt(k),
                                    depth = depth + 1,
                                    expandAll = expandAll
                                )
                            }
                        }
                    }
                }
            }

            is JSONArray -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isExpanded = !isExpanded }
                        .padding(vertical = 1.dp)
                ) {
                    Text(
                        text = if (isExpanded) "▼ " else "▶ ",
                        color = DevToolsDimArrow,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = buildAnnotatedString {
                            if (keyName != null) {
                                withStyle(SpanStyle(color = DevToolsKeyName)) { append("$keyName: ") }
                            }
                            withStyle(SpanStyle(color = Color.LightGray)) {
                                append("Array(${value.length()})")
                            }
                        },
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                if (isExpanded) {
                    for (i in 0 until value.length()) {
                        JsonTreeItem(
                            keyName = "$i",
                            value = value.opt(i),
                            depth = depth + 1,
                            expandAll = expandAll
                        )
                    }
                }
            }

            is String -> LeafValueRow(keyName, "\"$value\"", DevToolsStringVal)
            is Number -> LeafValueRow(keyName, value.toString(), DevToolsNumberVal)
            is Boolean -> LeafValueRow(keyName, value.toString(), DevToolsKeyword)
            null -> LeafValueRow(keyName, "null", DevToolsDimArrow)
            else -> LeafValueRow(keyName, value.toString(), Color.White)
        }
    }
}

@Composable
private fun LeafValueRow(keyName: String?, displayValue: String, valueColor: Color) {
    Text(
        text = buildAnnotatedString {
            if (keyName != null) {
                withStyle(SpanStyle(color = DevToolsKeyName)) { append("$keyName: ") }
            }
            withStyle(SpanStyle(color = valueColor)) { append(displayValue) }
        },
        fontSize = 12.sp,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.padding(vertical = 1.dp)
    )
}