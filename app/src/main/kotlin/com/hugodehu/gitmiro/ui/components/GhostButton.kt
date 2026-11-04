package com.hugodehu.gitmiro.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hugodehu.gitmiro.extensions.handMouseClickable

/**
 * Transparent button with a thin border, for the secondary actions next to a primary one.
 *
 * @param color color of the text and border, the default is neutral
 */
@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = MaterialTheme.colors.onBackground,
) {
    val borderColor = if (color == MaterialTheme.colors.onBackground) color.copy(alpha = 0.16f) else color.copy(alpha = 0.5f)

    Box(
        modifier = modifier
            .heightIn(min = 32.dp)
            .clip(MaterialTheme.shapes.small)
            .border(1.dp, borderColor.copy(alpha = if (enabled) borderColor.alpha else 0.08f), MaterialTheme.shapes.small)
            .handMouseClickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.body2,
            fontWeight = FontWeight.Medium,
            color = color.copy(alpha = if (enabled) 1f else 0.4f),
        )
    }
}
