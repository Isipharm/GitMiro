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
import com.hugodehu.gitmiro.app.generated.resources.more_vert
import androidx.compose.foundation.shape.CircleShape
import com.hugodehu.gitmiro.theme.monoTypography
import com.hugodehu.gitmiro.theme.addFile
import com.hugodehu.gitmiro.theme.onBackgroundSecondary
import com.hugodehu.gitmiro.ui.context_menu.ContextMenuElement
import com.hugodehu.gitmiro.ui.context_menu.DropDownMenu
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
    count: Int? = null,
    stats: Pair<Int, Int>? = null,
    extraItems: List<ContextMenuElement> = emptyList(),
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
                .height(48.dp)
                .fillMaxWidth()
                .hoverable(headerHoverInteraction),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (stats != null && count != null) {
                // Commit summary: "14 files +1,204 -18"
                Text(
                    modifier = Modifier.padding(start = 18.dp),
                    text = if (count == 1) "1 file" else "$count files",
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colors.onBackground,
                    style = MaterialTheme.typography.caption,
                    maxLines = 1,
                )
                Text(
                    modifier = Modifier.padding(start = 10.dp),
                    text = "+%,d".format(stats.first),
                    color = MaterialTheme.colors.addFile,
                    style = MaterialTheme.typography.caption,
                    maxLines = 1,
                )
                Text(
                    modifier = Modifier.padding(start = 6.dp),
                    text = "-%,d".format(stats.second),
                    color = MaterialTheme.colors.error,
                    style = MaterialTheme.typography.caption,
                    maxLines = 1,
                )
            } else {
                Text(
                    modifier = Modifier.padding(start = 14.dp),
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Left,
                    color = MaterialTheme.colors.onBackground,
                    style = MaterialTheme.typography.body2,
                    maxLines = 1,
                )

                if (count != null) {
                    Text(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colors.onBackground.copy(alpha = 0.08f))
                            .padding(horizontal = 7.dp, vertical = 1.dp),
                        text = count.toString(),
                        fontFamily = monoTypography(),
                        color = MaterialTheme.colors.onBackgroundSecondary,
                        style = MaterialTheme.typography.caption,
                        maxLines = 1,
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

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

            if (actionInfo != null && onAllAction != null) {
                // The stage action is the primary one, unstaging stays neutral
                val isPrimary = actionInfo.color == MaterialTheme.colors.primary

                HeaderTextButton(
                    text = if (showActionForSelected) actionInfo.applyToSelectedTitle else actionInfo.applyToAllTitle,
                    isPrimary = isPrimary,
                    enabled = count == null || count > 0,
                    onClick = onAllAction,
                )
            }

            val overflowItems = buildList {
                addAll(extraItems)

                if (onDiscardAll != null) {
                    if (extraItems.isNotEmpty()) add(ContextMenuElement.ContextSeparator)

                    add(
                        ContextMenuElement.ContextTextEntry(
                            label = "Discard all changes",
                            icon = { painterResource(Res.drawable.delete) },
                            onClick = onDiscardAll,
                            isDanger = true,
                        )
                    )
                }
            }

            if (overflowItems.isNotEmpty()) {
                DropDownMenu(items = { overflowItems }) {
                    HeaderIconButton(
                        icon = painterResource(Res.drawable.more_vert),
                        tooltip = "More actions",
                        onClick = null,
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))
        }

        PanelDivider()

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
private fun HeaderTextButton(
    text: String,
    isPrimary: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val color = when {
        !enabled -> MaterialTheme.colors.onBackgroundSecondary.copy(alpha = 0.5f)
        isPrimary -> MaterialTheme.colors.primaryVariant
        else -> MaterialTheme.colors.onBackgroundSecondary
    }

    Text(
        text = text,
        color = color,
        fontWeight = FontWeight.SemiBold,
        style = MaterialTheme.typography.body2,
        maxLines = 1,
        modifier = Modifier
            .padding(horizontal = 2.dp)
            .clip(MaterialTheme.shapes.small)
            .background(if (isPrimary && enabled) MaterialTheme.colors.primary.copy(alpha = 0.16f) else Color.Transparent)
            .handMouseClickable(enabled) { onClick() }
            .padding(horizontal = 12.dp, vertical = 5.dp),
    )
}

@Composable
private fun HeaderIconButton(
    icon: Painter,
    tooltip: String,
    tint: Color = MaterialTheme.colors.onBackground,
    // Null when a parent handles the click, as the dropdown menu does
    onClick: (() -> Unit)?,
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
                .then(if (onClick != null) Modifier.handMouseClickable { onClick() } else Modifier),
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
