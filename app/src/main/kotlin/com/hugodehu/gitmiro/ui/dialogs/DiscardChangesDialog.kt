package com.hugodehu.gitmiro.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hugodehu.gitmiro.app.generated.resources.Res
import com.hugodehu.gitmiro.app.generated.resources.delete
import com.hugodehu.gitmiro.app.generated.resources.generic_button_cancel
import com.hugodehu.gitmiro.domain.models.DiscardScope
import com.hugodehu.gitmiro.domain.models.StatusEntry
import com.hugodehu.gitmiro.domain.models.StatusType
import com.hugodehu.gitmiro.theme.dialogOverlay
import com.hugodehu.gitmiro.theme.onBackgroundSecondary
import com.hugodehu.gitmiro.ui.components.PrimaryButton
import com.hugodehu.gitmiro.ui.components.ScrollableLazyColumn
import com.hugodehu.gitmiro.ui.dialogs.base.MaterialDialog
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Confirms discarding uncommitted changes, with what happens to each file.
 */
@Composable
fun DiscardChangesDialog(
    scope: DiscardScope,
    staged: List<StatusEntry>,
    unstaged: List<StatusEntry>,
    onDiscard: () -> Unit,
    onDismiss: () -> Unit,
) {
    val entries = when (scope) {
        // A file can be both staged and unstaged: its staged entry tells best what happens to it
        DiscardScope.ALL -> (staged + unstaged).distinctBy { it.filePath }
        DiscardScope.STAGED -> staged
        DiscardScope.UNSTAGED -> unstaged
    }

    val title = when (scope) {
        DiscardScope.ALL -> "Discard all changes?"
        DiscardScope.STAGED -> "Discard staged changes?"
        DiscardScope.UNSTAGED -> "Discard unstaged changes?"
    }

    val explanation = when (scope) {
        DiscardScope.ALL -> "Every file goes back to its version in the last commit, whether its changes are " +
                "staged or not. New files, including untracked ones, are deleted."

        DiscardScope.STAGED -> "These files go back to their version in the last commit. If they also have " +
                "unstaged changes, those are lost too. New staged files are deleted. The other unstaged changes " +
                "are kept."

        DiscardScope.UNSTAGED -> "These files go back to their staged version, or to the last commit when " +
                "nothing is staged. Untracked files are deleted. Staged changes are kept."
    }

    // Same window as the dialogs shown through navigation (AppTab), so it sits above the screen
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            scrimColor = MaterialTheme.colors.dialogOverlay,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        MaterialDialog(onCloseRequested = onDismiss) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(520.dp),
            ) {
                Icon(
                    painterResource(Res.drawable.delete),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .size(64.dp),
                    tint = MaterialTheme.colors.error,
                )

                Text(
                    text = title,
                    modifier = Modifier.padding(bottom = 8.dp),
                    color = MaterialTheme.colors.onBackground,
                    style = MaterialTheme.typography.body1,
                    fontWeight = FontWeight.SemiBold,
                )

                Text(
                    text = "$explanation Ignored files are kept.",
                    color = MaterialTheme.colors.onBackground,
                    style = MaterialTheme.typography.body2,
                    textAlign = TextAlign.Center,
                )

                Text(
                    text = "${entries.size} file${if (entries.size == 1) "" else "s"} affected. This can't be undone.",
                    modifier = Modifier.padding(top = 8.dp),
                    color = MaterialTheme.colors.error,
                    style = MaterialTheme.typography.body2,
                    fontWeight = FontWeight.SemiBold,
                )

                ScrollableLazyColumn(
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .heightIn(max = 260.dp)
                        .fillMaxWidth(),
                ) {
                    items(entries) { entry ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 2.dp),
                        ) {
                            Text(
                                text = when (entry.statusType) {
                                    StatusType.ADDED -> "Deleted"
                                    StatusType.REMOVED -> "Restored"
                                    StatusType.MODIFIED, StatusType.CONFLICTING -> "Reverted"
                                },
                                modifier = Modifier.width(72.dp),
                                color = if (entry.statusType == StatusType.ADDED) {
                                    MaterialTheme.colors.error
                                } else {
                                    MaterialTheme.colors.onBackgroundSecondary
                                },
                                style = MaterialTheme.typography.body2,
                            )

                            Text(
                                text = entry.filePath,
                                color = MaterialTheme.colors.onBackground,
                                style = MaterialTheme.typography.body2,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .padding(top = 24.dp)
                        .align(Alignment.End),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PrimaryButton(
                        text = stringResource(Res.string.generic_button_cancel),
                        onClick = onDismiss,
                        backgroundColor = Color.Transparent,
                        textColor = MaterialTheme.colors.onBackground,
                    )
                    PrimaryButton(
                        text = "Discard",
                        onClick = onDiscard,
                        backgroundColor = MaterialTheme.colors.error,
                        textColor = MaterialTheme.colors.onError,
                    )
                }
            }
        }
    }
}
