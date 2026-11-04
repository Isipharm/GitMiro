@file:OptIn(ExperimentalComposeUiApi::class)

package com.hugodehu.gitmiro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hugodehu.gitmiro.LocalTabFocusRequester
import com.hugodehu.gitmiro.Screen
import com.hugodehu.gitmiro.app.generated.resources.*
import com.hugodehu.gitmiro.domain.models.*
import com.hugodehu.gitmiro.domain.models.ui.SelectedItem
import com.hugodehu.gitmiro.extensions.handOnHover
import com.hugodehu.gitmiro.extensions.setClipboardText
import com.hugodehu.gitmiro.keybindings.KeybindingOption
import com.hugodehu.gitmiro.keybindings.keyBinding
import com.hugodehu.gitmiro.repositoryopen.RepositoryOpenViewModel
import com.hugodehu.gitmiro.theme.onBackgroundSecondary
import com.hugodehu.gitmiro.ui.components.AdjustableOutlinedTextField
import com.hugodehu.gitmiro.ui.components.ScrollableLazyColumn
import com.hugodehu.gitmiro.ui.components.SideMenuHeader
import com.hugodehu.gitmiro.ui.components.SideMenuSubentry
import com.hugodehu.gitmiro.ui.components.tooltip.DelayedTooltip
import com.hugodehu.gitmiro.ui.components.tooltip.InstantTooltip
import com.hugodehu.gitmiro.ui.context_menu.*
import com.hugodehu.gitmiro.viewmodels.sidepanel.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun SidePanel(
    viewModel: RepositoryOpenViewModel,
    onNavigate: (Screen) -> Unit,
) {
    val filter by viewModel.filter.collectAsState()
    val selectedItem by viewModel.selectedItem.collectAsState()

    val branchesState by viewModel.branchesState.collectAsState()
    val remotesState by viewModel.remoteState.collectAsState()
    val tagsState by viewModel.tagsState.collectAsState()
    val stashesState by viewModel.stashesState.collectAsState()
    val submodulesState by viewModel.submodulesState.collectAsState()

    val searchFocusRequester = remember { FocusRequester() }
    val tabFocusRequester = LocalTabFocusRequester.current

    LaunchedEffect(viewModel) {
        viewModel.freeSearchFocusFlow.collectLatest {
            tabFocusRequester.requestFocus()
        }
    }

    Column(modifier = Modifier.padding(12.dp)) {
        FilterTextField(
            value = filter,
            onValueChange = { newValue ->
                viewModel.newFilter(newValue)
            },
            modifier = Modifier
                .focusRequester(searchFocusRequester)
                .onFocusChanged {
                    if (it.isFocused) {
                        viewModel.addSidePanelSearchToCloseables()
                    } else {
                        viewModel.removeSidePanelSearchFromCloseables()
                    }
                }
        )

        ScrollableLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 10.dp)
        ) {
            localBranches(
                branchesState = branchesState,
                selectedItem = selectedItem,
                viewModel = viewModel,
                onChangeDefaultUpstreamBranch = { onNavigate(Screen.BranchChangeUpstream(it)) },
                onRenameBranch = { onNavigate(Screen.BranchRename(it)) },
                onCreateBranch = { onNavigate(Screen.BranchCreate(null)) },
            )

            remotes(
                remotesState = remotesState,
                viewModel = viewModel,
                onShowAddEditRemoteDialog = { onNavigate(Screen.AddEditRemote(it)) },
            )

            tags(
                tagsState = tagsState,
                selectedItem = selectedItem,
                viewModel = viewModel,
            )

            stashes(
                stashesState = stashesState,
                selectedItem = selectedItem,
                onExpandStashes = { viewModel.onExpandStashes() },
                onSelectStash = { viewModel.selectStash(it) },
                onApplyStash = { viewModel.applyStash(it) },
                onPopStash = { viewModel.popStash(it) },
                onDeleteStash = { viewModel.deleteStash(it) },
            )

            submodules(
                submodulesState = submodulesState,
                viewModel = viewModel,
                onAddSubmodule = { onNavigate(Screen.SubmoduleAdd) },
            )
        }
    }
}

