package com.hugodehu.gitmiro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.hugodehu.gitmiro.domain.extensions.dirName
import com.hugodehu.gitmiro.domain.extensions.lowercaseContains
import com.hugodehu.gitmiro.extensions.handMouseClickable
import com.hugodehu.gitmiro.extensions.onMiddleMouseButtonClick
import com.hugodehu.gitmiro.keybindings.KeybindingOption
import com.hugodehu.gitmiro.keybindings.keyBinding
import com.hugodehu.gitmiro.keybindings.matchesBinding
import com.hugodehu.gitmiro.theme.backgroundSelected
import com.hugodehu.gitmiro.theme.graphColors
import com.hugodehu.gitmiro.theme.onBackgroundSecondary
import com.hugodehu.gitmiro.ui.KeybindingHint
import com.hugodehu.gitmiro.ui.components.tooltip.InstantTooltip

private const val MAX_RECENT_REPOSITORIES = 8
private val COLOR_MARK_SHAPE = RoundedCornerShape(3.dp)

// Stable color per repository name, so a repository keeps its color between sessions
@Composable
fun repositoryColor(name: String): Color {
    val colors = MaterialTheme.colors.graphColors
    return colors[Math.floorMod(name.hashCode(), colors.size)]
}

private sealed interface SwitcherEntry {
    val name: String
    val path: String?

    data class Open(val index: Int, override val name: String, override val path: String?) : SwitcherEntry
    data class Recent(override val path: String) : SwitcherEntry {
        override val name = path.dirName
    }
}

/**
 * Current repository in the title bar. A click (or the open repository keybinding) opens a palette to switch between
 * the open repositories, reopen a recent one or open a new one.
 */
@Composable
fun <T : TabInformationProvider> RepositorySwitcher(
    tabs: List<TabInformation<T>>,
    currentTab: TabInformation<T>,
    recentRepositories: List<String>,
    defaultTabName: String,
    isOpen: Boolean,
    onOpenChange: (Boolean) -> Unit,
    onTabSelected: (TabInformation<T>) -> Unit,
    onTabClosed: (TabInformation<T>) -> Unit,
    onOpenRecent: (String) -> Unit,
    onOpenNew: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val name = currentTab.name.collectAsState().value ?: defaultTabName
    val path = currentTab.extraInfo.collectAsState().value
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = modifier) {
            val keybinding = KeybindingOption.OPEN_REPOSITORY.keyBinding

            InstantTooltip(
                text = "Switch repository",
                trailingContent = keybinding?.let { { KeybindingHint(it) } },
                enabled = !isOpen,
            ) {
                Row(
                    modifier = Modifier
                        .height(34.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(
                            if (isHovered || isOpen)
                                MaterialTheme.colors.background
                            else
                                MaterialTheme.colors.background.copy(alpha = 0.6f)
                        )
                        .hoverable(interactionSource)
                        .handMouseClickable { onOpenChange(!isOpen) }
                        .padding(start = 12.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    RepositoryColorMark(name)

                    Text(
                        text = name,
                        style = MaterialTheme.typography.body1,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colors.onBackground,
                        maxLines = 1,
                    )

                    if (path != null) {
                        Text(
                            text = path.shortenedHome(),
                            style = MaterialTheme.typography.body2,
                            color = MaterialTheme.colors.onBackgroundSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 240.dp),
                        )
                    }

                    Icon(
                        imageVector = Icons.Outlined.ExpandMore,
                        contentDescription = "Switch repository",
                        tint = MaterialTheme.colors.onBackgroundSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            if (isOpen) {
                val offsetY = with(LocalDensity.current) { 40.dp.roundToPx() }

                Popup(
                    offset = IntOffset(0, offsetY),
                    onDismissRequest = { onOpenChange(false) },
                    properties = PopupProperties(focusable = true),
                ) {
                    RepositoryPalette(
                        tabs = tabs,
                        currentTab = currentTab,
                        recentRepositories = recentRepositories,
                        defaultTabName = defaultTabName,
                        onDismiss = { onOpenChange(false) },
                        onTabSelected = onTabSelected,
                        onTabClosed = onTabClosed,
                        onOpenRecent = onOpenRecent,
                        onOpenNew = onOpenNew,
                    )
                }
            }
        }

        if (tabs.count() > 1) {
            Text(
                text = "${tabs.count()} open",
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onBackgroundSecondary,
                maxLines = 1,
                modifier = Modifier.padding(start = 10.dp, end = 16.dp),
            )
        }
    }
}

@Composable
private fun <T : TabInformationProvider> RepositoryPalette(
    tabs: List<TabInformation<T>>,
    currentTab: TabInformation<T>,
    recentRepositories: List<String>,
    defaultTabName: String,
    onDismiss: () -> Unit,
    onTabSelected: (TabInformation<T>) -> Unit,
    onTabClosed: (TabInformation<T>) -> Unit,
    onOpenRecent: (String) -> Unit,
    onOpenNew: () -> Unit,
) {
    var filter by remember { mutableStateOf("") }
    val searchFocusRequester = remember { FocusRequester() }

    val openEntries = tabs.mapIndexed { index, tab ->
        SwitcherEntry.Open(index, tab.name.value ?: defaultTabName, tab.extraInfo.value)
    }
    val openPaths = openEntries.mapNotNull { it.path }.toSet()
    val recentEntries = recentRepositories
        .filter { it !in openPaths }
        .map { SwitcherEntry.Recent(it) }

    val matches: (SwitcherEntry) -> Boolean = {
        filter.isBlank() || it.name.lowercaseContains(filter) || it.path?.lowercaseContains(filter) == true
    }
    val filteredOpen = openEntries.filter(matches)
    val filteredRecent = recentEntries.filter(matches).take(MAX_RECENT_REPOSITORIES)
    val entries = filteredOpen + filteredRecent

    var focusedIndex by remember { mutableStateOf(tabs.indexOf(currentTab).coerceAtLeast(0)) }

    LaunchedEffect(filter) {
        if (filter.isNotEmpty()) focusedIndex = 0
    }

    val onEntrySelected: (SwitcherEntry) -> Unit = { entry ->
        onDismiss()
        when (entry) {
            is SwitcherEntry.Open -> onTabSelected(tabs[entry.index])
            is SwitcherEntry.Recent -> onOpenRecent(entry.path)
        }
    }

    Column(
        modifier = Modifier
            .width(440.dp)
            .heightIn(max = 560.dp)
            .shadow(24.dp, MaterialTheme.shapes.large)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colors.background)
            .padding(8.dp)
            .onPreviewKeyEvent {
                if (it.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false

                when {
                    it.matchesBinding(KeybindingOption.DOWN) -> {
                        if (focusedIndex < entries.lastIndex) focusedIndex += 1
                        true
                    }

                    it.matchesBinding(KeybindingOption.UP) -> {
                        if (focusedIndex > 0) focusedIndex -= 1
                        true
                    }

                    it.matchesBinding(KeybindingOption.SIMPLE_ACCEPT) -> {
                        entries.getOrNull(focusedIndex)?.let(onEntrySelected)
                        true
                    }

                    it.matchesBinding(KeybindingOption.EXIT) -> {
                        if (filter.isEmpty()) onDismiss() else filter = ""
                        true
                    }

                    else -> false
                }
            }
    ) {
        AdjustableOutlinedTextField(
            value = filter,
            onValueChange = { filter = it },
            hint = "Search repositories",
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(searchFocusRequester),
        )

        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .padding(top = 4.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            if (filteredOpen.isNotEmpty()) {
                PaletteSectionTitle("Open")
            }

            filteredOpen.forEachIndexed { index, entry ->
                PaletteRow(
                    entry = entry,
                    isCurrent = tabs[entry.index] == currentTab,
                    isFocused = index == focusedIndex,
                    onHover = { focusedIndex = index },
                    onClick = { onEntrySelected(entry) },
                    onClose = { onTabClosed(tabs[entry.index]) },
                )
            }

            if (filteredRecent.isNotEmpty()) {
                PaletteSectionTitle("Recent")
            }

            filteredRecent.forEachIndexed { index, entry ->
                val entryIndex = filteredOpen.count() + index

                PaletteRow(
                    entry = entry,
                    isCurrent = false,
                    isFocused = entryIndex == focusedIndex,
                    onHover = { focusedIndex = entryIndex },
                    onClick = { onEntrySelected(entry) },
                    onClose = null,
                )
            }

            if (entries.isEmpty()) {
                Text(
                    text = "No repository matches \"$filter\"",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onBackgroundSecondary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
                )
            }
        }

        Box(
            modifier = Modifier
                .padding(vertical = 6.dp)
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colors.onBackground.copy(alpha = 0.08f))
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .clip(MaterialTheme.shapes.small)
                .handMouseClickable {
                    onDismiss()
                    onOpenNew()
                }
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = MaterialTheme.colors.primary,
                modifier = Modifier.size(18.dp),
            )

            Text(
                text = "Open or clone a repository",
                style = MaterialTheme.typography.body2,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colors.primary,
            )
        }
    }

    LaunchedEffect(Unit) {
        searchFocusRequester.requestFocus()
    }
}

