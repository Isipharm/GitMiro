package com.hugodehu.gitmiro.ui

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.hugodehu.gitmiro.app.generated.resources.*
import com.hugodehu.gitmiro.domain.models.PullType
import com.hugodehu.gitmiro.extensions.handMouseClickable
import com.hugodehu.gitmiro.extensions.ignoreKeyEvents
import com.hugodehu.gitmiro.keybindings.Keybinding
import com.hugodehu.gitmiro.keybindings.KeybindingOption
import com.hugodehu.gitmiro.keybindings.keyBinding
import com.hugodehu.gitmiro.repositoryopen.RepositoryOpenViewModel
import com.hugodehu.gitmiro.theme.monoTypography
import com.hugodehu.gitmiro.theme.onBackgroundSecondary
import com.hugodehu.gitmiro.ui.components.tooltip.InstantTooltip
import com.hugodehu.gitmiro.ui.context_menu.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private const val DISABLED_BUTTON_ALPHA = 0.5F

@Composable
fun Menu(
    modifier: Modifier,
    viewModel: RepositoryOpenViewModel,
    onStashWithMessage: () -> Unit,
) {
    val isPullWithRebaseDefault by viewModel.isPullWithRebaseDefault.collectAsState(false)
    val hasUncommittedChanges by viewModel.hasUncommittedChanges.collectAsState()
    val stashesState by viewModel.stashesState.collectAsState()
    val remotes by viewModel.remoteState.collectAsState()
    val hasRemotes = remotes.remotes.isNotEmpty()

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val pullTooltip = if (!hasRemotes) {
            stringResource(Res.string.menu_pull_tooltip_disabled)
        } else if (isPullWithRebaseDefault) {
            stringResource(Res.string.menu_pull_rebase)
        } else {
            stringResource(Res.string.menu_pull_default)
        }

        ExtendedMenuButton(
            title = stringResource(Res.string.menu_pull),
            tooltipText = pullTooltip,
            enabled = hasRemotes,
            icon = painterResource(Res.drawable.download),
            keybinding = KeybindingOption.PULL.keyBinding,
            onClick = { viewModel.pull(PullType.DEFAULT) },
            extendedListItems = pullContextMenuItems(
                isPullWithRebaseDefault = isPullWithRebaseDefault,
                onPullWith = {
                    // Do the reverse of the default
                    val pullType = if (isPullWithRebaseDefault) {
                        PullType.MERGE
                    } else {
                        PullType.REBASE
                    }

                    viewModel.pull(pullType = pullType)
                },
                onFetchAll = {
                    viewModel.fetchAll()
                },
                onPrune = {
                    viewModel.prune()
                },
            )
        )

        val pushTooltip = if (!hasRemotes) {
            stringResource(Res.string.menu_push_tooltip_disabled)
        } else {
            stringResource(Res.string.menu_push_tooltip)
        }

        ExtendedMenuButton(
            title = stringResource(Res.string.menu_push),
            tooltipText = pushTooltip,
            enabled = hasRemotes,
            icon = painterResource(Res.drawable.upload),
            onClick = { viewModel.push(force = false, pushTags = false) },
            keybinding = KeybindingOption.PUSH.keyBinding,
            extendedListItems = pushContextMenuItems(
                onPushWithTags = {
                    viewModel.push(force = false, pushTags = true)
                },
                onForcePush = {
                    viewModel.push(force = true, pushTags = false)
                }
            )
        )

        val enableStash = hasUncommittedChanges
        val enablePopStash = stashesState.stashes.isNotEmpty()

        val stashTooltipText = if (enableStash) {
            stringResource(Res.string.menu_stash_tooltip)
        } else {
            stringResource(Res.string.menu_stash_tooltip_disabled)
        }

        ExtendedMenuButton(
            title = stringResource(Res.string.menu_stash),
            tooltipText = stashTooltipText,
            enabled = enableStash,
            // Pop stash stays reachable while there is nothing to stash
            isDropdownEnabled = enableStash || enablePopStash,
            icon = painterResource(Res.drawable.stash),
            keybinding = if (enableStash) KeybindingOption.STASH.keyBinding else null,
            onClick = { viewModel.stash() },
            extendedListItems = stashContextMenuItems(
                canStash = enableStash,
                canPopStash = enablePopStash,
                onStashWithMessage = onStashWithMessage,
                onPopStash = { viewModel.popStash() },
            )
        )
    }
}

