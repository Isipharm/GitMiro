@file:OptIn(ExperimentalAnimationApi::class, ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)

package com.hugodehu.gitmiro.ui.status

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isMetaPressed
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.hugodehu.gitmiro.LocalTabFocusRequester
import com.hugodehu.gitmiro.app.generated.resources.*
import com.hugodehu.gitmiro.common.systemSeparator
import com.hugodehu.gitmiro.compose.rememberInTab
import com.hugodehu.gitmiro.domain.extensions.fileName
import com.hugodehu.gitmiro.domain.extensions.parentDirectoryPath
import com.hugodehu.gitmiro.domain.models.*
import com.hugodehu.gitmiro.domain.repositories.CompletedTask
import com.hugodehu.gitmiro.extensions.handMouseClickable
import com.hugodehu.gitmiro.extensions.letter
import com.hugodehu.gitmiro.extensions.iconColor
import com.hugodehu.gitmiro.extensions.setClipboardText
import com.hugodehu.gitmiro.keybindings.KeybindingOption
import com.hugodehu.gitmiro.keybindings.matchesBinding
import com.hugodehu.gitmiro.theme.abortButton
import com.hugodehu.gitmiro.theme.textFieldColors
import com.hugodehu.gitmiro.ui.components.*
import com.hugodehu.gitmiro.ui.context_menu.ContextMenuElement
import com.hugodehu.gitmiro.theme.onBackgroundSecondary
import androidx.compose.foundation.border
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import com.hugodehu.gitmiro.ui.context_menu.statusDirEntriesContextMenuItems
import com.hugodehu.gitmiro.ui.context_menu.statusEntriesContextMenuItems
import com.hugodehu.gitmiro.ui.context_menu.statusEntryContextMenuItems
import com.hugodehu.gitmiro.ui.components.tooltip.InstantTooltip
import com.hugodehu.gitmiro.ui.dialogs.CommitAuthorDialog
import com.hugodehu.gitmiro.ui.dialogs.DiscardChangesDialog
import com.hugodehu.gitmiro.ui.tree_files.TreeItem
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun StatusPane(
    statusState: StatusState,
    onAction: (StatusAction) -> Unit,
    onBlameFile: (String) -> Unit,
    onHistoryFile: (String) -> Unit,
    completedTasks: StateFlow<List<CompletedTask>>,
    onStash: () -> Unit,
    onStashWithMessage: () -> Unit,
) {
    val swapUncommittedChanges = statusState.swapUncommittedChanges
    val stagedListState = rememberInTab("statusStagedListState", statusState.staged) {
        LazyListState()
    }
    val unstagedListState = rememberInTab("statusUnstagedListState", statusState.unstaged) {
        LazyListState()
    }

    val commitMessage = statusState.commitMessage

    val isAmend = statusState.isAmend
    val isAmendRebaseInteractive = statusState.isAmendRebaseInteractive
    val committerDataRequestState = statusState.committerDataRequestState
    val rebaseInteractiveState = statusState.rebaseInteractiveState
    val selectedUnstagedDiffEntries = statusState.selectedUnstagedDiffEntries
    val selectedStagedDiffEntries = statusState.selectedStagedDiffEntries

    val showAsTree = statusState.showAsTree
    val showSearchStaged = statusState.showSearchStaged
    val searchFilterStaged = statusState.searchFilterStaged
    val showSearchUnstaged = statusState.showSearchUnstaged
    val searchFilterUnstaged = statusState.searchFilterUnstaged

    val isAmenableRebaseInteractive =
        statusState.repositoryState.isRebasing && rebaseInteractiveState is RebaseInteractiveState.ProcessingCommits && rebaseInteractiveState.isCurrentStepAmenable

    val doCommit = {
        onAction(StatusAction.Commit(commitMessage.text))
        Unit
    }

    val canCommit = commitMessage.text.isNotEmpty() && statusState.hasStagedFiles
    val canAmend = commitMessage.text.isNotEmpty() && statusState.hasPreviousCommits
    val tabFocusRequester = LocalTabFocusRequester.current

    // Discarding is offered outside of merges, rebases and the like, which have their own abort
    val canDiscard = statusState.repositoryState == RepositoryState.SAFE
    var discardScope by remember { mutableStateOf<DiscardScope?>(null) }

    LaunchedEffect(statusState) {
        launch {
            if (statusState.showSearchUnstaged || statusState.showSearchStaged) {
                tabFocusRequester.requestFocus()
            }
        }
    }

    // Stashing only concerns the uncommitted changes, so it lives in the unstaged list's menu
    val stashItems = buildList {
        if (statusState.hasStagedFiles || statusState.hasUnstagedFiles) {
            add(
                ContextMenuElement.ContextTextEntry(
                    label = "Stash all changes",
                    icon = { painterResource(Res.drawable.stash) },
                    onClick = onStash,
                )
            )
            add(
                ContextMenuElement.ContextTextEntry(
                    label = "Stash with message…",
                    icon = { painterResource(Res.drawable.message) },
                    onClick = onStashWithMessage,
                )
            )
        }
    }

    if (committerDataRequestState is CommitterDataRequestState.WaitingInput) {
        CommitAuthorDialog(
            committerDataRequestState.authorInfo,
            onClose = { onAction(StatusAction.RejectCommitterData) },
            onAccept = { newAuthorInfo, persist ->
                onAction(StatusAction.AcceptCommitterData(newAuthorInfo, persist))
            },
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        AnimatedVisibility(
            visible = statusState.isLoading,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colors.primaryVariant)
        }

        Column(
            modifier = Modifier.weight(1f),
        ) {
            @Composable
            fun staged() {
                StatusChangesList(
                    entryType = EntryType.STAGED,
                    statusState,
                    showSearchStaged,
                    showAsTree = showAsTree,
                    searchFilter = searchFilterStaged,
                    listState = stagedListState,
                    selectedEntries = selectedStagedDiffEntries,
                    onSearchFilterToggled = { onAction(StatusAction.SearchFilterToggledStaged(it)) },
                    onSearchFocused = { onAction(StatusAction.AddStagedSearchToCloseableView) },
                    onDiscardAll = if (canDiscard && statusState.hasStagedFiles) {
                        { discardScope = DiscardScope.STAGED }
                    } else {
                        null
                    },
                    onBlameFile = onBlameFile,
                    onHistoryFile = onHistoryFile,
                    onAction = { onAction(it) },
                )
            }

            @Composable
            fun unstaged() {
                StatusChangesList(
                    entryType = EntryType.UNSTAGED,
                    statusState = statusState,
                    showSearch = showSearchUnstaged,
                    showAsTree = showAsTree,
                    searchFilter = searchFilterUnstaged,
                    listState = unstagedListState,
                    selectedEntries = selectedUnstagedDiffEntries,
                    onSearchFilterToggled = { onAction(StatusAction.SearchFilterToggledUnstaged(it)) },
                    onSearchFocused = { onAction(StatusAction.AddUnstagedSearchToCloseableView) },
                    extraItems = stashItems,
                    onDiscardAll = if (canDiscard && statusState.hasUnstagedFiles) {
                        { discardScope = DiscardScope.UNSTAGED }
                    } else {
                        null
                    },
                    onBlameFile = onBlameFile,
                    onHistoryFile = onHistoryFile,
                    onAction = { onAction(it) },
                )
            }

            if (swapUncommittedChanges) {
                unstaged()
                staged()
            } else {
                staged()
                unstaged()
            }
        }

        CommitField(
            canCommit,
            isAmend,
            canAmend,
            doCommit,
            commitMessage,
            previousCommitMessage = statusState.previousCommitMessage,
            statusState.repositoryState,
            isAmenableRebaseInteractive,
            statusState.hasUnstagedFiles,
            rebaseInteractiveState,
            statusState.hasStagedFiles,
            isAmendRebaseInteractive,
            !statusState.isLoading && statusState.haveConflictsBeenSolved,
            setCommitMessage = {
                onAction(StatusAction.UpdateCommitMessage(it))
            },
            onResetRepoState = {
                onAction(StatusAction.ResetRepositoryState)
                onAction(StatusAction.UpdateCommitMessage(TextFieldValue("")))
            },
            onAbortRebase = {
                onAction(StatusAction.AbortRebase)
                onAction(StatusAction.UpdateCommitMessage(TextFieldValue("")))
            },
            onAmendChecked = { amend ->
                if (amend && commitMessage.text.isEmpty()) {
                    onAction(StatusAction.UpdateCommitMessage(TextFieldValue(statusState.previousCommitMessage.orEmpty())))
                }
                onAction(StatusAction.ToggleAmend(amend))
            },
            onContinueRebase = { onAction(StatusAction.ContinueRebase(commitMessage.text)) },
            onSkipRebase = { onAction(StatusAction.SkipRebase) },
            onAmendRebaseInteractiveChecked = { amend ->
                if (amend && commitMessage.text.isEmpty()) {
                    onAction(StatusAction.UpdateCommitMessage(TextFieldValue(statusState.previousCommitMessage.orEmpty())))
                }

                onAction(StatusAction.ToggleAmendRebaseInteractive(amend))
            },
        )
    }

    discardScope?.let { scope ->
        DiscardChangesDialog(
            scope = scope,
            staged = statusState.getEntriesByEntryType(EntryType.STAGED),
            unstaged = statusState.getEntriesByEntryType(EntryType.UNSTAGED),
            onDiscard = {
                onAction(StatusAction.DiscardChanges(scope))
                discardScope = null
            },
            onDismiss = { discardScope = null },
        )
    }
}

