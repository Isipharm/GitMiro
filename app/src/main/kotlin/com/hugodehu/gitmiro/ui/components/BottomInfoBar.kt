package com.hugodehu.gitmiro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
