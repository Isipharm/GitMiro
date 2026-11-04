package com.hugodehu.gitmiro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hugodehu.gitmiro.app.generated.resources.Res
import com.hugodehu.gitmiro.app.generated.resources.done
import com.hugodehu.gitmiro.extensions.handMouseClickable
import com.hugodehu.gitmiro.theme.onBackgroundSecondary
import org.jetbrains.compose.resources.painterResource

private val CHECKBOX_SHAPE = RoundedCornerShape(4.dp)

@Composable
fun CheckboxText(
    value: Boolean,
    onCheckedChange: () -> Unit,
    text: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .handMouseClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onCheckedChange,
            )
            .padding(vertical = 6.dp, horizontal = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CHECKBOX_SHAPE)
                .background(if (value) MaterialTheme.colors.primary else Color.Transparent)
                .border(
                    width = 1.5.dp,
                    color = if (value) MaterialTheme.colors.primary else MaterialTheme.colors.onBackgroundSecondary,
                    shape = CHECKBOX_SHAPE,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (value) {
                Icon(
                    painter = painterResource(Res.drawable.done),
                    contentDescription = null,
                    tint = MaterialTheme.colors.onPrimary,
                    modifier = Modifier.size(12.dp),
                )
            }
        }

        Text(
            text,
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onBackground,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}
