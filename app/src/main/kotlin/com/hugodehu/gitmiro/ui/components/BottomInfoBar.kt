package com.hugodehu.gitmiro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import com.hugodehu.gitmiro.keybindings.Keybinding
import com.hugodehu.gitmiro.ui.KeybindingHint
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hugodehu.gitmiro.AppConstants
import com.hugodehu.gitmiro.app.generated.resources.Res
import com.hugodehu.gitmiro.app.generated.resources.bottom_info_bar_app_version
import com.hugodehu.gitmiro.app.generated.resources.bottom_info_bar_update_available
import com.hugodehu.gitmiro.app.generated.resources.refresh
import com.hugodehu.gitmiro.extensions.handMouseClickable
import com.hugodehu.gitmiro.theme.onBackgroundSecondary
import com.hugodehu.gitmiro.ui.components.tooltip.InstantTooltip
import com.hugodehu.gitmiro.ui.components.tooltip.InstantTooltipPosition
import com.hugodehu.gitmiro.ui.dialogs.UpdateDialog
import com.hugodehu.gitmiro.updates.Update
import com.hugodehu.gitmiro.updates.fetchAvailableUpdate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.seconds

@Composable
fun BottomInfoBar(
    newUpdate: Update?,
    onOpenUrlInBrowser: (String) -> Unit,
    leadingContent: @Composable (() -> Unit)? = null,
    actions: @Composable (RowScope.() -> Unit)? = null,
) {
    var showUpdateDialog by remember { mutableStateOf(false) }
    // Found by the check button, before the periodic check finds it too
    var checkedUpdate by remember { mutableStateOf<Update?>(null) }
    // "Checking…", then the result for a few seconds
    var checkStatus by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val update = newUpdate ?: checkedUpdate

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .background(MaterialTheme.colors.surface)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leadingContent?.invoke()

        Spacer(Modifier.weight(1f, true))

        if (actions != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = actions,
            )

            Box(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .size(width = 1.dp, height = 14.dp)
                    .background(MaterialTheme.colors.onBackground.copy(alpha = 0.12f))
            )
        }

        if (update != null) {
            SecondaryButton(
                text = stringResource(Res.string.bottom_info_bar_update_available, update.appVersion),
                onClick = { showUpdateDialog = true },
                backgroundButton = MaterialTheme.colors.primary,
                modifier = Modifier.padding(end = 16.dp)
            )
        } else if (AppConstants.VERSION_CHECK_URL != null) {
            checkStatus?.let { status ->
                Text(
                    status,
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onBackgroundSecondary,
                    maxLines = 1,
                    modifier = Modifier.padding(end = 8.dp),
                )
            }

            InstantTooltip(text = "Check for updates", position = InstantTooltipPosition.TOP) {
                Icon(
                    painterResource(Res.drawable.refresh),
                    contentDescription = "Check for updates",
                    tint = MaterialTheme.colors.onBackgroundSecondary,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(16.dp)
                        .handMouseClickable {
                            if (checkStatus != null) return@handMouseClickable

                            scope.launch {
                                checkStatus = "Checking…"

                                checkStatus = try {
                                    val found = fetchAvailableUpdate()

                                    if (found != null) {
                                        checkedUpdate = found
                                        showUpdateDialog = true
                                        null
                                    } else {
                                        "Up to date"
                                    }
                                } catch (ex: CancellationException) {
                                    throw ex
                                } catch (ex: Exception) {
                                    "Check failed"
                                }

                                if (checkStatus != null) {
                                    delay(3.seconds)
                                    checkStatus = null
                                }
                            }
                        },
                )
            }
        }

        // Not clickable: it used to open Gitnuro's releases and GitMiro has no public releases page
        Text(
            stringResource(Res.string.bottom_info_bar_app_version, AppConstants.APP_VERSION),
            style = MaterialTheme.typography.body2,
            maxLines = 1,
        )
    }

    if (showUpdateDialog && update != null) {
        UpdateDialog(
            update = update,
            onClose = { showUpdateDialog = false },
            onOpenUrlInBrowser = onOpenUrlInBrowser,
        )
    }
}

@Composable
fun BottomBarAction(
    title: String,
    icon: Painter,
    tooltip: String,
    keybinding: Keybinding? = null,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    InstantTooltip(
        text = tooltip,
        position = InstantTooltipPosition.TOP,
        trailingContent = keybinding?.let { { KeybindingHint(it) } },
    ) {
        Row(
            modifier = Modifier
                .height(26.dp)
                .clip(MaterialTheme.shapes.small)
                .background(
                    if (isHovered) MaterialTheme.colors.onBackground.copy(alpha = 0.07f) else Color.Transparent
                )
                .hoverable(interactionSource)
                .handMouseClickable { onClick() }
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = MaterialTheme.colors.onBackgroundSecondary,
                modifier = Modifier.size(16.dp),
            )

            Text(
                text = title,
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onBackground,
                maxLines = 1,
            )
        }
    }
}
