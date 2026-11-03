package com.hugodehu.gitmiro.repositoryopen

import androidx.compose.foundation.background
import androidx.compose.material.Icon
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import com.hugodehu.gitmiro.theme.monoTypography
import com.hugodehu.gitmiro.theme.onBackgroundSecondary
import com.hugodehu.gitmiro.ui.components.tooltip.InstantTooltip
import androidx.compose.material.icons.Icons
import com.hugodehu.gitmiro.LocalTitleBarActions
import org.jetbrains.compose.resources.painterResource
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.hugodehu.gitmiro.LocalTabFocusRequester
import com.hugodehu.gitmiro.Screen
import com.hugodehu.gitmiro.app.generated.resources.*
import com.hugodehu.gitmiro.domain.models.Identity
import com.hugodehu.gitmiro.domain.models.PullType
import com.hugodehu.gitmiro.domain.models.RebaseInteractiveState
import com.hugodehu.gitmiro.domain.models.RepositoryState
import com.hugodehu.gitmiro.domain.models.ui.SelectedItem
import com.hugodehu.gitmiro.extensions.handMouseClickable
import com.hugodehu.gitmiro.keybindings.KeybindingOption
import com.hugodehu.gitmiro.keybindings.matchesBinding
import com.hugodehu.gitmiro.ui.*
import com.hugodehu.gitmiro.ui.components.BottomInfoBar
import com.hugodehu.gitmiro.ui.components.BottomBarAction
import com.hugodehu.gitmiro.compose.rememberInTab
import com.hugodehu.gitmiro.keybindings.keyBinding
import com.hugodehu.gitmiro.ui.components.TripleVerticalSplitPanel
import com.hugodehu.gitmiro.ui.diff.DiffPane
import com.hugodehu.gitmiro.ui.log.Log
import com.hugodehu.gitmiro.ui.status.StatusPane
import com.hugodehu.gitmiro.updates.Update
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
fun RepositoryOpenPage(
    repositoryOpenViewModel: RepositoryOpenViewModel,
    onNavigate: (Screen) -> Unit, // TODO Perhaps have specific callbacks instead of directly navigating
) {
    val repositoryState by repositoryOpenViewModel.repositoryState.collectAsState()
    val selectedItem by repositoryOpenViewModel.selectedItem.collectAsState()
    val blameState by repositoryOpenViewModel.blameState.collectAsState()
    val showHistory by repositoryOpenViewModel.showHistory.collectAsState()

    val focusRequester = remember { FocusRequester() }
    var isDetailsPanelCollapsed by rememberInTab("isDetailsPanelCollapsed") { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .focusRequester(focusRequester)
            .focusable(true)
            .onPreviewKeyEvent {
                when {
                    it.matchesBinding(KeybindingOption.PULL) -> {
                        repositoryOpenViewModel.pull(PullType.DEFAULT)
                        true
                    }

                    it.matchesBinding(KeybindingOption.PUSH) -> {
                        repositoryOpenViewModel.push(force = false, pushTags = false)
                        true
                    }

                    it.matchesBinding(KeybindingOption.BRANCH_CREATE) -> {
                        onNavigate(Screen.BranchCreate(null))
                        true
                    }

                    it.matchesBinding(KeybindingOption.STASH) -> {
                        repositoryOpenViewModel.stash()
                        true
                    }

                    it.matchesBinding(KeybindingOption.STASH_POP) -> {
                        repositoryOpenViewModel.popStash()
                        true
                    }

                    it.matchesBinding(KeybindingOption.EXIT) -> {
                        repositoryOpenViewModel.closeLastView()
                        true
                    }

                    it.matchesBinding(KeybindingOption.REFRESH) -> {
                        repositoryOpenViewModel.refreshAll()
                        true
                    }

                    it.matchesBinding(KeybindingOption.SETTINGS) -> {
                        onNavigate(Screen.GlobalSettings)
                        true
                    }

                    else -> false
                }

            }
    ) {
        CompositionLocalProvider(
            LocalTabFocusRequester provides focusRequester
        ) {
            val titleBarActions = LocalTitleBarActions.current

            val menu: @Composable () -> Unit = {
                Menu(
                    viewModel = repositoryOpenViewModel,
                    modifier = Modifier.padding(end = 8.dp),
                    onStashWithMessage = { onNavigate(Screen.StashWithMessage) },
                )
            }

            SideEffect {
                titleBarActions.value = menu
            }
            DisposableEffect(titleBarActions) {
                onDispose {
                    titleBarActions.value = null
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                RepoContent(
                    repositoryOpenViewModel = repositoryOpenViewModel,
                    selectedItem = selectedItem,
                    repositoryState = repositoryState,
                    blameState = blameState,
                    showHistory = showHistory,
                    isDetailsPanelCollapsed = isDetailsPanelCollapsed,
                    onToggleDetailsPanel = { isDetailsPanelCollapsed = !isDetailsPanelCollapsed },
                    onNavigate = onNavigate,
                )
            }
        }


        val userInfo by repositoryOpenViewModel.authorInfoSimple.collectAsState()
        val newUpdate = repositoryOpenViewModel.update.collectAsState().value

        RepositoryOpenBottomInfoBar(
            userInfo,
            newUpdate,
            onOpenUrlInBrowser = { repositoryOpenViewModel.openUrlInBrowser(it) },
            onShowAuthorInfoDialog = { onNavigate(Screen.Author) },
            onOpenTerminal = { repositoryOpenViewModel.openTerminal() },
            onQuickActions = { onNavigate(Screen.QuickActions) },
            onShowSettings = { onNavigate(Screen.GlobalSettings) },
        )
    }

    LaunchedEffect(repositoryOpenViewModel) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(repositoryOpenViewModel) {
        repositoryOpenViewModel.branchesToPrune.collect { onNavigate(Screen.PruneBranches(it)) }
    }
}

@Composable
private fun RepositoryOpenBottomInfoBar(
    userInfo: UiDataState<Identity>,
    newUpdate: Update?,
    onOpenUrlInBrowser: (String) -> Unit,
    onShowAuthorInfoDialog: () -> Unit,
    onOpenTerminal: () -> Unit,
    onQuickActions: () -> Unit,
    onShowSettings: () -> Unit,
) {
    BottomInfoBar(
        newUpdate,
        onOpenUrlInBrowser,
        actions = {
            BottomBarAction(
                title = stringResource(Res.string.menu_terminal),
                icon = painterResource(Res.drawable.terminal),
                tooltip = stringResource(Res.string.menu_terminal_tooltip),
                onClick = onOpenTerminal,
            )
            BottomBarAction(
                title = stringResource(Res.string.menu_actions),
                icon = painterResource(Res.drawable.bolt),
                tooltip = stringResource(Res.string.menu_actions_tooltip),
                onClick = onQuickActions,
            )
            BottomBarAction(
                title = stringResource(Res.string.menu_settings),
                icon = painterResource(Res.drawable.settings),
                tooltip = stringResource(Res.string.menu_settings_tooltip),
                keybinding = KeybindingOption.SETTINGS.keyBinding,
                onClick = onShowSettings,
            )
        },
        leadingContent = {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .handMouseClickable { onShowAuthorInfoDialog() },
                contentAlignment = Alignment.Center,
            ) {
                val identity = userInfo.data

                if (identity != null) {
                    val name = identity.name ?: stringResource(Res.string.bottom_info_bar_name_not_set)
                    val email = identity.email ?: stringResource(Res.string.bottom_info_bar_email_not_set)

                    Text(
                        text = stringResource(Res.string.bottom_info_bar_name_and_email, name, email),
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onBackground,
                    )
                }
            }
        }
    )
}