@Composable
fun ColumnScope.StatusChangesList(
    entryType: EntryType,
    statusState: StatusState,
    showSearch: Boolean,
    showAsTree: Boolean,
    searchFilter: TextFieldValue,
    listState: LazyListState,
    selectedEntries: List<DiffType.UncommittedDiff>,
    onSearchFilterToggled: (Boolean) -> Unit,
    onSearchFocused: () -> Unit,
    onDiscardAll: (() -> Unit)?,
    onBlameFile: (String) -> Unit,
    onHistoryFile: (String) -> Unit,
    onAction: (StatusAction) -> Unit,
    extraItems: List<ContextMenuElement> = emptyList(),
) {
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboard.current

    val title = when (entryType) {
        EntryType.STAGED -> stringResource(Res.string.uncommited_changes_staged_title)
        EntryType.UNSTAGED -> stringResource(Res.string.uncommited_changes_unstaged_title)
    }

    val actionInfo = getActionInfo(entryType)
    val entries = if (searchFilter.text.trim().isEmpty()) {
        when (entryType) {
            EntryType.STAGED -> statusState.staged
            EntryType.UNSTAGED -> statusState.unstaged
        }
    } else {
        when (entryType) {
            EntryType.STAGED -> statusState.filteredStaged
            EntryType.UNSTAGED -> statusState.filteredUnstaged
        }
    }

    this.ChangesList(
        title = title,
        actionInfo = actionInfo,
        entryType = entryType,
        entries = entries,
        showSearch = showSearch,
        showAsTree = showAsTree,
        searchFilter = searchFilter,
        listState = listState,
        selectedEntries = selectedEntries,
        onSearchFilterToggled = onSearchFilterToggled,
        onSearchFocused = onSearchFocused,
        onDiscardAll = onDiscardAll,
        count = entries.count { it is TreeItem.File<*> },
        extraItems = extraItems,
        onBlameFile = onBlameFile,
        onHistoryFile = onHistoryFile,
        onAction = onAction,
        onCopy = { relative, entries ->
            scope.launch {
                copyEntriesPath(clipboard, entries, relative, statusState.repositoryPath)
            }
        }
    )
}

