package com.hugodehu.gitmiro.ui.dialogs.errors

import com.hugodehu.gitmiro.theme.monoTypography
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hugodehu.gitmiro.extensions.handOnHover
import com.hugodehu.gitmiro.extensions.onDoubleClick
import com.hugodehu.gitmiro.app.generated.resources.Res
import com.hugodehu.gitmiro.app.generated.resources.copy
import com.hugodehu.gitmiro.app.generated.resources.error
import com.hugodehu.gitmiro.app.generated.resources.error_dialog_copy_button_tooltip
import com.hugodehu.gitmiro.app.generated.resources.generic_button_ok
import com.hugodehu.gitmiro.domain.errors.GenericError
import com.hugodehu.gitmiro.domain.models.TaskType
import com.hugodehu.gitmiro.domain.repositories.CompletedTask
import com.hugodehu.gitmiro.theme.secondarySurface
import com.hugodehu.gitmiro.ui.components.GhostButton
import com.hugodehu.gitmiro.ui.components.PrimaryButton
import com.hugodehu.gitmiro.ui.dialogs.base.DialogHeader
import com.hugodehu.gitmiro.ui.components.tooltip.InstantTooltip
import com.hugodehu.gitmiro.ui.dialogs.base.MaterialDialog
import com.hugodehu.gitmiro.ui.getErrorText
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun ErrorDialog(
    error: CompletedTask.Failure,
    onAccept: () -> Unit,
) {
    val horizontalScroll = rememberScrollState()
    val verticalScroll = rememberScrollState()
    val clipboard = LocalClipboardManager.current
    val errorStackTrace = remember(error) {
        (error.reason as? GenericError)
            ?.exception
            ?.stackTraceToString()
    }
    var showStackTrace by remember { mutableStateOf(false) }

    MaterialDialog(
        onCloseRequested = onAccept,
    ) {
        Column(
            modifier = Modifier
                .width(580.dp)
        ) {
            DialogHeader(
                icon = painterResource(Res.drawable.error),
                title = error.taskType.errorTitle(),
                subtitle = error.reason.getErrorText(),
                tint = MaterialTheme.colors.error,
            )

            if (showStackTrace && errorStackTrace != null) {
                Box(
                    modifier = Modifier
                        .padding(top = 24.dp)
                        .height(400.dp)
                        .fillMaxWidth()
                ) {
                    SelectionContainer {
                        Text(
                            text = errorStackTrace,
                            fontFamily = monoTypography(),
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onBackground,
                            softWrap = false,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(MaterialTheme.shapes.small)
                                .background(MaterialTheme.colors.background)
                                .border(1.dp, MaterialTheme.colors.onBackground.copy(alpha = 0.12f), MaterialTheme.shapes.small)
                                .horizontalScroll(horizontalScroll)
                                .verticalScroll(verticalScroll)
                                .padding(12.dp),
                        )
                    }

                    HorizontalScrollbar(
                        rememberScrollbarAdapter(horizontalScroll),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                    )

                    VerticalScrollbar(
                        rememberScrollbarAdapter(verticalScroll),
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                    )

                    InstantTooltip(
                        text = stringResource(Res.string.error_dialog_copy_button_tooltip),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = 16.dp)
                    ) {
                        IconButton(
                            onClick = {
                                copyMessageError(clipboard, Exception(error.reason.toString()))
                            },
                            modifier = Modifier
                                .size(24.dp)
                                .handOnHover()
                                .background(MaterialTheme.colors.background.copy(alpha = 0.8f))
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.copy),
                                contentDescription = null,
                                tint = MaterialTheme.colors.onSurface,
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (errorStackTrace != null) {
                    GhostButton(
                        text = if (showStackTrace) "Hide details" else "Show details",
                        onClick = { showStackTrace = !showStackTrace },
                        modifier = Modifier.height(36.dp),
                    )
                }

                PrimaryButton(
                    text = stringResource(Res.string.generic_button_ok),
                    onClick = onAccept
                )
            }
        }
    }
}

fun copyMessageError(clipboard: ClipboardManager, ex: Exception) {
    clipboard.setText(AnnotatedString(ex.stackTraceToString()))
}

fun TaskType.errorTitle(): String {
    return when (this) {
        TaskType.Unspecified -> "Error"
        TaskType.StageAllFiles -> "Staging all the files failed"
        TaskType.UnstageAllFiles -> "Unstaging all the files failed"
        TaskType.StageFile -> "File stage failed"
        TaskType.UnstageFile -> "File unstage failed"
        TaskType.StageHunk -> "File stage failed"
        TaskType.UnstageHunk -> "Hunk unstage failed"
        TaskType.StageLine -> "File line stage failed"
        TaskType.UnstageLine -> "File line unstage failed"
        TaskType.DiscardFile -> "Discard file failed"
        TaskType.DiscardChanges -> "Discard changes failed"
        TaskType.DeleteFile -> "Delete file failed"
        TaskType.ResolveConflict -> "Resolve conflict failed"
        TaskType.BlameFile -> "File blaming failed"
        TaskType.HistoryFile -> "Could not load file history"
        TaskType.DoCommit -> "Commit failed"
        TaskType.RevertCommit -> "Commit revert failed"
        TaskType.CherryPickCommit -> "Commit cherry-pick failed"
        TaskType.CheckoutCommit -> "Checkout commit failed"
        TaskType.ResetToCommit -> "Reset to commit failed"
        TaskType.CheckoutBranch -> "Branch checkout failed"
        TaskType.CheckoutRemoteBranch -> "Remote branch checkout failed"
        TaskType.CreateBranch -> "Could not create the new branch"
        TaskType.DeleteBranch -> "Could not delete the branch"
        TaskType.RenameBranch -> "Could not rename the branch"
        TaskType.MergeBranch -> "Merge failed"
        TaskType.RebaseBranch -> "Rebase failed"
        TaskType.RebaseInteractive -> "Rebase interactive failed"
        TaskType.ContinueRebase -> "Could not continue rebase"
        TaskType.AbortRebase -> "Could not abort rebase"
        TaskType.SkipRebase -> "Could not skip rebase step"
        TaskType.ChangeBranchUpstream -> "Upstream branch change failed"
        TaskType.PullFromBranch -> "Pull from branch failed"
        TaskType.PushToBranch -> "Push to branch failed"
        TaskType.DeleteRemoteBranch -> "Deleting remote branch failed"
        TaskType.Pull -> "Pull failed"
        TaskType.Push -> "Push failed"
        TaskType.Fetch -> "Fetch failed"
        TaskType.Stash -> "Stash failed"
        TaskType.ApplyStash -> "Apply stash failed"
        TaskType.PopStash -> "Pop stash failed"
        TaskType.DeleteStash -> "Delete stash failed"
        TaskType.CreateTag -> "Create tag failed"
        TaskType.CheckoutTag -> "Could not checkout tag's commit"
        TaskType.DeleteTag -> "Could not delete tag"
        TaskType.AddSubmodule -> "Add submodule failed"
        TaskType.DeleteSubmodule -> "Delete submodule failed"
        TaskType.InitSubmodule -> "Init submodule failed"
        TaskType.DeinitSubmodule -> "Deinit submodule failed"
        TaskType.SyncSubmodule -> "Sync submodule failed"
        TaskType.UpdateSubmodule -> "Update submodule failed"
        TaskType.SaveCustomTheme -> "Failed trying to save the custom theme"
        TaskType.ResetRepoState -> "Could not reset repository state"
        TaskType.ChangesDetection -> "Repository changes detection has stopped working"
        TaskType.RepositoryOpen -> "Could not open the repository"
        TaskType.RepositoryClone -> "Could not clone the repository"
        TaskType.AddRemote -> "Adding remote failed"
        TaskType.DeleteRemote -> "Deleting remote failed"
        TaskType.LoadAuthor -> "Loading author failed"
        TaskType.StageDir -> "Staging directory failed"
        TaskType.UnstageDir -> "Unstaging directory failed"
        TaskType.SaveAuthor -> "Saving author failed"
        TaskType.GetCommitForRebase -> "Get commit for rebase failed"
        TaskType.GetFileCommits -> "Get file commits failed"
        TaskType.GetLinesForRebaseInteractive -> "Get lines for rebase interactive failed"
        TaskType.GetCommitDiffEntries -> "Getting commit entries failed"
        TaskType.RefreshBranches -> "Refresh branches failed"
        TaskType.RefreshLog -> "Refresh log failed"
        TaskType.RefreshRemotes -> "Refresh remotes failed"
        TaskType.RefreshRepositoryState -> "Refresh repository state failed"
        TaskType.RefreshStashes -> "Refresh stashes failed"
        TaskType.RefreshStatus -> "Refresh status failed"
        TaskType.RefreshSubmodules -> "Refresh submodules failed"
        TaskType.RefreshTags -> "Refresh tags failed"
        TaskType.GetWorktree -> "Get worktree failed"
        TaskType.UpdateRemote -> "Update remote failed"
    }
}