@Composable
fun FilterTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = MaterialTheme.shapes.small
    val borderColor = if (isFocused) MaterialTheme.colors.primary else MaterialTheme.colors.onBackground.copy(alpha = 0.12f)

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        interactionSource = interactionSource,
        textStyle = MaterialTheme.typography.body2.copy(color = MaterialTheme.colors.onBackground),
        cursorBrush = SolidColor(MaterialTheme.colors.primary),
        modifier = modifier
            .fillMaxWidth()
            .height(34.dp)
            .background(MaterialTheme.colors.background, shape)
            .border(1.dp, borderColor, shape),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painterResource(Res.drawable.search),
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colors.onBackgroundSecondary,
                )

                Box(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                    if (value.isEmpty()) {
                        Text(
                            text = stringResource(Res.string.side_pane_search_hint),
                            style = MaterialTheme.typography.body2,
                            color = MaterialTheme.colors.onBackgroundSecondary,
                            maxLines = 1,
                        )
                    }

                    innerTextField()
                }

                if (value.isNotEmpty()) {
                    Icon(
                        painterResource(Res.drawable.close),
                        contentDescription = "Clear the filter",
                        tint = MaterialTheme.colors.onBackground,
                        modifier = Modifier
                            .size(14.dp)
                            .handOnHover()
                            .clickable { onValueChange("") },
                    )
                }
            }
        },
    )
}

@OptIn(ExperimentalComposeUiApi::class)
fun LazyListScope.localBranches(
    branchesState: BranchesState,
    selectedItem: SelectedItem,
    viewModel: RepositoryOpenViewModel,
    onChangeDefaultUpstreamBranch: (Branch) -> Unit,
    onRenameBranch: (Branch) -> Unit,
    onCreateBranch: () -> Unit,
) {
    val isExpanded = branchesState.isExpanded
    val branches = branchesState.branches
    val currentBranch = branchesState.currentBranch

    item {
        ContextMenu(
            items = { emptyList() }
        ) {
            SideMenuHeader(
                text = stringResource(Res.string.side_pane_local_branches_title),
                icon = painterResource(Res.drawable.branch),
                itemsCount = branches.count(),
                hoverIcon = {
                    val keybinding = KeybindingOption.BRANCH_CREATE.keyBinding

                    InstantTooltip(
                        text = stringResource(Res.string.menu_branch_tooltip),
                        trailingContent = keybinding?.let { { KeybindingHint(it) } },
                    ) {
                        IconButton(
                            onClick = onCreateBranch,
                            modifier = Modifier
                                .padding(end = 0.dp)
                                .size(16.dp)
                                .handOnHover(),
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.add),
                                contentDescription = stringResource(Res.string.menu_branch_tooltip),
                                modifier = Modifier.fillMaxSize(),
                                tint = MaterialTheme.colors.onBackground,
                            )
                        }
                    }
                },
                isExpanded = isExpanded,
                onExpand = { viewModel.onExpandBranches() }
            )
        }
    }

    if (isExpanded) {
        items(branches, key = { it.name }) { branch ->
            val scope = rememberCoroutineScope()
            val clipboard = LocalClipboard.current

            Branch(
                branch = branch,
                isSelectedItem = selectedItem is SelectedItem.BranchItem && selectedItem.branch == branch,
                currentBranch = currentBranch,
                onBranchClicked = { viewModel.selectBranch(branch) },
                onBranchDoubleClicked = { viewModel.checkoutBranch(branch) },
                onCheckoutBranch = { viewModel.checkoutBranch(it) },
                onMergeBranch = { viewModel.mergeBranch(it) },
                onRebaseBranch = { viewModel.rebaseBranch(it) },
                onDeleteBranch = { viewModel.deleteBranch(it) },
                onChangeDefaultUpstreamBranch = { onChangeDefaultUpstreamBranch(it) },
                onRenameBranch = { onRenameBranch(it) },
                onCopyBranchNameToClipboard = {
                    scope.launch {
                        clipboard.setClipboardText(it.simpleName)
                    }
                },
            )
        }
    }
}

