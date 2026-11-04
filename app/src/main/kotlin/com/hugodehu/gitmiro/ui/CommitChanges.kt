package com.hugodehu.gitmiro.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.hugodehu.gitmiro.LocalTabFocusRequester
import com.hugodehu.gitmiro.compose.rememberInTab
import com.hugodehu.gitmiro.domain.extensions.fileName
import com.hugodehu.gitmiro.domain.extensions.filePath
import com.hugodehu.gitmiro.domain.extensions.parentDirectoryPath
import com.hugodehu.gitmiro.domain.models.Commit
import com.hugodehu.gitmiro.domain.models.CommitStats
import com.hugodehu.gitmiro.repositoryopen.LogAction
import com.hugodehu.gitmiro.theme.monoTypography
import com.hugodehu.gitmiro.ui.context_menu.DropDownMenu
import com.hugodehu.gitmiro.app.generated.resources.Res
import com.hugodehu.gitmiro.app.generated.resources.more_vert
import org.jetbrains.compose.resources.painterResource
import androidx.compose.foundation.border
import androidx.compose.material.Icon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.hugodehu.gitmiro.domain.models.DiffSelected
import com.hugodehu.gitmiro.domain.models.Identity
import com.hugodehu.gitmiro.extensions.handMouseClickable
import com.hugodehu.gitmiro.extensions.letter
import com.hugodehu.gitmiro.extensions.iconColor
import com.hugodehu.gitmiro.extensions.toSmartSystemString
import com.hugodehu.gitmiro.repositoryopen.CommitChangesAction
import com.hugodehu.gitmiro.repositoryopen.CommitChangesState
import com.hugodehu.gitmiro.repositoryopen.RepositoryOpenViewModel
import com.hugodehu.gitmiro.theme.onBackgroundSecondary
import com.hugodehu.gitmiro.theme.tertiarySurface
import com.hugodehu.gitmiro.ui.components.*
import com.hugodehu.gitmiro.ui.context_menu.ContextMenuElement
import com.hugodehu.gitmiro.ui.context_menu.committedChangesEntriesContextMenuItems
import com.hugodehu.gitmiro.ui.tree_files.TreeItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import org.eclipse.jgit.diff.DiffEntry

@Composable
fun CommitChanges(
    viewModel: RepositoryOpenViewModel,
    commitChangesState: CommitChangesState,
    onBlame: (String) -> Unit,
    onHistory: (String) -> Unit,
) {
    val diffSelected by viewModel
        .diffSelected
        .filterIsInstance<DiffSelected.CommitedChanges>()
        .collectAsState(null)

    val stats by viewModel.commitStats.collectAsState()
    val logState by viewModel.logState.collectAsState()
    val commit = commitChangesState.commit
    val branchName = logState.branches[commit.hash].orEmpty().firstOrNull { it.isLocal }?.simpleName
        ?: logState.currentBranch?.simpleName

    CommitChangesView(
        stats = stats,
        branchName = branchName,
        isStash = logState.stashes.contains(commit.hash),
        onCheckout = { viewModel.onAction(LogAction.CheckoutCommit(commit)) },
        onCherryPick = { viewModel.onAction(LogAction.CherryPickCommit(commit)) },
        onRevert = { viewModel.onAction(LogAction.RevertCommit(commit)) },
        onApplyStash = { viewModel.onAction(LogAction.ApplyStash(commit)) },
        onPopStash = { viewModel.onAction(LogAction.PopStash(commit)) },
        onDropStash = { viewModel.onAction(LogAction.DeleteStash(commit)) },
        diffSelected = diffSelected,
        commitChangesState = commitChangesState,
        onBlame = onBlame,
        onHistory = onHistory,
        onOpenFileInFolder = { viewModel.openFileInFolder(it) },
        onDiffSelected = {
            viewModel.onAction(CommitChangesAction.SelectEntry(it))
        },
        onSearchFilterToggled = { visible ->
            viewModel.onAction(CommitChangesAction.SearchFilterToggle(visible))
        },
        onSearchFocused = {
            viewModel.onAction(CommitChangesAction.AddSearchToCloseables)
        },
        onSearchFilterChanged = { filter ->
            viewModel.onAction(CommitChangesAction.SearchFilterChanged(filter))
        },
        onDirectoryClicked = { viewModel.onAction(CommitChangesAction.TreeDirectoryToggle(it.fullPath)) },
        onAlternateShowAsTree = { viewModel.onAction(CommitChangesAction.ToggleShowAsTree) },
    )
}