@Composable
fun RepoContent(
    repositoryOpenViewModel: RepositoryOpenViewModel,
    selectedItem: SelectedItem,
    repositoryState: RepositoryState,
    blameState: BlameState,
    showHistory: Boolean,
    isDetailsPanelCollapsed: Boolean,
    onToggleDetailsPanel: () -> Unit,
    onNavigate: (Screen) -> Unit,
) {
    if (showHistory) {
        val historyViewModel = repositoryOpenViewModel.historyViewModel

        if (historyViewModel != null) {
            FileHistory(
                historyViewModel = historyViewModel,
                onClose = {
                    repositoryOpenViewModel.closeHistory()
                }
            )
        }
    } else {
        MainContentView(
            viewModel = repositoryOpenViewModel,
            selectedItem = selectedItem,
            repositoryState = repositoryState,
            blameState = blameState,
            isDetailsPanelCollapsed = isDetailsPanelCollapsed,
            onToggleDetailsPanel = onToggleDetailsPanel,
            onNavigate = onNavigate,
        )
    }
}

@Composable
fun MainContentView(
    viewModel: RepositoryOpenViewModel,
    selectedItem: SelectedItem,
    repositoryState: RepositoryState,
    blameState: BlameState,
    isDetailsPanelCollapsed: Boolean,
    onToggleDetailsPanel: () -> Unit,
    onNavigate: (Screen) -> Unit,
) {
    val diffSelected by viewModel.diffSelected.collectAsState()
    val rebaseInteractiveState by viewModel.rebaseInteractiveState.collectAsState()
    val density = LocalDensity.current.density
    val scope = rememberCoroutineScope()

    val statusState by viewModel.statusState.collectAsState()

    // We create 2 mutableStates here because using directly the flow makes compose lose some drag events for some reason
    var firstWidth by remember(viewModel) { mutableStateOf(viewModel.firstPaneWidth.value) }
    var thirdWidth by remember(viewModel) { mutableStateOf(viewModel.thirdPaneWidth.value) }

    LaunchedEffect(Unit) {
        // Update the pane widths if they have been changed in a different tab
        viewModel.onPanelsWidthPersisted.collectLatest {
            firstWidth = viewModel.firstPaneWidth.value
            thirdWidth = viewModel.thirdPaneWidth.value
        }
    }

    val isRebaseInteractive = rebaseInteractiveState is RebaseInteractiveState.AwaitingInteraction
    val commitChangesState = viewModel.commitChangesState.collectAsState().value

    Row {
    TripleVerticalSplitPanel(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
        firstWidth = if (isRebaseInteractive) 0f else firstWidth,
        thirdWidth = if (isDetailsPanelCollapsed) 0f else thirdWidth,
        first = {
            SidePanel(
                viewModel = viewModel,
                onNavigate = onNavigate,
            )
        },
        second = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                if (
                    rebaseInteractiveState is RebaseInteractiveState.AwaitingInteraction
                    && diffSelected == null
                ) {
                    RebaseInteractive(viewModel, rebaseInteractiveState)
                } else if (blameState is BlameState.Loaded && !blameState.isMinimized) {
                    Blame(
                        filePath = blameState.filePath,
                        blameResult = blameState.blameResult,
                        onClose = { viewModel.resetBlameState() },
                        onSelectCommit = { viewModel.selectCommit(it, isStash = false) }
                    )
                } else {
                    Column {
                        Box(modifier = Modifier.weight(1f, true)) {
                            val showDiff = diffSelected?.entries?.count() == 1

                            Log(
                                modifier = Modifier.focusable(!showDiff),
                                viewModel = viewModel,
                                selectedItem = selectedItem,
                                repositoryState = repositoryState,
                                // TODO Move nav outside of this? Applies to next lines
                                onCreateBranch = { onNavigate(Screen.BranchCreate(it)) },
                                onResetBranch = { onNavigate(Screen.BranchReset(it)) },
                                onCreateTag = { onNavigate(Screen.TagCreate(it)) },
                                onChangeUpstreamBranch = { onNavigate(Screen.BranchChangeUpstream(it)) },
                                onRenameBranch = { onNavigate(Screen.BranchRename(it)) },
                            )

                            if (showDiff) {
                                val tabFocusRequester = LocalTabFocusRequester.current

                                DiffPane(
                                    viewModel = viewModel,
                                    onCloseDiffView = {
                                        tabFocusRequester.requestFocus()
                                    },
                                    onDeletedFileConflict = { filePath, deletedBy, repositoryState ->
                                        onNavigate(Screen.DeletedFileConflict(filePath, deletedBy, repositoryState))
                                    },
                                )
                            }
                        }

                        if (blameState is BlameState.Loaded) { // BlameState.isMinimized is true here
                            MinimizedBlame(
                                filePath = blameState.filePath,
                                onExpand = { viewModel.expandBlame() },
                                onClose = { viewModel.resetBlameState() }
                            )
                        }
                    }
                }
            }
        },
        third = {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
            ) {
                DetailsPanelHeader(
                    title = if (commitChangesState != null) "Commit" else "Changes",
                    subtitle = commitChangesState?.commit?.shortHash,
                    onCollapse = onToggleDetailsPanel,
                )

                if (commitChangesState != null) {
                    CommitChanges(
                        viewModel = viewModel,
                        onBlame = { viewModel.blameFile(it) },
                        onHistory = { viewModel.fileHistory(it) },
                        commitChangesState = commitChangesState,
                    )
                } else if (rebaseInteractiveState !is RebaseInteractiveState.AwaitingInteraction) {
                    StatusPane(
                        statusState = statusState,
                        completedTasks = viewModel.completedTasks,
                        onAction = { viewModel.onAction(it) },
                        onBlameFile = { viewModel.blameFile(it) },
                        onHistoryFile = { viewModel.fileHistory(it) }
                    )
                }
            }
        },
        onFirstSizeDragStarted = { currentWidth ->
            firstWidth = currentWidth
            viewModel.setFirstPaneWidth(currentWidth)
        },
        onFirstSizeChange = {
            val newWidth = firstWidth + it / density

            if (newWidth > 150 && rebaseInteractiveState !is RebaseInteractiveState.AwaitingInteraction) {
                firstWidth = newWidth
                viewModel.setFirstPaneWidth(newWidth)
            }
        },
        onFirstSizeDragStopped = {
            scope.launch {
                viewModel.persistFirstPaneWidth()
            }
        },
        onThirdSizeChange = {
            val newWidth = thirdWidth - it / density

            if (newWidth > 150) {
                thirdWidth = newWidth
                viewModel.setThirdPaneWidth(newWidth)
            }
        },
        onThirdSizeDragStarted = { currentWidth ->
            thirdWidth = currentWidth
            viewModel.setThirdPaneWidth(currentWidth)
        },
        onThirdSizeDragStopped = {
            scope.launch {
                viewModel.persistThirdPaneWidth()
            }
        },
    )

    if (isDetailsPanelCollapsed) {
        CollapsedDetailsPanel(onExpand = onToggleDetailsPanel)
    }
    }
}

