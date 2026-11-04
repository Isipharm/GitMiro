package com.hugodehu.gitmiro.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hugodehu.gitmiro.app.generated.resources.Res
import com.hugodehu.gitmiro.app.generated.resources.generic_button_cancel
import com.hugodehu.gitmiro.app.generated.resources.warning
import com.hugodehu.gitmiro.domain.models.ConflictSide
import com.hugodehu.gitmiro.domain.models.RepositoryState
import com.hugodehu.gitmiro.theme.graphColors
import com.hugodehu.gitmiro.ui.components.GhostButton
import com.hugodehu.gitmiro.ui.components.PrimaryButton
import com.hugodehu.gitmiro.ui.dialogs.base.DialogHeader
import com.hugodehu.gitmiro.ui.dialogs.base.MaterialDialog
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Shown for delete/modify conflicts, where one side deleted the file and the other side modified it.
 */
@Composable
fun DeletedFileConflictDialog(
    filePath: String,
    deletedBy: ConflictSide,
    repositoryState: RepositoryState,
    onResolve: (ConflictSide) -> Unit,
    onDismiss: () -> Unit,
) {
    // During a rebase, HEAD is the branch being rebased onto and the incoming changes are the commit being applied
    val currentLabel = if (repositoryState.isRebasing) "the branch you are rebasing onto" else "the current branch (HEAD)"
    val incomingLabel = when {
        repositoryState.isRebasing -> "the commit being applied"
        repositoryState.isMerging -> "the branch being merged"
        else -> "the incoming changes"
    }

    val (deletedIn, modifiedIn) = when (deletedBy) {
        ConflictSide.CURRENT -> currentLabel to incomingLabel
        ConflictSide.INCOMING -> incomingLabel to currentLabel
    }
    val modifiedSide = when (deletedBy) {
        ConflictSide.CURRENT -> ConflictSide.INCOMING
        ConflictSide.INCOMING -> ConflictSide.CURRENT
    }

    MaterialDialog(onCloseRequested = onDismiss) {
        Column(modifier = Modifier.width(480.dp)) {
            DialogHeader(
                icon = painterResource(Res.drawable.warning),
                title = "File deleted on one side",
                subtitle = "\"$filePath\" was deleted in $deletedIn, but modified in $modifiedIn.",
                tint = MaterialTheme.colors.graphColors[3],
            )

            Row(
                modifier = Modifier
                    .padding(top = 24.dp)
                    .align(Alignment.End),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                GhostButton(
                    text = stringResource(Res.string.generic_button_cancel),
                    onClick = onDismiss,
                    modifier = Modifier.height(36.dp),
                )
                GhostButton(
                    text = "Delete file",
                    onClick = { onResolve(deletedBy) },
                    color = MaterialTheme.colors.error,
                    modifier = Modifier.height(36.dp),
                )
                PrimaryButton(
                    text = "Keep modified file",
                    onClick = { onResolve(modifiedSide) },
                )
            }
        }
    }
}
