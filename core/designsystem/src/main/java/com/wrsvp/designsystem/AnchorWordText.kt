package com.wrsvp.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.wear.compose.material.Text

@Composable
fun AnchorWordText(
    before: String,
    anchor: String,
    after: String,
    fontSize: TextUnit,
    anchorColor: Color = Color(0xFFFF4D4D),
) {
    Text(
        text = buildAnnotatedString {
            append(before)
            withStyle(SpanStyle(color = anchorColor, fontWeight = FontWeight.Bold)) {
                append(anchor)
            }
            append(after)
        },
        fontSize = fontSize,
    )
}