fun LazyListScope.remotes(
    remotesState: RemotesState,
    viewModel: RepositoryOpenViewModel,
    onShowAddEditRemoteDialog: (Remote?) -> Unit,
) {
    val isExpanded = remotesState.isExpanded
    val remotes = remotesState.remotes

    item {
        SideMenuHeader(
            text = stringResource(Res.string.side_pane_remotes_title),
            icon = painterResource(Res.drawable.cloud),
            itemsCount = remotes.count(),
            hoverIcon = {
                IconButton(
                    onClick = { onShowAddEditRemoteDialog(null) },
                    modifier = Modifier
                        .padding(end = 0.dp)
                        .size(16.dp)
                        .handOnHover(),
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.add),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize(),
                        tint = MaterialTheme.colors.onBackground,
                    )
                }
            },
            isExpanded = isExpanded,
            onExpand = { viewModel.onExpandRemotes() }
        )
    }

    if (isExpanded) {
        for (remote in remotes) {
            item {
                Remote(
                    remote = remote,
                    onEditRemote = {
                        val wrapper = remote.remoteInfo.remote
                        onShowAddEditRemoteDialog(wrapper)
                    },
                    onDeleteRemote = { viewModel.deleteRemote(remote.remoteInfo) },
                    onRemoteClicked = { viewModel.onRemoteClicked(remote) },
                    onFetchBranches = { viewModel.onFetchRemoteBranches(remote) },
                )
            }

            if (remote.isExpanded) {
                items(remote.remoteInfo.branchesList) { remoteBranch ->
                    val scope = rememberCoroutineScope()
                    val clipboard = LocalClipboard.current
                    RemoteBranches(
                        remoteBranch = remoteBranch,
                        currentBranch = remotesState.currentBranch,
                        onBranchClicked = { viewModel.selectBranch(remoteBranch) },
                        onCheckoutBranch = { viewModel.checkoutRemoteBranch(it) },
                        onDeleteBranch = { viewModel.deleteRemoteBranch(it) },
                        onPushRemoteBranch = { viewModel.pushToRemoteBranch(it) },
                        onPullRemoteBranch = { viewModel.pullFromRemoteBranch(it) },
                        onRebaseRemoteBranch = { viewModel.rebaseBranch(it) },
                        onMergeRemoteBranch = { viewModel.mergeBranch(it) },
                        onCopyBranchNameToClipboard = {
                            scope.launch {
                                clipboard.setClipboardText(it.simpleName)
                            }
                        }
                    )
                }
            }
        }
    }
}


fun LazyListScope.tags(
    tagsState: TagsState,
    viewModel: RepositoryOpenViewModel,
    selectedItem: SelectedItem,
) {
    val isExpanded = tagsState.isExpanded
    val tags = tagsState.tags

    item {
        ContextMenu(
            items = { emptyList() }
        ) {
            SideMenuHeader(
                text = stringResource(Res.string.side_pane_tags_title),
                icon = painterResource(Res.drawable.tag),
                itemsCount = tags.count(),
                hoverIcon = null,
                isExpanded = isExpanded,
                onExpand = { viewModel.onExpandTags() }
            )
        }
    }

    if (isExpanded) {
        items(tags, key = { it.name }) { tag ->
            Tag(
                tag,
                isSelected = selectedItem is SelectedItem.TagItem && selectedItem.tag == tag,
                onTagClicked = { viewModel.selectTag(tag) },
                onCheckoutTag = { viewModel.checkoutTagCommit(tag) },
                onDeleteTag = { viewModel.deleteTag(tag) }
            )
        }
    }
}

fun LazyListScope.stashes(
    stashesState: StashesState,
    selectedItem: SelectedItem,
    onExpandStashes: () -> Unit,
    onSelectStash: (Commit) -> Unit,
    onApplyStash: (Commit) -> Unit,
    onPopStash: (Commit) -> Unit,
    onDeleteStash: (Commit) -> Unit,
) {
    val isExpanded = stashesState.isExpanded
    val stashes = stashesState.stashes

    item {
        ContextMenu(
            items = { emptyList() }
        ) {
            SideMenuHeader(
                text = stringResource(Res.string.side_pane_stashes_title),
                icon = painterResource(Res.drawable.stash),
                itemsCount = stashes.count(),
                hoverIcon = null,
                isExpanded = isExpanded,
                onExpand = onExpandStashes,
            )
        }
    }

    if (isExpanded) {
        items(stashes, key = { it.hash }) { stash ->
            Stash(
                stash,
                isSelected = selectedItem is SelectedItem.CommitItem && selectedItem.isStash && selectedItem.commit.hash == stash.hash,
                onClick = { onSelectStash(stash) },
                onApply = { onApplyStash(stash) },
                onPop = { onPopStash(stash) },
                onDelete = { onDeleteStash(stash) },
            )
        }
    }
}