@Composable
private fun CommitChangesView(
    stats: CommitStats?,
    branchName: String?,
    isStash: Boolean,
    onCheckout: () -> Unit,
    onCherryPick: () -> Unit,
    onRevert: () -> Unit,
    onApplyStash: () -> Unit,
    onPopStash: () -> Unit,
    onDropStash: () -> Unit,
    commitChangesState: CommitChangesState,
    diffSelected: DiffSelected.CommitedChanges?,
    onBlame: (String) -> Unit,
    onHistory: (String) -> Unit,
    onOpenFileInFolder: (String) -> Unit,
    onDiffSelected: (DiffEntry) -> Unit,
    onSearchFilterToggled: (Boolean) -> Unit,
    onSearchFocused: () -> Unit,
    onSearchFilterChanged: (TextFieldValue) -> Unit,
    onDirectoryClicked: (TreeItem.Dir) -> Unit,
    onAlternateShowAsTree: () -> Unit,
) {
    val tabFocusRequester = LocalTabFocusRequester.current
    val commit = commitChangesState.commit
    val clipboard = LocalClipboardManager.current

    LaunchedEffect(commitChangesState.showSearch) {
        if (!commitChangesState.showSearch) {
            tabFocusRequester.requestFocus()
        }
    }

    val showSearch = commitChangesState.showSearch
    val showAsTree = commitChangesState.showAsTree

    // TODO Is this needed?
    var searchFilter by remember(commitChangesState.searchFilter, showSearch, commitChangesState) {
        mutableStateOf(commitChangesState.searchFilter)
    }

    val changesListScroll = rememberInTab("commitChangesChangesScroll", commitChangesState.commit) {
        LazyListState()
    }

    val textScroll = rememberInTab("commitChangesTextScroll", commitChangesState.commit) {
        ScrollState(0)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        CommitHeader(commit, branchName, textScroll)
        PanelDivider()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = true)
        ) {
            FilesChangedHeader(
                title = "Files changed",
                showAsTree = showAsTree,
                showSearch = showSearch,
                onAlternateShowAsTree = onAlternateShowAsTree,
                searchFilter = searchFilter,
                onSearchFocused = onSearchFocused,
                onSearchFilterToggled = onSearchFilterToggled,
                onSearchFilterChanged = onSearchFilterChanged,
                showActionForSelected = false,
                count = commitChangesState.changes.size,
                stats = stats?.let { it.added to it.removed },
            )

            if (commitChangesState.isLoading) {
                AppProgressBar()
            }

            if (commitChangesState.showAsTree) {
                TreeCommitLogChanges(
                    diffSelected = diffSelected,
                    changesListScroll = changesListScroll,
                    treeItems = if (showSearch && searchFilter.text.isNotBlank()) {
                        commitChangesState.changesTreeFiltered
                    } else {
                        commitChangesState.changesTree
                    },
                    onDiffSelected = onDiffSelected,
                    onGenerateContextMenu = { diffEntry ->
                        committedChangesEntriesContextMenuItems(
                            diffEntry,
                            onBlame = { onBlame(diffEntry.filePath) },
                            onHistory = { onHistory(diffEntry.filePath) },
                            onOpenFileInFolder = { onOpenFileInFolder(diffEntry.filePath) },
                        )
                    },
                    onDirectoryClicked = onDirectoryClicked,
                )
            } else {
                ListCommitLogChanges(
                    diffSelected = diffSelected,
                    changesListScroll = changesListScroll,
                    diffEntries = if (showSearch && searchFilter.text.isNotBlank()) {
                        commitChangesState.changesFiltered
                    } else {
                        commitChangesState.changes
                    },
                    onDiffSelected = onDiffSelected,
                    onGenerateContextMenu = { diffEntry ->
                        committedChangesEntriesContextMenuItems(
                            diffEntry,
                            onBlame = { onBlame(diffEntry.filePath) },
                            onHistory = { onHistory(diffEntry.filePath) },
                            onOpenFileInFolder = { onOpenFileInFolder(diffEntry.filePath) },
                        )
                    }
                )
            }
        }

        PanelDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isStash) {
                // A stash isn't part of the history: it is applied back to the working directory, not checked out
                PrimaryButton(
                    text = "Apply stash",
                    onClick = onApplyStash,
                    modifier = Modifier.weight(1f).height(34.dp),
                )
                GhostButton(text = "Pop stash", onClick = onPopStash, modifier = Modifier.height(34.dp))
            } else {
                PrimaryButton(
                    text = "Checkout",
                    onClick = onCheckout,
                    modifier = Modifier.weight(1f).height(34.dp),
                )
                GhostButton(text = "Cherry-pick", onClick = onCherryPick, modifier = Modifier.height(34.dp))
            }

            DropDownMenu(
                showIcons = false,
                items = {
                    val copyHash = ContextMenuElement.ContextTextEntry(
                        label = "Copy commit hash",
                        onClick = { clipboard.setText(AnnotatedString(commit.hash)) },
                    )

                    if (isStash) {
                        listOf(
                            copyHash,
                            ContextMenuElement.ContextTextEntry(label = "Drop stash", onClick = onDropStash, isDanger = true),
                        )
                    } else {
                        listOf(
                            ContextMenuElement.ContextTextEntry(label = "Revert commit", onClick = onRevert, isDanger = true),
                            copyHash,
                        )
                    }
                },
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(MaterialTheme.shapes.small)
                        .border(1.dp, MaterialTheme.colors.onBackground.copy(alpha = 0.16f), MaterialTheme.shapes.small),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.more_vert),
                        contentDescription = "More actions",
                        tint = MaterialTheme.colors.onBackground,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

