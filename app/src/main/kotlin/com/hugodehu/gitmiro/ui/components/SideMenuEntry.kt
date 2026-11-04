package com.hugodehu.gitmiro.ui.components

import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.hugodehu.gitmiro.extensions.handMouseClickable
import com.hugodehu.gitmiro.app.generated.resources.Res
import com.hugodehu.gitmiro.app.generated.resources.chevron_right
import com.hugodehu.gitmiro.app.generated.resources.expand_more
import com.hugodehu.gitmiro.theme.monoTypography
import com.hugodehu.gitmiro.theme.onBackgroundSecondary
import org.jetbrains.compose.resources.painterResource

/**
 * Section title of the side panel: small uppercase label with its count and, if any, a quick action.
 */
@Composable
fun SideMenuHeader(
    text: String,
    @Suppress("UNUSED_PARAMETER") icon: Painter? = null,
    itemsCount: Int,
    isExpanded: Boolean,
    onExpand: () -> Unit = {},
    hoverIcon: @Composable (() -> Unit)? = null,
) {
    val hoverInteraction = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .height(30.dp)
            .fillMaxWidth()
            .hoverable(hoverInteraction)
            .handMouseClickable { onExpand() }
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(if (isExpanded) Res.drawable.expand_more else Res.drawable.chevron_right),
            contentDescription = null,
            tint = MaterialTheme.colors.onBackgroundSecondary,
            modifier = Modifier
                .padding(end = 8.dp)
                .size(14.dp),
        )

        Text(
            text = text.uppercase(),
            modifier = Modifier.weight(1f),
            maxLines = 1,
            fontSize = 11.sp,
            letterSpacing = 0.06.em,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colors.onBackgroundSecondary,
            overflow = TextOverflow.Ellipsis,
        )

        Text(
            text = itemsCount.toString(),
            fontSize = 11.sp,
            fontFamily = monoTypography(),
            color = MaterialTheme.colors.onBackgroundSecondary,
        )

        if (hoverIcon != null) {
            Box(modifier = Modifier.padding(start = 6.dp)) {
                hoverIcon()
            }
        }
    }
}