@Composable
fun ColumnScope.ChangesList(
    title: String,
    actionInfo: ActionInfo,
    entryType: EntryType,
    showSearch: Boolean,
    searchFilter: TextFieldValue,
    listState: LazyListState,
    selectedEntries: List<DiffType.UncommittedDiff>,
    showAsTree: Boolean,
    entries: List<TreeItem<StatusEntry>>,
    onSearchFilterToggled: (Boolean) -> Unit,
    onSearchFocused: () -> Unit,
    onDiscardAll: (() -> Unit)?,
    count: Int,
    extraItems: List<ContextMenuElement>,
    onBlameFile: (String) -> Unit,
    onHistoryFile: (String) -> Unit,
    onAction: (StatusAction) -> Unit,
    onCopy: (relative: Boolean, entries: List<StatusEntry>) -> Unit,
) {
    fun entriesContextMenu(): (StatusEntry) -> List<ContextMenuElement> = { statusEntry ->
        statusEntryContextMenuItems(
            statusEntry = statusEntry,
            entryType = entryType,
            onBlame = { onBlameFile(statusEntry.filePath) },
            onHistory = { onHistoryFile(statusEntry.filePath) },
            onReset = { onAction(StatusAction.Reset(statusEntry)) },
            onDelete = { onAction(StatusAction.Delete(statusEntry)) },
            onOpenFileInFolder = { onAction(StatusAction.OpenInFolder(statusEntry.filePath)) },
            onResolveConflict = { side -> onAction(StatusAction.ResolveConflict(statusEntry, side)) },
            onToggleStage = { onAction(StatusAction.EntryAction(statusEntry)) },
            onCopyFilePath = { relative ->
                onCopy(
                    relative,
                    listOf(statusEntry),
                )
            },
        )
    }

    fun selectedEntriesContextMenu(): (StatusEntry) -> List<ContextMenuElement> = {
        statusEntriesContextMenuItems(
            selectedEntriesCount = selectedEntries.count(),
            entryType = entryType,
            onDiscard = { onAction(StatusAction.DiscardSelected(entryType)) },
            onStageSelected = { onAction(StatusAction.SelectedEntriesAction(EntryType.UNSTAGED)) },
            onUnstageSelected = { onAction(StatusAction.SelectedEntriesAction(EntryType.STAGED)) },
            onCopyFilesPath = { relative ->
                onCopy(
                    relative,
                    selectedEntries.map { it.statusEntry },
                )
            },
        )
    }

    val showActionForSelected = remember(selectedEntries) { selectedEntries.count() > 1 }
    val keyboardModifiers = LocalWindowInfo.current.keyboardModifiers

    ChangesList(
        title = title,
        actionInfo = actionInfo,
        showSearch = showSearch,
        showAsTree = showAsTree,
        showActionForSelected = showActionForSelected,
        searchFilter = searchFilter,
        onSearchFilterToggled = onSearchFilterToggled,
        onSearchFocused = onSearchFocused,
        onSearchFilterChanged = { onAction(StatusAction.SearchFilterChanged(it, entryType)) },
        listState = listState,
        onAllAction = { onAction(StatusAction.AllEntriesAction(entryType)) },
        onDiscardAll = onDiscardAll,
        count = count,
        extraItems = extraItems,
        onAlternateShowAsTree = { onAction(StatusAction.ToggleShowAsTree) },
    ) {
        if (entries.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillParentMaxWidth()
                        .height(96.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (entryType == EntryType.STAGED) "Nothing staged" else "No changes",
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onBackgroundSecondary,
                    )
                }
            }
        }

        items(entries, key = { it.fullPath }) { treeEntry ->
            val isEntrySelected = treeEntry is TreeItem.File<StatusEntry> &&
                    selectedEntries.any { entry ->
                        entry.statusEntry == treeEntry.data && ((entry.isUnstagedDiff && entryType == EntryType.UNSTAGED) ||
                                (entry.isStagedDiff && entryType == EntryType.STAGED))
                    }

            UncommittedTreeItemEntry(
                treeEntry,
                isSelected = isEntrySelected,
                actionTitle = actionInfo.applyToOneTitle,
                actionColor = actionInfo.color,
                showAsTree = showAsTree,
                onClick = {
                    if (treeEntry is TreeItem.File<StatusEntry>) {
                        onAction(
                            StatusAction.SelectEntry(
                                statusEntry = treeEntry.data,
                                isCtrlPressed = keyboardModifiers.isCtrlPressed,
                                isMetaPressed = keyboardModifiers.isMetaPressed,
                                isShiftPressed = keyboardModifiers.isShiftPressed,
                                diffEntries = entries,
                                selectedEntries = selectedEntries,
                            )
                        )
                    } else if (treeEntry is TreeItem.Dir) {
                        onAction(
                            StatusAction.TreeDirectoryToggle(
                                treeEntry.fullPath
                            )
                        )
                    }
                },
                onButtonClick = {
                    if (treeEntry is TreeItem.File<StatusEntry>) {
                        onAction(StatusAction.EntryAction(treeEntry.data))
                    }
                },
                onGenerateContextMenu = if (isEntrySelected && selectedEntries.count() > 1)
                    selectedEntriesContextMenu()
                else
                    entriesContextMenu(),
                onGenerateDirectoryContextMenu = { dir ->
                    statusDirEntriesContextMenuItems(
                        entryType = entryType,
                        onStageChanges = { onAction(StatusAction.DirectoryAction(dir.fullPath, entryType)) },
                        onDiscardDirectoryChanges = {},
                        onOpenInFolder = { onAction(StatusAction.OpenInFolder(dir.fullPath)) },
                    )
                },
            )
        }
    }
}

