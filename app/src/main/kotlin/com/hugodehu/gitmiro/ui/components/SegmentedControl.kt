package com.hugodehu.gitmiro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hugodehu.gitmiro.extensions.handMouseClickable
import com.hugodehu.gitmiro.theme.onBackgroundSecondary

/**
 * Pill shaped toggle between a few labelled options.
 */
@Composable
fun <T> SegmentedControl(
    options: List<Pair<T, String>>,
    current: T,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colors.background)
            .border(1.dp, MaterialTheme.colors.onBackground.copy(alpha = 0.1f), MaterialTheme.shapes.medium)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEach { (value, label) ->
            val isSelected = value == current

            Text(
                text = label,
                style = MaterialTheme.typography.body2,
                fontWeight = FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colors.onBackground else MaterialTheme.colors.onBackgroundSecondary,
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .background(if (isSelected) MaterialTheme.colors.onBackground.copy(alpha = 0.14f) else Color.Transparent)
                    .handMouseClickable { if (!isSelected) onSelected(value) }
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }
    }
}