fun LazyListScope.submodules(
    submodulesState: SubmodulesState,
    viewModel: RepositoryOpenViewModel,
    onAddSubmodule: () -> Unit,
) {
    val isExpanded = submodulesState.isExpanded
    val submodules = submodulesState.submodules

    item {
        ContextMenu(
            items = { emptyList() }
        ) {
            SideMenuHeader(
                text = stringResource(Res.string.side_pane_submodules_title),
                icon = painterResource(Res.drawable.topic),
                itemsCount = submodules.count(),
                hoverIcon = {
                    IconButton(
                        onClick = onAddSubmodule,
                        modifier = Modifier
                            .padding(end = 0.dp)
                            .size(16.dp)
                            .handOnHover(),
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.add),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize(),
                            tint = MaterialTheme.colors.onBackground,
                        )
                    }
                },
                isExpanded = isExpanded,
                onExpand = { viewModel.onExpandSubmodules() }
            )
        }
    }

    if (isExpanded) {
        items(submodules, key = { it.first }) { submodule ->
            Submodule(
                submodule = submodule,
                onInitializeSubmodule = { viewModel.initializeSubmodule(submodule.first) },
//                onDeinitializeSubmodule = { submodulesViewModel.onDeinitializeSubmodule(submodule.first) },
                onSyncSubmodule = { viewModel.syncSubmodule(submodule.first) },
                onUpdateSubmodule = { viewModel.updateSubmodule(submodule.first) },
                onOpenSubmoduleInTab = { viewModel.onOpenSubmoduleInTab(submodule.first) },
                onDeleteSubmodule = { viewModel.deleteSubmodule(submodule.first) },
            )
        }
    }
}

@Composable
private fun Branch(
    branch: Branch,
    currentBranch: Branch?,
    isSelectedItem: Boolean,
    onBranchClicked: () -> Unit,
    onBranchDoubleClicked: () -> Unit,
    onCheckoutBranch: (Branch) -> Unit,
    onMergeBranch: (Branch) -> Unit,
    onRebaseBranch: (Branch) -> Unit,
    onDeleteBranch: (Branch) -> Unit,
    onChangeDefaultUpstreamBranch: (Branch) -> Unit,
    onRenameBranch: (Branch) -> Unit,
    onCopyBranchNameToClipboard: (Branch) -> Unit,
) {
    val isCurrentBranch = currentBranch?.name == branch.name

    ContextMenu(
        items = {
            branchContextMenuItems(
                branch = branch,
                currentBranch = currentBranch,
                isCurrentBranch = isCurrentBranch,
                isLocal = true,
                onCheckoutBranch = onCheckoutBranch,
                onMergeBranch = onMergeBranch,
                onDeleteBranch = onDeleteBranch,
                onRebaseBranch = onRebaseBranch,
                onPushToRemoteBranch = {},
                onPullFromRemoteBranch = {},
                onChangeDefaultUpstreamBranch = onChangeDefaultUpstreamBranch,
                onRenameBranch = onRenameBranch,
                onCopyBranchNameToClipboard = onCopyBranchNameToClipboard
            )
        }
    ) {
        SideMenuSubentry(
            text = branch.simpleName,
            iconResourcePath = Res.drawable.branch,
            isSelected = isSelectedItem,
            isCurrent = isCurrentBranch,
            onClick = onBranchClicked,
            onDoubleClick = onBranchDoubleClicked,
        ) {
            if (isCurrentBranch) {
                Text(
                    text = stringResource(Res.string.side_pane_local_branches_current_branch_label),
                    color = MaterialTheme.colors.primaryVariant,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .background(MaterialTheme.colors.primary.copy(alpha = 0.22f))
                        .padding(horizontal = 6.dp, vertical = 1.dp),
                )
            }
        }
    }
}


@Composable
private fun Remote(
    remote: RemoteView,
    onEditRemote: () -> Unit,
    onDeleteRemote: () -> Unit,
    onFetchBranches: () -> Unit,
    onRemoteClicked: () -> Unit,
) {
    ContextMenu(
        items = {
            remoteContextMenu(
                onEdit = onEditRemote,
                onDelete = onDeleteRemote,
                onFetch = onFetchBranches,
            )
        }
    ) {
        SideMenuSubentry(
            text = remote.remoteInfo.remote.name,
            iconResourcePath = Res.drawable.cloud,
            onClick = onRemoteClicked,
            isSelected = false,
        )
    }
}