@Composable
fun ColumnScope.ChangesList(
    title: String,
    actionInfo: ActionInfo,
    showSearch: Boolean,
    showAsTree: Boolean,
    showActionForSelected: Boolean,
    searchFilter: TextFieldValue,
    listState: LazyListState,
    onSearchFilterToggled: (Boolean) -> Unit,
    onSearchFocused: () -> Unit,
    onSearchFilterChanged: (TextFieldValue) -> Unit,
    onAllAction: () -> Unit,
    onDiscardAll: (() -> Unit)?,
    count: Int,
    extraItems: List<ContextMenuElement>,
    onAlternateShowAsTree: () -> Unit,
    content: LazyListScope.() -> Unit,
) {
    val modifier = Modifier
        .weight(5f)
        .fillMaxWidth()
    Column(
        modifier = modifier
    ) {
        FilesChangedHeader(
            title = title,
            actionInfo = actionInfo,
            onAllAction = onAllAction,
            onDiscardAll = onDiscardAll,
            count = count,
            extraItems = extraItems,
            onAlternateShowAsTree = onAlternateShowAsTree,
            searchFilter = searchFilter,
            onSearchFilterChanged = onSearchFilterChanged,
            onSearchFilterToggled = onSearchFilterToggled,
            onSearchFocused = onSearchFocused,
            showAsTree = showAsTree,
            showSearch = showSearch,
            showActionForSelected = showActionForSelected,
        )

        ScrollableLazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            state = listState,
        ) {
            this.content()
        }

        PanelDivider()
    }
}