/**
 * Hash and branch, subject, body and author of the selected commit.
 */
@Composable
private fun CommitHeader(
    commit: Commit,
    branchName: String?,
    textScroll: ScrollState,
) {
    var copied by remember(commit.hash) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val body = commit.message.trim().lines().drop(1).joinToString("\n").trim()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = commit.shortHash,
                fontFamily = monoTypography(),
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.primaryVariant,
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .handMouseClickable {
                        scope.launch {
                            clipboard.setText(AnnotatedString(commit.hash))
                            copied = true
                            delay(2000)
                            copied = false
                        }
                    },
            )

            Text(
                text = if (copied) "Copied!" else if (branchName != null) "on $branchName" else "",
                style = MaterialTheme.typography.caption,
                color = if (copied) MaterialTheme.colors.primaryVariant else MaterialTheme.colors.onBackgroundSecondary,
                maxLines = 1,
            )
        }

        SelectionContainer {
            Column(
                modifier = Modifier
                    .heightIn(max = 180.dp)
                    .verticalScroll(textScroll),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = commit.shortMessage,
                    style = MaterialTheme.typography.body1.copy(fontSize = 16.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colors.onBackground,
                )

                if (body.isNotEmpty()) {
                    Text(
                        text = body,
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onBackground.copy(alpha = 0.8f),
                    )
                }
            }
        }

        Row(
            modifier = Modifier.padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AvatarImage(modifier = Modifier.size(30.dp), personIdent = commit.author)

            Column {
                TooltipText(
                    text = commit.author.name.orEmpty(),
                    maxLines = 1,
                    style = MaterialTheme.typography.body2,
                    fontWeight = FontWeight.Medium,
                    tooltipTitle = commit.author.email.orEmpty(),
                )

                TooltipText(
                    text = commit.date.toSmartSystemString(allowRelative = true, showTime = true),
                    color = MaterialTheme.colors.onBackgroundSecondary,
                    maxLines = 1,
                    style = MaterialTheme.typography.caption,
                    tooltipTitle = commit.date.toSmartSystemString(allowRelative = false, showTime = true),
                )
            }
        }
    }
}