@Composable
private fun RemoteBranches(
    remoteBranch: Branch,
    currentBranch: Branch?,
    onBranchClicked: () -> Unit,
    onCheckoutBranch: (Branch) -> Unit,
    onDeleteBranch: (Branch) -> Unit,
    onPushRemoteBranch: (Branch) -> Unit,
    onPullRemoteBranch: (Branch) -> Unit,
    onRebaseRemoteBranch: (Branch) -> Unit,
    onMergeRemoteBranch: (Branch) -> Unit,
    onCopyBranchNameToClipboard: (Branch) -> Unit,
) {
    ContextMenu(
        items = {
            branchContextMenuItems(
                branch = remoteBranch,
                currentBranch = currentBranch,
                isCurrentBranch = false,
                isLocal = false,
                onCheckoutBranch = onCheckoutBranch,
                onMergeBranch = onMergeRemoteBranch,
                onDeleteBranch = {},
                onDeleteRemoteBranch = onDeleteBranch,
                onRebaseBranch = onRebaseRemoteBranch,
                onPushToRemoteBranch = onPushRemoteBranch,
                onPullFromRemoteBranch = onPullRemoteBranch,
                onChangeDefaultUpstreamBranch = {},
                onRenameBranch = {},
                onCopyBranchNameToClipboard = onCopyBranchNameToClipboard
            )
        }
    ) {
        SideMenuSubentry(
            text = remoteBranch.simpleName,
            extraPadding = 24.dp,
            isSelected = false,
            iconResourcePath = Res.drawable.branch,
            onClick = onBranchClicked,
            onDoubleClick = { onCheckoutBranch(remoteBranch) },
        )
    }
}

@Composable
private fun Tag(
    tag: Tag,
    isSelected: Boolean,
    onTagClicked: () -> Unit,
    onCheckoutTag: () -> Unit,
    onDeleteTag: () -> Unit,
) {
    ContextMenu(
        items = {
            tagContextMenuItems(
                onCheckoutTag = onCheckoutTag,
                onDeleteTag = onDeleteTag,
            )
        }
    ) {
        SideMenuSubentry(
            text = tag.simpleName,
            isSelected = isSelected,
            iconResourcePath = Res.drawable.tag,
            onClick = onTagClicked,
        )
    }
}


@Composable
private fun Stash(
    stash: Commit,
    isSelected: Boolean,
    onClick: () -> Unit,
    onApply: () -> Unit,
    onPop: () -> Unit,
    onDelete: () -> Unit,
) {
    ContextMenu(
        items = {
            stashesContextMenuItems(
                onApply = onApply,
                onPop = onPop,
                onDelete = onDelete,
            )
        }
    ) {
        SideMenuSubentry(
            text = stash.shortMessage,
            isSelected = isSelected,
            iconResourcePath = Res.drawable.stash,
            onClick = onClick,
        )
    }
}

@Composable
private fun Submodule(
    submodule: Pair<String, Submodule>,
    onInitializeSubmodule: () -> Unit,
//    onDeinitializeSubmodule: () -> Unit,
    onSyncSubmodule: () -> Unit,
    onUpdateSubmodule: () -> Unit,
    onOpenSubmoduleInTab: () -> Unit,
    onDeleteSubmodule: () -> Unit,
) {
    ContextMenu(
        items = {
            submoduleContextMenuItems(
                submodule.second,
                onInitializeSubmodule = onInitializeSubmodule,
//                onDeinitializeSubmodule = onDeinitializeSubmodule,
                onSyncSubmodule = onSyncSubmodule,
                onUpdateSubmodule = onUpdateSubmodule,
                onOpenSubmoduleInTab = onOpenSubmoduleInTab,
                onDeleteSubmodule = onDeleteSubmodule,
            )
        }
    ) {
        SideMenuSubentry(
            text = submodule.first,
            iconResourcePath = Res.drawable.topic,
            isSelected = false,
            onClick = {
                if (submodule.second.state.isValid) {
                    onOpenSubmoduleInTab()
                }
            },
        ) {
            val stateName = submodule.second.state.toString()
            DelayedTooltip(stateName) {
                Text(
                    text = stateName.first().toString(),
                    color = MaterialTheme.colors.onBackgroundSecondary,
                    style = MaterialTheme.typography.body2,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
    }
}