@Composable
private fun PaletteSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.caption,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colors.onBackgroundSecondary,
        modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 4.dp),
    )
}

@Composable
private fun PaletteRow(
    entry: SwitcherEntry,
    isCurrent: Boolean,
    isFocused: Boolean,
    onHover: () -> Unit,
    onClick: () -> Unit,
    onClose: (() -> Unit)?,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    LaunchedEffect(isHovered) {
        if (isHovered) onHover()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(MaterialTheme.shapes.small)
            .background(
                when {
                    isFocused -> MaterialTheme.colors.backgroundSelected
                    else -> Color.Transparent
                }
            )
            .hoverable(interactionSource)
            .handMouseClickable { onClick() }
            .let { if (onClose != null) it.onMiddleMouseButtonClick { onClose() } else it }
            .padding(start = 10.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (entry is SwitcherEntry.Recent) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(COLOR_MARK_SHAPE)
                    .background(MaterialTheme.colors.onBackgroundSecondary.copy(alpha = 0.3f))
            )
        } else {
            RepositoryColorMark(entry.name)
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.name,
                style = MaterialTheme.typography.body2,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                color = MaterialTheme.colors.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            val path = entry.path
            if (path != null) {
                Text(
                    text = path.shortenedHome(),
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onBackgroundSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (isCurrent) {
            Text(
                text = "Current",
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onBackgroundSecondary,
            )
        }

        if (onClose != null && (isHovered || isFocused)) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(MaterialTheme.shapes.small)
                    .handMouseClickable { onClose() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colors.onBackgroundSecondary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun RepositoryColorMark(name: String) {
    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(COLOR_MARK_SHAPE)
            .background(repositoryColor(name))
    )
}

private fun String.shortenedHome(): String {
    val home = System.getProperty("user.home")
    return if (startsWith(home)) "~${removePrefix(home)}" else this
}
