package com.hugodehu.gitmiro.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hugodehu.gitmiro.app.generated.resources.Res
import com.hugodehu.gitmiro.app.generated.resources.add
import com.hugodehu.gitmiro.app.generated.resources.close
import com.hugodehu.gitmiro.extensions.handMouseClickable
import com.hugodehu.gitmiro.extensions.onMiddleMouseButtonClick
import com.hugodehu.gitmiro.theme.onBackgroundSecondary
import com.hugodehu.gitmiro.theme.secondarySurface
import com.hugodehu.gitmiro.ui.components.tooltip.InstantTooltip
import com.hugodehu.gitmiro.ui.drag_sorting.HorizontalDraggableItem
import com.hugodehu.gitmiro.ui.drag_sorting.horizontalDragContainer
import com.hugodehu.gitmiro.ui.drag_sorting.rememberHorizontalDragDropState
import kotlinx.coroutines.flow.firstOrNull
import org.jetbrains.compose.resources.painterResource

private val TAB_HEIGHT = 32.dp

/**
 * Repository tabs, meant to be laid out in the window title bar.
 *
 * @param clientArea marks a part as interactive for the title bar, so a click there is not a window drag
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T : TabInformationProvider> TabsRow(
    tabs: List<TabInformation<T>>,
    defaultTabName: String,
    currentTab: TabInformation<T>?,
    onTabSelected: (TabInformation<T>) -> Unit,
    onTabClosed: (TabInformation<T>) -> Unit,
    onMoveTab: (Int, Int) -> Unit,
    onAddNewTab: () -> Unit,
    clientArea: @Composable (Modifier) -> Modifier,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    var latestTabCount by remember { mutableStateOf(tabs.count()) }

    val dragDropState = rememberHorizontalDragDropState(listState) { fromIndex, toIndex ->
        onMoveTab(fromIndex, toIndex)
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LazyRow(
            modifier = clientArea(
                Modifier
                    .height(TAB_HEIGHT)
                    .weight(1f, false)
                    .horizontalDragContainer(
                        dragDropState = dragDropState,
                        onDraggedItem = {
                            val tab = tabs.getOrNull(it)

                            if (tab != null) {
                                onTabSelected(tab)
                            }
                        },
                    )
            ),
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            itemsIndexed(
                items = tabs,
                key = { _, tab -> tab.data }
            ) { index, tab ->
                HorizontalDraggableItem(dragDropState, index) { _ ->
                    val tooltip by tab.extraInfo.collectAsState()
                    val tabName by tab.name.collectAsState()

                    InstantTooltip(tooltip) {
                        Tab(
                            title = tabName ?: defaultTabName,
                            isSelected = currentTab == tab,
                            onClick = { onTabSelected(tab) },
                            onCloseTab = { onTabClosed(tab) },
                        )
                    }
                }
            }
        }

        InstantTooltip("Open a repository") {
            Box(
                modifier = clientArea(
                    Modifier
                        .size(TAB_HEIGHT)
                        .clip(MaterialTheme.shapes.small)
                        .handMouseClickable { onAddNewTab() }
                ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(Res.drawable.add),
                    contentDescription = "Open a repository",
                    tint = MaterialTheme.colors.onBackgroundSecondary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }

    LaunchedEffect(tabs.count()) {
        // Show the new tab if it's empty (so it's not a new submodule tab)
        // TODO Use TabInformationProvider to know if the app should scroll to the tab or not instead of relying on extra info
        if (latestTabCount < tabs.count() && currentTab?.extraInfo?.firstOrNull() == null) {
            listState.animateScrollToItem(tabs.lastIndex)
        }

        latestTabCount = tabs.count()
    }
}

@Composable
private fun Tab(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    onCloseTab: () -> Unit,
) {
    val hoverInteraction = remember { MutableInteractionSource() }
    val isHovered by hoverInteraction.collectIsHoveredAsState()
    val showClose = isHovered || isSelected

    val tabModifier = if (isSelected) {
        Modifier
            .background(MaterialTheme.colors.secondarySurface, MaterialTheme.shapes.small)
            .border(1.dp, MaterialTheme.colors.onBackground.copy(alpha = 0.1f), MaterialTheme.shapes.small)
    } else if (isHovered) {
        Modifier.background(MaterialTheme.colors.onBackground.copy(alpha = 0.06f), MaterialTheme.shapes.small)
    } else {
        Modifier
    }

    Row(
        modifier = tabModifier
            .height(TAB_HEIGHT)
            .widthIn(max = 260.dp)
            .clip(MaterialTheme.shapes.small)
            .hoverable(hoverInteraction)
            .handMouseClickable { onClick() }
            .onMiddleMouseButtonClick { onCloseTab() }
            .padding(start = 14.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f, false),
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.body2,
            fontWeight = FontWeight.Medium,
            color = if (isSelected) MaterialTheme.colors.onBackground else MaterialTheme.colors.onBackgroundSecondary,
            maxLines = 1,
            softWrap = false,
        )

        Box(
            modifier = Modifier
                .padding(start = 10.dp)
                .size(22.dp)
                .alpha(if (showClose) 1f else 0f)
                .clip(MaterialTheme.shapes.small)
                .handMouseClickable(enabled = showClose) { onCloseTab() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(Res.drawable.close),
                contentDescription = "Close tab",
                tint = MaterialTheme.colors.onBackgroundSecondary,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}