@Composable
private fun CommitMessageField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    enabled: Boolean,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = MaterialTheme.shapes.small
    val borderColor = if (isFocused) MaterialTheme.colors.primary else MaterialTheme.colors.onBackground.copy(alpha = 0.12f)

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        interactionSource = interactionSource,
        textStyle = MaterialTheme.typography.body1.copy(color = MaterialTheme.colors.onBackground),
        cursorBrush = SolidColor(MaterialTheme.colors.primary),
        modifier = modifier
            .padding(bottom = 8.dp)
            .background(MaterialTheme.colors.surface, shape)
            .border(1.dp, borderColor, shape),
        decorationBox = { innerTextField ->
            Box(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 10.dp)) {
                if (value.text.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onBackgroundSecondary,
                    )
                }

                innerTextField()
            }
        },
    )
}

@Composable
private fun CommitField(
    canCommit: Boolean,
    isAmend: Boolean,
    canAmend: Boolean,
    doCommit: () -> Unit,
    commitMessage: TextFieldValue,
    previousCommitMessage: String?,
    repositoryState: RepositoryState,
    isAmenableRebaseInteractive: Boolean,
    hasUnstagedFiles: Boolean,
    rebaseInteractiveState: RebaseInteractiveState,
    hasStagedFiles: Boolean,
    isAmendRebaseInteractive: Boolean,
    haveConflictsBeenSolved: Boolean,
    setCommitMessage: (TextFieldValue) -> Unit,
    onResetRepoState: () -> Unit,
    onAbortRebase: () -> Unit,
    onContinueRebase: () -> Unit,
    onSkipRebase: () -> Unit,
    onAmendChecked: (Boolean) -> Unit,
    onAmendRebaseInteractiveChecked: (Boolean) -> Unit,
) {
    val isReadOnlyRebase = repositoryState.isRebasing && !isAmenableRebaseInteractive
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp)
    ) {
        CommitMessageField(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .onPreviewKeyEvent { keyEvent ->
                    if (keyEvent.matchesBinding(KeybindingOption.TEXT_ACCEPT) && (canCommit || isAmend && canAmend)) {
                        doCommit()
                        true
                    } else
                        false
                },
            value = if (isReadOnlyRebase) TextFieldValue(previousCommitMessage.orEmpty()) else commitMessage,
            onValueChange = setCommitMessage,
            enabled = !repositoryState.isRebasing || isAmenableRebaseInteractive,
            placeholder = if (isReadOnlyRebase) {
                stringResource(Res.string.uncommited_changes_text_input_label_message_read_only)
            } else {
                stringResource(Res.string.uncommited_changes_text_input_label_message)
            },
        )

        when {
            repositoryState.isMerging -> MergeButtons(
                haveConflictsBeenSolved = !hasUnstagedFiles,
                onAbort = onResetRepoState,
                onMerge = { doCommit() }
            )

            repositoryState.isRebasing -> {
                val isCurrentStepAmenable =
                    (rebaseInteractiveState as? RebaseInteractiveState.ProcessingCommits)?.isCurrentStepAmenable == true
                RebasingButtons(
                    canContinue = hasStagedFiles || hasUnstagedFiles || (isAmenableRebaseInteractive && isAmendRebaseInteractive && commitMessage.text.isNotEmpty()),
                    haveConflictsBeenSolved = !hasUnstagedFiles,
                    onAbort = onAbortRebase,
                    onContinue = onContinueRebase,
                    onSkip = onSkipRebase,
                    isAmendable = isCurrentStepAmenable,
                    isAmend = isAmendRebaseInteractive,
                    onAmendChecked = onAmendRebaseInteractiveChecked,
                )
            }

            repositoryState.isCherryPicking -> CherryPickingButtons(
                haveConflictsBeenSolved = !hasUnstagedFiles,
                onAbort = onResetRepoState,
                onCommit = {
                    doCommit()
                }
            )

            repositoryState.isReverting -> RevertingButtons(
                haveConflictsBeenSolved = haveConflictsBeenSolved,
                canCommit = commitMessage.text.isNotBlank(),
                onAbort = onResetRepoState,
                onCommit = {
                    doCommit()
                }
            )

            else -> UncommittedChangesButtons(
                canCommit = canCommit,
                canAmend = canAmend,
                isAmend = isAmend,
                onAmendChecked = onAmendChecked,
                onCommit = doCommit,
            )
        }
    }
}