@Composable
fun MenuButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    title: String,
    icon: Painter,
    keybinding: Keybinding?,
    tooltip: String,
    tooltipEnabled: Boolean = true,
    showTitle: Boolean = true,
    onClick: () -> Unit,
) {
    val keybinding = if (enabled) keybinding else null

    InstantTooltip(
        text = if (showTitle) tooltip else "$title: $tooltip",
        enabled = tooltipEnabled,
        trailingContent = if (keybinding != null) {
            { KeybindingHint(keybinding) }
        } else {
            null
        }
    ) {
        val color = MaterialTheme.colors.onBackground.copy(alpha = if (enabled) 1F else DISABLED_BUTTON_ALPHA)

        Row(
            modifier = modifier
                .ignoreKeyEvents()
                .height(MENU_BUTTON_HEIGHT)
                .clip(MaterialTheme.shapes.small)
                .menuButtonHover(enabled)
                .handMouseClickable(enabled) { onClick() }
                .padding(horizontal = if (showTitle) 10.dp else 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = icon,
                contentDescription = title,
                modifier = Modifier.size(18.dp),
                tint = color,
            )

            if (showTitle) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.body2,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    color = color,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
    }
}

private val MENU_BUTTON_HEIGHT = 32.dp

@Composable
private fun Modifier.menuButtonHover(enabled: Boolean): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    return this
        .hoverable(interactionSource, enabled)
        .background(
            if (isHovered && enabled) MaterialTheme.colors.onBackground.copy(alpha = 0.07f) else Color.Transparent
        )
}

@Composable
fun KeybindingHint(keybinding: Keybinding) {
    val parts = remember(keybinding) { getParts(keybinding) }.joinToString("+")

    Text(
        parts,
        fontFamily = monoTypography(),
        fontSize = MaterialTheme.typography.caption.fontSize,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colors.onBackgroundSecondary,
    )
}

@Preview
@Composable
fun KeybindingHintPartPreview() {
    KeybindingHintPart("CTRL")
}

@Composable
fun KeybindingHintPart(part: String) {
    Text(
        text = part,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colors.primary,
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .border(2.dp, MaterialTheme.colors.primary, MaterialTheme.shapes.small)
            .background(MaterialTheme.colors.primary.copy(alpha = 0.05f))
            .padding(horizontal = 4.dp, vertical = 4.dp)

    )
}

fun getParts(keybinding: Keybinding): List<String> {
    val parts = mutableListOf<String>()

    if (keybinding.control) {
        parts.add("Ctrl")
    }

    if (keybinding.meta) {
        parts.add("⌘")
    }

    if (keybinding.alt) {
        parts.add("Alt")
    }

    if (keybinding.shift) {
        parts.add("Shift")
    }

    val key = when (keybinding.key) {
        Key.A -> "A"
        Key.B -> "B"
        Key.C -> "C"
        Key.D -> "D"
        Key.E -> "E"
        Key.F -> "F"
        Key.G -> "G"
        Key.H -> "H"
        Key.I -> "I"
        Key.J -> "J"
        Key.K -> "K"
        Key.L -> "L"
        Key.M -> "M"
        Key.N -> "N"
        Key.O -> "O"
        Key.P -> "P"
        Key.Q -> "Q"
        Key.R -> "R"
        Key.S -> "S"
        Key.T -> "T"
        Key.U -> "U"
        Key.V -> "V"
        Key.W -> "W"
        Key.X -> "X"
        Key.Y -> "Y"
        Key.Z -> "Z"
        Key.Tab -> "Tab"
        else -> throw NotImplementedError("Key not implemented")
    }

    parts.add(key)

    return parts
}

@Composable
fun ExtendedMenuButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isDropdownEnabled: Boolean = enabled,
    title: String,
    tooltipText: String,
    icon: Painter,
    keybinding: Keybinding?,
    onClick: () -> Unit,
    extendedListItems: List<ContextMenuElement>,
) {
    val color = MaterialTheme.colors.onBackground.copy(alpha = if (enabled) 1F else DISABLED_BUTTON_ALPHA)
    val dropdownColor = MaterialTheme.colors.onBackground.copy(alpha = if (isDropdownEnabled) 1F else DISABLED_BUTTON_ALPHA)
    val keybinding = if (enabled) keybinding else null

    Row(
        modifier = modifier
            .height(MENU_BUTTON_HEIGHT)
            .ignoreKeyEvents()
            .clip(MaterialTheme.shapes.small)
            .menuButtonHover(enabled || isDropdownEnabled)
    ) {
        InstantTooltip(
            text = tooltipText,
            modifier = Modifier.fillMaxHeight(),
            trailingContent = if (keybinding != null) {
                { KeybindingHint(keybinding) }
            } else {
                null
            }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .handMouseClickable(enabled) { onClick() }
                    .padding(start = 10.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = icon,
                    contentDescription = title,
                    modifier = Modifier.size(18.dp),
                    tint = color,
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.body2,
                    fontWeight = FontWeight.Medium,
                    color = color,
                    maxLines = 1,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }

        DropDownMenu(
            enabled = isDropdownEnabled,
            items = { extendedListItems }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(end = 6.dp)
                    .ignoreKeyEvents(),
                contentAlignment = Alignment.Center,
            ) {

                Icon(
                    painterResource(Res.drawable.expand_more),
                    contentDescription = null,
                    tint = dropdownColor,
                    modifier = Modifier.size(14.dp)
                )

            }
        }
    }
}
