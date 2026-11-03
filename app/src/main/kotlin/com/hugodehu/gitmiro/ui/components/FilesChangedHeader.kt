package com.hugodehu.gitmiro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hugodehu.gitmiro.extensions.handMouseClickable
import com.hugodehu.gitmiro.ui.components.tooltip.InstantTooltip
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import com.hugodehu.gitmiro.app.generated.resources.Res
import com.hugodehu.gitmiro.app.generated.resources.delete
import com.hugodehu.gitmiro.app.generated.resources.list
import com.hugodehu.gitmiro.app.generated.resources.search
import com.hugodehu.gitmiro.app.generated.resources.tree
import com.hugodehu.gitmiro.theme.tertiarySurface
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Immutable
data class ActionInfo(
    val applyToOneTitle: String,
    val applyToAllTitle: String,
    val applyToSelectedTitle: String,
    val icon: DrawableResource,
    val color: Color,
    val textColor: Color,
)

@Composable
fun FilesChangedHeader(
    title: String,
    showAsTree: Boolean,
    showSearch: Boolean,
    showActionForSelected: Boolean,
    actionInfo: ActionInfo? = null,
    onAllAction: (() -> Unit)? = null,
    onDiscardAll: (() -> Unit)? = null,
    onAlternateShowAsTree: () -> Unit,
    onSearchFilterToggled: (Boolean) -> Unit,
    onSearchFocused: () -> Unit,
    searchFilter: TextFieldValue,
    onSearchFilterChanged: (TextFieldValue) -> Unit,
) {
    val searchFocusRequester = remember { FocusRequester() }

    /**
     * State used to prevent the text field from getting the focus when returning from another tab
     */
    var requestFocus by remember { mutableStateOf(false) }

    val headerHoverInteraction = remember { MutableInteractionSource() }
    val isHeaderHovered by headerHoverInteraction.collectIsHoveredAsState()
    Column {
        Row(
            modifier = Modifier
                .height(34.dp)
                .fillMaxWidth()
                .background(color = MaterialTheme.colors.tertiarySurface)
                .hoverable(headerHoverInteraction),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier
                    .padding(start = 14.dp, end = 8.dp)
                    .weight(1f),
                text = title,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Left,
                color = MaterialTheme.colors.onBackground,
                style = MaterialTheme.typography.body2,
                maxLines = 1,
            )

            HeaderIconButton(
                icon = painterResource(if (showAsTree) Res.drawable.list else Res.drawable.tree),
                tooltip = if (showAsTree) "Show as a list" else "Show as a tree",
                onClick = onAlternateShowAsTree,
            )

            HeaderIconButton(
                icon = painterResource(Res.drawable.search),
                tooltip = "Search files",
                onClick = {
                    onSearchFilterToggled(!showSearch)

                    if (!showSearch)
                        requestFocus = true
                },
            )

            if (onDiscardAll != null) {
                HeaderIconButton(
                    icon = painterResource(Res.drawable.delete),
                    tooltip = "Discard all the changes of this list",
                    tint = MaterialTheme.colors.error,
                    onClick = onDiscardAll,
                )
            }

            if (actionInfo != null && onAllAction != null) {
                SecondaryButtonCompactable(
                    text = if (showActionForSelected) actionInfo.applyToSelectedTitle else actionInfo.applyToAllTitle,
                    icon = actionInfo.icon,
                    isParentHovered = isHeaderHovered,
                    backgroundButton = actionInfo.color,
                    onBackgroundColor = actionInfo.textColor,
                    onClick = onAllAction,
                    modifier = Modifier.padding(start = 4.dp, end = 16.dp),
                )
            }
        }

        if (showSearch) {
            SearchTextField(
                searchFilter = searchFilter,
                onSearchFilterChanged = onSearchFilterChanged,
                searchFocusRequester = searchFocusRequester,
                onSearchFocused = onSearchFocused,
                onClose = { onSearchFilterToggled(false) },
            )
        }

        LaunchedEffect(showSearch, requestFocus) {
            if (showSearch && requestFocus) {
                searchFocusRequester.requestFocus()
                requestFocus = false
            }
        }
    }
}

@Composable
private fun HeaderIconButton(
    icon: Painter,
    tooltip: String,
    tint: Color = MaterialTheme.colors.onBackground,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    InstantTooltip(text = tooltip) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(MaterialTheme.shapes.small)
                .background(if (isHovered) MaterialTheme.colors.onBackground.copy(alpha = 0.07f) else Color.Transparent)
                .hoverable(interactionSource)
                .handMouseClickable { onClick() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = icon,
                contentDescription = tooltip,
                modifier = Modifier.size(18.dp),
                tint = tint,
            )
        }
    }
}