@Composable
fun UncommittedChangesButtons(
    canCommit: Boolean,
    canAmend: Boolean,
    isAmend: Boolean,
    onAmendChecked: (Boolean) -> Unit,
    onCommit: () -> Unit,
) {
    val buttonText = if (isAmend)
        stringResource(Res.string.uncommited_changes_primary_button_amend)
    else
        stringResource(Res.string.uncommited_changes_primary_button_commit)

    Column {
        CheckboxText(
            value = isAmend,
            onCheckedChange = { onAmendChecked(!isAmend) },
            text = stringResource(Res.string.uncommited_changes_amend_check)
        )
        Row(
            modifier = Modifier
                .padding(top = 2.dp)
        ) {
            ConfirmationButton(
                text = buttonText,
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp),
                onClick = {
                    onCommit()
                },
                enabled = canCommit || (canAmend && isAmend),
                shape = MaterialTheme.shapes.small
            )
        }
    }
}

@Composable
fun MergeButtons(
    haveConflictsBeenSolved: Boolean,
    onAbort: () -> Unit,
    onMerge: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .height(36.dp)
    ) {
        AbortButton(
            modifier = Modifier
                .weight(1f)
                .padding(end = 4.dp)
                .fillMaxHeight(),
            onClick = onAbort
        )

        ConfirmationButton(
            text = stringResource(Res.string.uncommited_changes_primary_button_merge),
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp)
                .fillMaxHeight(),
            enabled = haveConflictsBeenSolved,
            onClick = onMerge,
        )
    }
}

