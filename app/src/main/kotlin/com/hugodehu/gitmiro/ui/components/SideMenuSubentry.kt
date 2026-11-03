@file:OptIn(ExperimentalComposeUiApi::class)

package com.hugodehu.gitmiro.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hugodehu.gitmiro.extensions.onDoubleClick
import com.hugodehu.gitmiro.theme.backgroundSelected
import com.hugodehu.gitmiro.theme.linesHeight
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SideMenuSubentry(
    text: String,
    fontWeight: FontWeight = FontWeight.Normal,
    iconResourcePath: DrawableResource,
    isSelected: Boolean,
    // The checked out branch, drawn in the accent color
    isCurrent: Boolean = false,
    extraPadding: Dp = 0.dp,
    onClick: () -> Unit,
    onDoubleClick: (() -> Unit)? = null,
    additionalInfo: @Composable () -> Unit = {},
) {
    val hoverInteraction = remember { MutableInteractionSource() }
    val isHovered by hoverInteraction.collectIsHoveredAsState()

    val contentColor = if (isCurrent) MaterialTheme.colors.primaryVariant else MaterialTheme.colors.onBackground
    val backgroundColor = when {
        isSelected -> MaterialTheme.colors.backgroundSelected
        isHovered -> MaterialTheme.colors.onBackground.copy(alpha = 0.05f)
        else -> Color.Transparent
    }

    Row(
        modifier = Modifier
            .padding(start = extraPadding)
            .height(MaterialTheme.linesHeight.sidePanelItemHeight)
            .fillMaxWidth()
            .clip(RoundedCornerShape7)
            .background(backgroundColor)
            .hoverable(hoverInteraction)
            .clickable { onClick() }
            .run {
                if (onDoubleClick != null)
                    this.onDoubleClick(onDoubleClick)
                else
                    this
            }
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(iconResourcePath),
            contentDescription = null,
            modifier = Modifier
                .padding(end = 9.dp)
                .size(14.dp),
            tint = contentColor,
        )

        Text(
            text = text,
            fontWeight = if (isCurrent) FontWeight.Medium else fontWeight,
            modifier = Modifier.weight(1f, fill = true),
            maxLines = 1,
            style = MaterialTheme.typography.body2,
            color = contentColor,
            softWrap = false,
        )

        additionalInfo()
    }
}

private val RoundedCornerShape7 = androidx.compose.foundation.shape.RoundedCornerShape(7.dp)
