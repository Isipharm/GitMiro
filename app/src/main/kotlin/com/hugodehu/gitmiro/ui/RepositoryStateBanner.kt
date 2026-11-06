package com.hugodehu.gitmiro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hugodehu.gitmiro.app.generated.resources.Res
import com.hugodehu.gitmiro.app.generated.resources.warning
import com.hugodehu.gitmiro.domain.models.RebaseProgress
import com.hugodehu.gitmiro.domain.models.RepositoryState
import com.hugodehu.gitmiro.domain.models.StatusType
import com.hugodehu.gitmiro.theme.graphColors
import com.hugodehu.gitmiro.ui.components.GhostButton
import com.hugodehu.gitmiro.ui.components.PanelDivider
import com.hugodehu.gitmiro.ui.components.PrimaryButton
import com.hugodehu.gitmiro.ui.status.StatusAction
import com.hugodehu.gitmiro.ui.status.StatusState
import com.hugodehu.gitmiro.ui.tree_files.TreeItem
import org.jetbrains.compose.resources.painterResource

/**
 * Amber banner shown while a merge, rebase, cherry-pick or revert is in progress, with the actions to leave it.
 */
@Composable
fun RepositoryStateBanner(
    repositoryState: RepositoryState,
    statusState: StatusState,
    rebaseProgress: RebaseProgress?,
    currentBranch: String?,
    onAction: (StatusAction) -> Unit,
) {
    val operation = when {
        repositoryState.isMerging -> "merge"
        repositoryState.isRebasing -> "rebase"
        repositoryState.isCherryPicking -> "cherry-pick"
        repositoryState.isReverting -> "revert"
        else -> return
    }

    val conflictsCount = statusState.unstaged.count { it is TreeItem.File && it.data.statusType == StatusType.CONFLICTING }
    val message = statusState.commitMessage.text
    val isRebasing = repositoryState.isRebasing

    val title = when {
        isRebasing && rebaseProgress != null -> "Rebasing ${rebaseProgress.branch} onto ${rebaseProgress.onto}"
        repositoryState.isMerging -> {
            val merged = Regex("Merge (?:remote-tracking )?branch '([^']+)'").find(message)?.groupValues?.get(1)

            if (merged != null && currentBranch != null) "Merging $merged into $currentBranch" else "Merge in progress"
        }
        else -> "${operation.replaceFirstChar { it.uppercase() }} in progress"
    }

    val position = rebaseProgress?.currentPosition
    val subtitle = when {
        conflictsCount > 0 -> {
            val files = if (conflictsCount == 1) "1 file still has conflicts. Resolve it" else "$conflictsCount files still have conflicts. Resolve them"
            val prefix = if (isRebasing && position != null) "Commit $position of ${rebaseProgress.steps.size} stopped on a conflict. " else ""

            "$prefix$files, then continue the $operation."
        }

        // JGit commits only the index on continue, and changes left in the working tree stop the next commit
        isRebasing && statusState.hasUnstagedFiles -> "Some changes aren't staged. Stage or discard them, then continue the rebase."
        isRebasing && !statusState.hasStagedFiles -> "Nothing left to commit for this commit. Skip it to go on with the rebase."
        else -> "All conflicts are resolved. Continue the $operation."
    }

    val canContinue = conflictsCount == 0 && !statusState.isLoading && when {
        isRebasing -> statusState.hasStagedFiles && !statusState.hasUnstagedFiles
        repositoryState.isReverting -> message.isNotBlank()
        else -> true
    }

    val amber = MaterialTheme.colors.graphColors[3]

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(amber.copy(alpha = 0.10f))
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                painter = painterResource(Res.drawable.warning),
                contentDescription = null,
                tint = amber,
                modifier = Modifier.size(20.dp),
            )

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.body1,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colors.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.body2,
                    color = amber.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            GhostButton(
                text = "Abort $operation",
                color = MaterialTheme.colors.error,
                modifier = Modifier.height(34.dp),
                onClick = {
                    onAction(if (isRebasing) StatusAction.AbortRebase else StatusAction.ResetRepositoryState)
                    onAction(StatusAction.UpdateCommitMessage(androidx.compose.ui.text.input.TextFieldValue("")))
                },
            )

            if (isRebasing) {
                GhostButton(
                    text = "Skip this commit",
                    modifier = Modifier.height(34.dp),
                    onClick = {
                        onAction(StatusAction.SkipRebase)
                        onAction(StatusAction.UpdateCommitMessage(androidx.compose.ui.text.input.TextFieldValue("")))
                    },
                )
            }

            PrimaryButton(
                text = if (isRebasing) "Continue" else "Continue $operation",
                enabled = canContinue,
                modifier = Modifier.height(34.dp),
                onClick = {
                    onAction(if (isRebasing) StatusAction.ContinueRebase(message) else StatusAction.Commit(message))
                    // The message shown while stopped is the one of the commit being replayed, not a draft to keep
                    if (isRebasing) onAction(StatusAction.UpdateCommitMessage(androidx.compose.ui.text.input.TextFieldValue("")))
                },
            )
        }

        Box(Modifier.fillMaxWidth().height(1.dp).background(amber.copy(alpha = 0.35f)))
    }
}