@Composable
fun CherryPickingButtons(
    haveConflictsBeenSolved: Boolean,
    onAbort: () -> Unit,
    onCommit: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .height(36.dp)
    ) {
        AbortButton(
            modifier = Modifier
                .weight(1f)
                .padding(end = 4.dp)
                .fillMaxHeight(),
            onClick = onAbort
        )

        ConfirmationButton(
            text = stringResource(Res.string.uncommited_changes_primary_button_commit),
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp)
                .fillMaxHeight(),
            enabled = haveConflictsBeenSolved,
            onClick = onCommit,
        )
    }
}

@Composable
fun RebasingButtons(
    canContinue: Boolean,
    isAmendable: Boolean,
    isAmend: Boolean,
    onAmendChecked: (Boolean) -> Unit,
    haveConflictsBeenSolved: Boolean,
    onAbort: () -> Unit,
    onContinue: () -> Unit,
    onSkip: () -> Unit,
) {
    Column {
        if (isAmendable) {
            CheckboxText(
                value = isAmend,
                onCheckedChange = { onAmendChecked(!isAmend) },
                text = stringResource(Res.string.uncommited_changes_amend_check)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .height(36.dp)
        ) {
            AbortButton(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 4.dp)
                    .fillMaxHeight(),
                onClick = onAbort
            )

            if (canContinue) {
                ConfirmationButton(
                    text = stringResource(Res.string.uncommited_changes_primary_button_continue),
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp)
                        .fillMaxHeight(),
                    enabled = haveConflictsBeenSolved,
                    onClick = onContinue,
                )
            } else {
                ConfirmationButton(
                    text = stringResource(Res.string.uncommited_changes_primary_button_skip),
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp)
                        .fillMaxHeight(),
                    onClick = onSkip,
                )
            }
        }
    }
}

@Composable
fun RevertingButtons(
    canCommit: Boolean,
    haveConflictsBeenSolved: Boolean,
    onAbort: () -> Unit,
    onCommit: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .height(36.dp)
    ) {
        AbortButton(
            modifier = Modifier
                .weight(1f)
                .padding(end = 4.dp),
            onClick = onAbort
        )

        ConfirmationButton(
            text = stringResource(Res.string.uncommited_changes_primary_button_continue),
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp)
                .fillMaxHeight(),
            enabled = canCommit && haveConflictsBeenSolved,
            onClick = onCommit,
        )
    }
}

@Composable
fun AbortButton(modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .handMouseClickable { onClick() }
            .focusable(false)
            .clip(MaterialTheme.shapes.small)
            .border(1.dp, MaterialTheme.colors.error.copy(alpha = 0.5f), MaterialTheme.shapes.small),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(Res.string.uncommited_changes_secondary_button_abort),
            style = MaterialTheme.typography.body1.copy(color = MaterialTheme.colors.error),
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
fun ConfirmationButton(
    text: String,
    modifier: Modifier,
    enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.small,
    onClick: () -> Unit,
) {
    val (backgroundColor, contentColor) = if (enabled) {
        (MaterialTheme.colors.primary to MaterialTheme.colors.onPrimary)
    } else {
        (MaterialTheme.colors.onSurface.copy(alpha = 0.12f)
            .compositeOver(MaterialTheme.colors.surface) to MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.disabled))
    }

    Box(
        modifier = modifier
            .handMouseClickable { if (enabled) onClick() }
            .focusable(false) // TODO this and the abort button should be focusable (show some kind of border when focused?)
            .clip(shape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.body1.copy(color = contentColor),
        )
    }
}