@Composable
private fun DetailsPanelHeader(
    title: String,
    subtitle: String?,
    onCollapse: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .padding(start = 14.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.body1,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colors.onBackground,
            maxLines = 1,
        )

        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.body2,
                fontFamily = monoTypography(),
                color = MaterialTheme.colors.onBackgroundSecondary,
                maxLines = 1,
                modifier = Modifier.padding(start = 8.dp),
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        PanelToggleButton(
            icon = Icons.Outlined.ChevronRight,
            tooltip = "Collapse the panel",
            onClick = onCollapse,
        )
    }
}

/**
 * Thin strip left on the right edge of the window while the details panel is collapsed, to open it again.
 */
@Composable
private fun CollapsedDetailsPanel(onExpand: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .padding(end = 8.dp, bottom = 8.dp)
            .width(36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(6.dp))

        PanelToggleButton(
            icon = Icons.Outlined.ChevronLeft,
            tooltip = "Show the changes and commit panel",
            onClick = onExpand,
        )
    }
}

@Composable
private fun PanelToggleButton(
    icon: ImageVector,
    tooltip: String,
    onClick: () -> Unit,
) {
    InstantTooltip(text = tooltip) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colors.onBackground.copy(alpha = 0.06f))
                .handMouseClickable { onClick() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = tooltip,
                tint = MaterialTheme.colors.onBackgroundSecondary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
