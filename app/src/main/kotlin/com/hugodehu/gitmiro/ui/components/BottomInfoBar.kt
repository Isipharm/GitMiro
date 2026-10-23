package com.hugodehu.gitmiro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.hugodehu.gitmiro.ui.dialogs.UpdateDialog
import com.hugodehu.gitmiro.updates.Update
import org.jetbrains.compose.resources.stringResource

@Composable
fun BottomInfoBar(
    newUpdate: Update?,
    onOpenUrlInBrowser: (String) -> Unit,
    leadingContent: @Composable (() -> Unit)? = null,
) {
    var showUpdateDialog by remember { mutableStateOf(false) }

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

        if (newUpdate != null) {
            SecondaryButton(
                text = stringResource(Res.string.bottom_info_bar_update_available, newUpdate.appVersion),
                onClick = { showUpdateDialog = true },
                backgroundButton = MaterialTheme.colors.primary,
                modifier = Modifier.padding(end = 16.dp)
            )
        }

        // Not clickable: it used to open Gitnuro's releases and GitMiro has no public releases page
        Text(
            stringResource(Res.string.bottom_info_bar_app_version, AppConstants.APP_VERSION),
            style = MaterialTheme.typography.body2,
            maxLines = 1,
        )
    }

    if (showUpdateDialog && newUpdate != null) {
        UpdateDialog(
            update = newUpdate,
            onClose = { showUpdateDialog = false },
            onOpenUrlInBrowser = onOpenUrlInBrowser,
        )
    }
}