@Composable
private fun UncommittedFileEntry(
    statusEntry: StatusEntry,
    isSelected: Boolean,
    showDirectory: Boolean,
    actionTitle: String,
    actionColor: Color,
    onClick: () -> Unit,
    onButtonClick: () -> Unit,
    depth: Int = 0,
    onGenerateContextMenu: (StatusEntry) -> List<ContextMenuElement>,
) {
    FileEntry(
        statusLetter = statusEntry.letter,
        depth = depth,
        statusColor = statusEntry.iconColor,
        parentDirectoryPath = if (showDirectory) statusEntry.parentDirectoryPath else "",
        fileName = statusEntry.fileName,
        isSelected = isSelected,
        onClick = onClick,
        onDoubleClick = onButtonClick,
        onGenerateContextMenu = { onGenerateContextMenu(statusEntry) },
        // Staging is done with a double click or from the context menu, there is no button over the row
        trailingAction = null,
    )
}

@Composable
private fun TreeFileEntry(
    fileEntry: TreeItem.File<StatusEntry>,
    isSelected: Boolean,
    actionTitle: String,
    actionColor: Color,
    showAsTree: Boolean,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit,
    onGenerateContextMenu: (StatusEntry) -> List<ContextMenuElement>,
) {
    UncommittedFileEntry(
        statusEntry = fileEntry.data,
        isSelected = isSelected,
        showDirectory = !showAsTree,
        actionTitle = actionTitle,
        actionColor = actionColor,
        onClick = onClick,
        onButtonClick = onDoubleClick,
        depth = fileEntry.depth,
        onGenerateContextMenu = onGenerateContextMenu,
    )
}

@Composable
private fun UncommittedTreeItemEntry(
    entry: TreeItem<StatusEntry>,
    isSelected: Boolean,
    actionTitle: String,
    actionColor: Color,
    showAsTree: Boolean,
    onClick: () -> Unit,
    onButtonClick: () -> Unit,
    onGenerateContextMenu: (StatusEntry) -> List<ContextMenuElement>,
    onGenerateDirectoryContextMenu: (TreeItem.Dir) -> List<ContextMenuElement>,
) {
    when (entry) {
        is TreeItem.File -> TreeFileEntry(
            entry,
            isSelected,
            actionTitle,
            actionColor,
            showAsTree,
            onClick,
            onButtonClick,
            onGenerateContextMenu,
        )

        is TreeItem.Dir -> DirectoryEntry(
            dirName = entry.displayName,
            isExpanded = entry.isExpanded,
            onClick = onClick,
            depth = entry.depth,
            onGenerateContextMenu = { onGenerateDirectoryContextMenu(entry) },
        )
    }
}

@Composable
fun getActionInfo(entryType: EntryType): ActionInfo {
    val applyToOneTitle: String
    val applyToAllTitle: String
    val applyToSelectedTitle: String
    val icon: DrawableResource
    val color: Color
    val textColor: Color

    if (entryType == EntryType.STAGED) {
        applyToOneTitle = stringResource(Res.string.uncommited_changes_staged_item_action)
        applyToAllTitle = stringResource(Res.string.uncommited_changes_staged_all_items_action)
        applyToSelectedTitle = stringResource(Res.string.uncommited_changes_staged_selected_items_action)
        icon = Res.drawable.remove_done
        color = MaterialTheme.colors.error
        textColor = MaterialTheme.colors.onError
    } else {
        applyToOneTitle = stringResource(Res.string.uncommited_changes_unstaged_item_action)
        applyToAllTitle = stringResource(Res.string.uncommited_changes_unstaged_all_items_action)
        applyToSelectedTitle = stringResource(Res.string.uncommited_changes_unstaged_selected_items_action)
        icon = Res.drawable.done
        color = MaterialTheme.colors.primary
        textColor = MaterialTheme.colors.onPrimary
    }

    return ActionInfo(
        applyToOneTitle = applyToOneTitle,
        applyToAllTitle = applyToAllTitle,
        applyToSelectedTitle = applyToSelectedTitle,
        icon = icon,
        color = color,
        textColor = textColor,
    )
}


private suspend fun copyEntriesPath(
    clipboard: Clipboard,
    entries: List<StatusEntry>,
    relative: Boolean,
    repositoryPath: String?,
) {
    val pathsToCopy = entries.joinToString("\n") { entry ->
        if (relative) {
            entry.filePath
        } else {
            repositoryPath.orEmpty() + systemSeparator + entry.filePath
        }
    }

    clipboard.setClipboardText(pathsToCopy)
}