@Composable
fun ListCommitLogChanges(
    diffEntries: List<DiffEntry>,
    diffSelected: DiffSelected.CommitedChanges?,
    changesListScroll: LazyListState,
    onDiffSelected: (DiffEntry) -> Unit,
    onGenerateContextMenu: (DiffEntry) -> List<ContextMenuElement>,
) {
    ScrollableLazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        state = changesListScroll,
    ) {
        items(items = diffEntries) { diffEntry ->
            FileEntry(
                statusLetter = diffEntry.letter,
                statusColor = diffEntry.iconColor,
                parentDirectoryPath = diffEntry.parentDirectoryPath,
                fileName = diffEntry.fileName,
                isSelected = diffSelected?.items?.any { it.diffEntry == diffEntry} ?: false,
                onClick = { onDiffSelected(diffEntry) },
                onDoubleClick = {},
                onGenerateContextMenu = { onGenerateContextMenu(diffEntry) },
                trailingAction = null,
            )
        }
    }
}

@Composable
fun TreeCommitLogChanges(
    treeItems: List<TreeItem<DiffEntry>>,
    diffSelected: DiffSelected.CommitedChanges?,
    changesListScroll: LazyListState,
    onDiffSelected: (DiffEntry) -> Unit,
    onDirectoryClicked: (TreeItem.Dir) -> Unit,
    onGenerateContextMenu: (DiffEntry) -> List<ContextMenuElement>,
) {
    ScrollableLazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        state = changesListScroll,
    ) {
        items(items = treeItems) { entry ->
            CommitTreeItemEntry(
                entry = entry,
                isSelected = entry is TreeItem.File &&
                        diffSelected?.items?.any { it.diffEntry == entry.data } ?: false,
                onFileClick = { onDiffSelected(it) },
                onDirectoryClick = { onDirectoryClicked(it) },
                onGenerateContextMenu = onGenerateContextMenu,
                onGenerateDirectoryContextMenu = { emptyList() },
            )
        }
    }
}


@Composable
private fun CommitTreeItemEntry(
    entry: TreeItem<DiffEntry>,
    isSelected: Boolean,
    onFileClick: (DiffEntry) -> Unit,
    onDirectoryClick: (TreeItem.Dir) -> Unit,
    onGenerateContextMenu: (DiffEntry) -> List<ContextMenuElement>,
    onGenerateDirectoryContextMenu: (TreeItem.Dir) -> List<ContextMenuElement>,
) {
    when (entry) {
        is TreeItem.File -> CommitFileEntry(
            fileEntry = entry,
            isSelected = isSelected,
            onClick = { onFileClick(entry.data) },
            onGenerateContextMenu = onGenerateContextMenu,
        )

        is TreeItem.Dir -> DirectoryEntry(
            dirName = entry.displayName,
            isExpanded = entry.isExpanded,
            onClick = { onDirectoryClick(entry) },
            depth = entry.depth,
            onGenerateContextMenu = { onGenerateDirectoryContextMenu(entry) },
        )
    }
}

@Composable
private fun CommitFileEntry(
    fileEntry: TreeItem.File<DiffEntry>,
    isSelected: Boolean,
    onClick: () -> Unit,
    onGenerateContextMenu: (DiffEntry) -> List<ContextMenuElement>,
) {
    val diffEntry = fileEntry.data

    FileEntry(
        statusLetter = diffEntry.letter,
        statusColor = diffEntry.iconColor,
        parentDirectoryPath = "",
        fileName = diffEntry.fileName,
        isSelected = isSelected,
        onClick = onClick,
        onDoubleClick = {},
        depth = fileEntry.depth,
        onGenerateContextMenu = { onGenerateContextMenu(diffEntry) },
        trailingAction = null,
    )
}