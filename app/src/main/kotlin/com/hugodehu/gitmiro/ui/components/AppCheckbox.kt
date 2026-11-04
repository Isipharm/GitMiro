package com.hugodehu.gitmiro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import com.hugodehu.gitmiro.app.generated.resources.Res
import com.hugodehu.gitmiro.app.generated.resources.done
import com.hugodehu.gitmiro.extensions.handMouseClickable
import com.hugodehu.gitmiro.theme.onBackgroundSecondary
import org.jetbrains.compose.resources.painterResource

private val CHECKBOX_SHAPE = RoundedCornerShape(4.dp)

/**
 * 16dp rounded checkbox following the Graphite style, with an indeterminate state.
 */
@Composable
fun AppCheckbox(
    state: ToggleableState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val isOn = state != ToggleableState.Off

    Box(
        modifier = modifier
            .size(16.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(CHECKBOX_SHAPE)
            .background(if (isOn) MaterialTheme.colors.primary else Color.Transparent)
            .border(
                width = 1.5.dp,
                color = if (isOn) MaterialTheme.colors.primary else MaterialTheme.colors.onBackgroundSecondary,
                shape = CHECKBOX_SHAPE,
            )
            .handMouseClickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        when (state) {
            ToggleableState.On -> Icon(
                painter = painterResource(Res.drawable.done),
                contentDescription = null,
                tint = MaterialTheme.colors.onPrimary,
                modifier = Modifier.size(12.dp),
            )

            ToggleableState.Indeterminate -> Box(
                Modifier
                    .width(8.dp)
                    .height(2.dp)
                    .background(MaterialTheme.colors.onPrimary)
            )

            ToggleableState.Off -> {}
        }
    }
}

@Composable
fun AppCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) = AppCheckbox(
    state = if (checked) ToggleableState.On else ToggleableState.Off,
    onClick = { onCheckedChange(!checked) },
    modifier = modifier,
    enabled = enabled,
)
