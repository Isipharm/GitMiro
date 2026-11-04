@file:OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)

package com.hugodehu.gitmiro.ui.log

import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import com.hugodehu.gitmiro.ui.components.AppProgressBar
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.hugodehu.gitmiro.ui.components.PanelDivider
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hugodehu.gitmiro.app.generated.resources.*
import com.hugodehu.gitmiro.common.printLog
import com.hugodehu.gitmiro.domain.BranchesConstants.LOCAL_PREFIX_LENGTH
import com.hugodehu.gitmiro.domain.models.*
import com.hugodehu.gitmiro.domain.models.ui.SelectedItem
import com.hugodehu.gitmiro.extensions.*
import com.hugodehu.gitmiro.keybindings.KeybindingOption
import com.hugodehu.gitmiro.keybindings.matchesBinding
import com.hugodehu.gitmiro.repositoryopen.LogAction
import com.hugodehu.gitmiro.repositoryopen.LogSearch
import com.hugodehu.gitmiro.repositoryopen.LogState
import com.hugodehu.gitmiro.repositoryopen.RepositoryOpenViewModel
import com.hugodehu.gitmiro.theme.*
import com.hugodehu.gitmiro.ui.components.AvatarImage
import com.hugodehu.gitmiro.ui.components.ScrollableLazyColumn
import com.hugodehu.gitmiro.ui.components.tooltip.InstantTooltip
import com.hugodehu.gitmiro.ui.components.tooltip.InstantTooltipPosition
import com.hugodehu.gitmiro.ui.context_menu.*
import com.hugodehu.gitmiro.ui.resizePointerIconEast
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.eclipse.jgit.lib.Constants
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import java.util.LinkedHashMap

private const val CANVAS_MIN_WIDTH = 60
private const val CANVAS_DEFAULT_WIDTH = 80
private const val MIN_GRAPH_LANES = 2

private const val HORIZONTAL_SCROLL_PIXELS_MULTIPLIER = 10

private const val ARC_DIRECTION_LEFT = -1
private const val ARC_DIRECTION_RIGHT = 1

/**
 * Additional number of lanes to simulate to create a margin at the end of the graph.
 */
private const val MARGIN_GRAPH_LANES = 2
private const val LANE_WIDTH = 18f
private const val NODE_SIZE = 10
private const val DIVIDER_WIDTH = 8
private const val ROW_INSET = 6

private const val LOG_BOTTOM_PADDING = 80

private const val MIN_COMMITS_BEFORE_REQUESTING_MORE = 200

private const val TAG = "LogView"

// TODO Min size for message column
@Composable
fun Log(
    modifier: Modifier,
    viewModel: RepositoryOpenViewModel,
    selectedItem: SelectedItem,
    repositoryState: RepositoryState,
    onCreateBranch: (Commit) -> Unit,
    onResetBranch: (Commit) -> Unit,
    onCreateTag: (Commit) -> Unit,
    onChangeUpstreamBranch: (Branch) -> Unit,
    onRenameBranch: (Branch) -> Unit,
) {
    val logStatusState = viewModel.logState.collectAsState()
    val logStatus = logStatusState.value
    val mergeTrackedBranches by viewModel.mergeTrackedBranchesInLog.collectAsState()
    var graphPadding by remember(viewModel) { mutableStateOf(viewModel.graphPadding) }

    LaunchedEffect(logStatus.verticalScrollState, logStatus.commitList) {
        launch {
            viewModel.focusCommit.collect { commit ->
                scrollToCommit(logStatus.verticalScrollState, logStatus.commitList, commit.commit)
            }
        }
        launch {
            viewModel.scrollToUncommittedChanges.collect {
                scrollToUncommittedChanges(logStatus.verticalScrollState, logStatus.commitList)
            }
        }
    }

    LaunchedEffect(selectedItem) {
        if (
            (selectedItem is SelectedItem.CommitBasedItem && selectedItem !is SelectedItem.CommitItem) ||
            (selectedItem is SelectedItem.CommitItem && selectedItem.scrollToItem)
        ) {
            scrollToCommit(logStatus.verticalScrollState, logStatus.commitList, selectedItem.commit)
        }
    }

    Box(modifier) {
        LogView(
            logState = logStatus,
            selectedItem = selectedItem,
            repositoryState = repositoryState,
            graphPadding = graphPadding,
            mergeTrackedBranches = mergeTrackedBranches,
            onRequestMoreLogItems = { firstVisibleItemIndex -> viewModel.loadMoreLogItems(firstVisibleItemIndex) },
            onCreateBranch = onCreateBranch,
            onResetBranch = onResetBranch,
            onCreateTag = onCreateTag,
            onChangeUpstreamBranch = onChangeUpstreamBranch,
            onRenameBranch = onRenameBranch,
            onGraphPaddingChange = { newGraphPadding ->
                graphPadding += newGraphPadding
                viewModel.graphPadding = graphPadding
            },
            onAction = { viewModel.onAction(it) },
            searchView = {
                SearchFilter(
                    logViewModel = viewModel,
                    searchFilterResults = it,
                    searchFocused = { viewModel.addSearchToCloseableView() },
                )
            }
        )

        AnimatedVisibility(visible = logStatus.isLoading) {
            AppProgressBar()
        }
    }
}

@Composable
private fun LogView(
    logState: LogState,
    selectedItem: SelectedItem,
    repositoryState: RepositoryState,
    graphPadding: Float,
    mergeTrackedBranches: Boolean,
    onRequestMoreLogItems: (Int) -> Unit,
    onCreateBranch: (Commit) -> Unit,
    onResetBranch: (Commit) -> Unit,
    onCreateTag: (Commit) -> Unit,
    onChangeUpstreamBranch: (Branch) -> Unit,
    onRenameBranch: (Branch) -> Unit,
    onGraphPaddingChange: (Float) -> Unit,
    onAction: (LogAction) -> Unit,
    searchView: @Composable (LogSearch.SearchResults) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val hasUncommittedChanges = logState.hasUncommittedChanges
    val commitList = logState.commitList

    val verticalScrollState = logState.verticalScrollState
    val horizontalScrollState = logState.horizontalScrollState
    val searchFilterValue = logState.searchFilter

    LaunchedEffect(verticalScrollState, logState) {
        snapshotFlow { verticalScrollState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect {
                val commitsList = logState.commitList

                // TODO Check what would happen with a repo with multiple starting commits
                if (
                    commitsList.commits.isNotEmpty() &&
                    commitsList.commits.count() - it < MIN_COMMITS_BEFORE_REQUESTING_MORE &&
                    commitsList.commits.entries.last().value.commit.parentCount > 0
                ) {
                    printLog(TAG, "Requesting more items")
                    onRequestMoreLogItems(verticalScrollState.firstVisibleItemIndex)
                }
            }
    }

    val selectedCommit = if (selectedItem is SelectedItem.CommitBasedItem) {
        selectedItem.commit
    } else {
        null
    }

    Column(
        modifier = Modifier
            .background(MaterialTheme.colors.background)
            .fillMaxSize()
    ) {
        var graphWidth = (CANVAS_DEFAULT_WIDTH + graphPadding).dp

        if (graphWidth.value < CANVAS_MIN_WIDTH) graphWidth = CANVAS_MIN_WIDTH.dp

        val maxLinePosition = if (commitList.isNotEmpty())
            commitList.maxLane
        else
            MIN_GRAPH_LANES

        var graphRealWidth = ((maxLinePosition + MARGIN_GRAPH_LANES) * LANE_WIDTH).dp

        // Using remember(graphRealWidth, graphWidth) makes the selected background color glitch when changing tabs
        if (graphRealWidth < graphWidth) {
            graphRealWidth = graphWidth
        }


        if (searchFilterValue is LogSearch.SearchResults) {
            searchView(searchFilterValue)
        }

        var branchFilter by remember { mutableStateOf<Branch?>(null) }
        var authorFilter by remember { mutableStateOf<String?>(null) }
        var dateFilter by remember { mutableStateOf(DateFilter.ANY) }

        val commitFilter = remember(branchFilter, authorFilter, dateFilter, commitList) {
            buildCommitFilter(commitList, branchFilter, authorFilter, dateFilter)
        }

        GraphHeader(
            branches = remember(logState.branches) {
                logState.branches.values.flatten().filter { it.isLocal }.distinctBy { it.simpleName }
            },
            authors = remember(commitList) {
                commitList.values.mapNotNull { it.author.name }.distinct().sorted()
            },
            branchFilter = branchFilter,
            authorFilter = authorFilter,
            dateFilter = dateFilter,
            onBranchFilter = { branchFilter = it },
            onAuthorFilter = { authorFilter = it },
            onDateFilter = { dateFilter = it },
            onShowSearch = {
                onAction(LogAction.SearchValueChange(""))
            }
        )

        Box {

            // This Box is only used to get a scroll state. With this scroll state we will manually add an offset in
            // the messages list
            Box(
                Modifier
                    .width(graphWidth)
                    .fillMaxHeight()
                    .horizontalScroll(horizontalScrollState)
                    .padding(bottom = 8.dp)
            ) {
                // The content has to be bigger in order to show the scroll bar in the parent component
                Box(
                    modifier = Modifier.width(graphRealWidth)
                )
            }

            CommitsList(
                scrollState = verticalScrollState,
                horizontalScrollState = horizontalScrollState,
                hasUncommittedChanges = hasUncommittedChanges,
                searchFilter = if (searchFilterValue is LogSearch.SearchResults) searchFilterValue.commits else null,
                commitFilter = commitFilter,
                selectedCommit = selectedCommit,
                logState = logState,
                repositoryState = repositoryState,
                selectedItem = selectedItem,
                commitList = commitList,
                branches = logState.branches,
                tags = logState.tags,
                stashes = logState.stashes,
                mergeTrackedBranches = mergeTrackedBranches,
                graphWidth = graphWidth,
                onCreateBranch = onCreateBranch,
                onResetBranch = onResetBranch,
                onCreateTag = onCreateTag,
                onChangeUpstreamBranch = onChangeUpstreamBranch,
                onRenameBranch = onRenameBranch,
                onAction = onAction,
            )

            val density = LocalDensity.current.density
            DividerLog(
                modifier = Modifier.draggable(
                    rememberDraggableState {
                        onGraphPaddingChange(it / density)
                    }, Orientation.Horizontal
                ),
                graphWidth = graphWidth,
            )


            // Scrollbar used to scroll horizontally the graph nodes
            // Added after every component to have the highest priority when clicking
            HorizontalScrollbar(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .width(graphWidth)
                    .padding(start = 4.dp, bottom = 4.dp), style = LocalScrollbarStyle.current.copy(
                    unhoverColor = MaterialTheme.colors.scrollbarNormal,
                    hoverColor = MaterialTheme.colors.scrollbarHover,
                ),
                adapter = rememberScrollbarAdapter(horizontalScrollState)
            )

            val isFirstItemVisible by remember(verticalScrollState) {
                derivedStateOf { verticalScrollState.firstVisibleItemIndex > 0 }
            }

            if (isFirstItemVisible) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                        .clip(RoundedCornerShape(50))
                        .handMouseClickable {
                            scope.launch {
                                verticalScrollState.scrollToItem(0)
                            }
                        }
                        .background(MaterialTheme.colors.primary)
                        .padding(vertical = 4.dp, horizontal = 8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painterResource(Res.drawable.align_top),
                            contentDescription = null,
                            tint = MaterialTheme.colors.onPrimary,
                            modifier = Modifier.size(20.dp),
                        )

                        Text(
                            text = "Scroll to top",
                            modifier = Modifier.padding(start = 8.dp),
                            color = MaterialTheme.colors.onPrimary,
                            maxLines = 1,
                            style = MaterialTheme.typography.body2,
                        )
                    }
                }
            }
        }
    }
}

suspend fun scrollToCommit(
    verticalScrollState: LazyListState,
    commitList: GraphCommits,
    commit: Commit,
) {
    val index = commitList.commits.entries.indexOfFirst { it.value.hash == commit?.hash }
    // TODO Show a message informing the user why we aren't scrolling
    // Index can be -1 if the ref points to a commit that is not shown in the graph due to the limited
    // number of displayed commits.
    val isCommitAlreadyVisible = verticalScrollState.layoutInfo.visibleItemsInfo.any {
        (it.key as? String).orEmpty().contains(commit.message)
    }

    if (!isCommitAlreadyVisible) {
        if (index >= 0) verticalScrollState.scrollToItem(index)
    }
}

suspend fun scrollToUncommittedChanges(
    verticalScrollState: LazyListState,
    commitList: GraphCommits,
) {
    if (commitList.isNotEmpty())
        verticalScrollState.scrollToItem(0)
}

@Composable
fun SearchFilter(
    logViewModel: RepositoryOpenViewModel,
    searchFilterResults: LogSearch.SearchResults,
    searchFocused: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var searchFilterText by remember { mutableStateOf(logViewModel.savedSearchFilter) }
    val textFieldFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        textFieldFocusRequester.requestFocus()
    }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .background(MaterialTheme.colors.secondarySurface)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val shape = MaterialTheme.shapes.small
            val fieldInteraction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            val isFocused by fieldInteraction.collectIsFocusedAsState()

            androidx.compose.foundation.text.BasicTextField(
                value = searchFilterText,
                onValueChange = {
                    searchFilterText = it
                    scope.launch {
                        logViewModel.onSearchValueChanged(it)
                    }
                },
                singleLine = true,
                interactionSource = fieldInteraction,
                textStyle = MaterialTheme.typography.body2.copy(color = MaterialTheme.colors.onBackground),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colors.primary),
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
                    .background(MaterialTheme.colors.background, shape)
                    .border(
                        1.dp,
                        if (isFocused) MaterialTheme.colors.primary else MaterialTheme.colors.onBackground.copy(alpha = 0.12f),
                        shape,
                    )
                    .focusRequester(textFieldFocusRequester)
                    .onFocusChanged {
                        if (it.isFocused) {
                            searchFocused()
                        }
                    }
                    .onPreviewKeyEvent { keyEvent ->
                        when {
                            keyEvent.matchesBinding(KeybindingOption.SIMPLE_ACCEPT) -> {
                                scope.launch {
                                    logViewModel.selectNextFilterCommit()
                                }
                                true
                            }

                            else -> false
                        }
                    },
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
                            if (searchFilterText.isEmpty()) {
                                Text(
                                    text = "Search by message, author name or commit ID",
                                    style = MaterialTheme.typography.body2,
                                    color = MaterialTheme.colors.onBackgroundSecondary,
                                    maxLines = 1,
                                )
                            }

                            innerTextField()
                        }

                        if (searchFilterText.isNotEmpty()) {
                            Text(
                                text = "${searchFilterResults.index}/${searchFilterResults.totalCount}",
                                fontFamily = monoTypography(),
                                style = MaterialTheme.typography.caption,
                                color = MaterialTheme.colors.onBackgroundSecondary,
                            )
                        }
                    }
                },
            )

            SearchBarIconButton(Res.drawable.dropdown, "Previous result", rotated = true) {
                scope.launch { logViewModel.selectPreviousFilterCommit() }
            }
            SearchBarIconButton(Res.drawable.dropdown, "Next result") {
                scope.launch { logViewModel.selectNextFilterCommit() }
            }
            SearchBarIconButton(Res.drawable.close, "Close search") { logViewModel.closeSearch() }
        }

        PanelDivider()
    }
}

@Composable
private fun SearchBarIconButton(
    icon: DrawableResource,
    description: String,
    rotated: Boolean = false,
    onClick: () -> Unit,
) {
    Icon(
        painter = painterResource(icon),
        contentDescription = description,
        tint = MaterialTheme.colors.onBackgroundSecondary,
        modifier = Modifier
            .size(28.dp)
            .clip(MaterialTheme.shapes.small)
            .handMouseClickable(onClick = onClick)
            .padding(6.dp)
            .rotate(if (rotated) 180f else 0f),
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CommitsList(
    scrollState: LazyListState,
    hasUncommittedChanges: Boolean,
    searchFilter: List<GraphCommit>?,
    commitFilter: ((GraphCommit) -> Boolean)?,
    selectedCommit: Commit?,
    logState: LogState,
    mergeTrackedBranches: Boolean,
    repositoryState: RepositoryState,
    selectedItem: SelectedItem,
    commitList: GraphCommits,
    branches: Map<String, List<Branch>>,
    tags: Map<String, List<Tag>>,
    stashes: HashSet<String>,
    onAction: (LogAction) -> Unit,
    onCreateBranch: (Commit) -> Unit,
    onResetBranch: (Commit) -> Unit,
    onCreateTag: (Commit) -> Unit,
    onChangeUpstreamBranch: (Branch) -> Unit,
    onRenameBranch: (Branch) -> Unit,
    graphWidth: Dp,
    horizontalScrollState: ScrollState,
) {
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboard.current

    scrollOnUncommittedChangesAppear(scrollState, hasUncommittedChanges)
    scrollOnNewCommitAdded(scrollState, commitList.commits)

    ScrollableLazyColumn(
        state = scrollState,
        modifier = Modifier
            .fillMaxSize()
            // The underlying composable assigned to the horizontal scroll bar won't be receiving the scroll events
            // because the commits list will consume the events, so this code tries to scroll manually when it detects
            // horizontal scrolling
            .onPointerEvent(PointerEventType.Scroll) { pointerEvent ->
                scope.launch {
                    val xScroll = pointerEvent.changes.map { it.scrollDelta.x }.sum()
                    horizontalScrollState.scrollBy(xScroll * HORIZONTAL_SCROLL_PIXELS_MULTIPLIER)
                }
            },
    ) {
        if (
            hasUncommittedChanges ||
            repositoryState.isMerging ||
            repositoryState.isRebasing ||
            repositoryState.isCherryPicking
        ) {
            item {
                Box(
                    modifier = Modifier.height(MaterialTheme.linesHeight.logCommitHeight)
                        .padding(horizontal = ROW_INSET.dp)
                        .clip(MaterialTheme.shapes.small)
                        .fillMaxWidth()
                        .handMouseClickable { onAction(LogAction.UncommittedChangesSelected) }
                ) {
                    UncommittedChangesGraphNode(
                        hasPreviousCommits = commitList.commits.isNotEmpty(),
                        isSelected = selectedItem is SelectedItem.UncommittedChanges,
                        modifier = Modifier.offset(-horizontalScrollState.value.dp)
                    )

                    UncommittedChangesLine(
                        graphWidth = graphWidth,
                        isSelected = selectedItem == SelectedItem.UncommittedChanges,
                        statusSummary = logState.statusSummary,
                        repositoryState = repositoryState,
                    )
                }
            }
        }

        // Setting a key makes the graph preserve the scroll position when a new line has been added on top (uncommitted changes)
        // Therefore, after popping a stash, the uncommitted changes wouldn't be visible and requires the user scrolling.
        // TODO This should be improved in case it's a dangling branch, shouldn't happen often but could be a thing
        items(
            items = commitList.values.toList(),
            key = { commit ->
                commit.hash + commit.lane + branches[commit.hash].orEmpty().joinToString() + tags[commit.hash].orEmpty()
                    .joinToString()
            },
        )
        { graphNode ->
            val isStash = stashes.contains(graphNode.hash)
            CommitLine(
                graphWidth = graphWidth,
                graphNode = graphNode,
                isSelected = selectedCommit?.hash == graphNode.hash,
                showInAmend = logState.currentBranch?.hash == graphNode.hash && !hasUncommittedChanges,
                isStash = isStash,
                mergeTrackedBranches = mergeTrackedBranches,
                branches = branches[graphNode.hash].orEmpty(),
                tags = tags[graphNode.hash].orEmpty(),
                currentBranch = logState.currentBranch,
                // Commits excluded by the header filters are dimmed, like the ones not matching the search
                matchesSearchFilter = if (commitFilter?.invoke(graphNode) == false) false else searchFilter?.contains(graphNode),
                horizontalScrollState = horizontalScrollState,
                showCreateNewBranch = { onCreateBranch(graphNode.commit) },
                showCreateNewTag = { onCreateTag(graphNode.commit) },
                resetBranch = { onResetBranch(graphNode.commit) },
                onMergeBranch = { onAction(LogAction.Merge(it)) },
                onDeleteBranch = { onAction(LogAction.DeleteBranch(it)) },
                onDeleteRemoteBranch = { onAction(LogAction.DeleteRemoteBranch(it)) },
                onCheckoutTag = { onAction(LogAction.CheckoutTag(it)) },
                onDeleteTag = { onAction(LogAction.DeleteTag(it)) },
                onPushToRemoteBranch = { onAction(LogAction.PushToRemoteBranch(it)) },
                onPullFromRemoteBranch = { onAction(LogAction.PullFromRemoteBranch(it)) },
                onRebaseBranch = { onAction(LogAction.Rebase(it)) },
                onRebaseInteractive = { onAction(LogAction.RebaseInteractive(graphNode.commit)) },
                onRevCommitSelected = { onAction(LogAction.CommitSelected(graphNode.commit, isStash)) },
                onChangeDefaultUpstreamBranch = { onChangeUpstreamBranch(it) },
                onRenameBranch = { onRenameBranch(it) },
                onDeleteStash = { onAction(LogAction.DeleteStash(graphNode.commit)) },
                onApplyStash = { onAction(LogAction.ApplyStash(graphNode.commit)) },
                onPopStash = { onAction(LogAction.PopStash(graphNode.commit)) },
                onShowStatusAmending = { onAction(LogAction.ShowStatusAmending) },
                onCheckoutCommit = { onAction(LogAction.CheckoutCommit(graphNode.commit)) },
                onRevertCommit = { onAction(LogAction.RevertCommit(graphNode.commit)) },
                onCherryPickCommit = { onAction(LogAction.CherryPickCommit(graphNode.commit)) },
                onCheckoutRemoteBranch = { onAction(LogAction.CheckoutRemoteBranch(it)) },
                onCheckoutBranch = { onAction(LogAction.CheckoutBranch(it)) },
                onCopyBranchNameToClipboard = {
                    scope.launch {
                        clipboard.setClipboardText(it.simpleName)
                    }
                },
            )
        }

        item {
            Box(modifier = Modifier.height(LOG_BOTTOM_PADDING.dp))
        }
    }
}

@Composable
private fun scrollOnUncommittedChangesAppear(
    scrollState: LazyListState,
    hasUncommittedChanges: Boolean
) {
    // On scrollstate/tab change this is reset to prevent triggering a scroll in other tabs
    val isFirst = remember(scrollState) { booleanArrayOf(true) }

    LaunchedEffect(scrollState, hasUncommittedChanges) {
        if (isFirst[0]) {
            isFirst[0] = false
            return@LaunchedEffect
        }

        if (hasUncommittedChanges && scrollState.firstVisibleItemIndex == 1) {
            scrollState.scrollToItem(0)
        }
    }
}

@Composable
private fun scrollOnNewCommitAdded(
    scrollState: LazyListState,
    commits: LinkedHashMap<String, GraphCommit>,
) {
    // On scrollstate/tab change this is reset to prevent triggering a scroll in other tabs
    val isFirst = remember(scrollState) { booleanArrayOf(true) }

    LaunchedEffect(scrollState, commits.keys.firstOrNull()) {
        if (isFirst[0]) {
            isFirst[0] = false
            return@LaunchedEffect
        }

        scrollState.scrollToItem(0)
    }
}


enum class DateFilter(val label: String, val seconds: Long?) {
    ANY("Any date", null),
    TODAY("Last 24 hours", 24 * 3600L),
    WEEK("Last 7 days", 7 * 24 * 3600L),
    MONTH("Last 30 days", 30 * 24 * 3600L),
}

/**
 * Predicate telling which commits match the header filters, or null when no filter is active.
 */
private fun buildCommitFilter(
    commitList: GraphCommits,
    branch: Branch?,
    author: String?,
    date: DateFilter,
): ((GraphCommit) -> Boolean)? {
    if (branch == null && author == null && date == DateFilter.ANY) return null

    // Commits reachable from the branch, only the loaded ones can be walked
    val reachable: Set<String>? = branch?.let {
        val visited = HashSet<String>()
        val pending = ArrayDeque(listOf(it.hash))

        while (pending.isNotEmpty()) {
            val hash = pending.removeLast()

            if (visited.add(hash)) {
                commitList.commits[hash]?.commit?.parentsHashes?.let(pending::addAll)
            }
        }

        visited
    }
    val minDate = date.seconds?.let { System.currentTimeMillis() / 1000 - it }

    return { node ->
        (reachable == null || node.hash in reachable) &&
                (author == null || node.author.name == author) &&
                (minDate == null || node.date >= minDate)
    }
}

@Composable
fun GraphHeader(
    branches: List<Branch>,
    authors: List<String>,
    branchFilter: Branch?,
    authorFilter: String?,
    dateFilter: DateFilter,
    onBranchFilter: (Branch?) -> Unit,
    onAuthorFilter: (String?) -> Unit,
    onDateFilter: (DateFilter) -> Unit,
    onShowSearch: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .padding(start = 14.dp, end = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterPill(
                label = branchFilter?.simpleName ?: "All branches",
                isActive = branchFilter != null,
                items = {
                    listOf(
                        ContextMenuElement.ContextTextEntry(label = "All branches", onClick = { onBranchFilter(null) })
                    ) + branches.map { branch ->
                        ContextMenuElement.ContextTextEntry(label = branch.simpleName, onClick = { onBranchFilter(branch) })
                    }
                },
            )
            FilterPill(
                label = authorFilter ?: "Author",
                isActive = authorFilter != null,
                items = {
                    listOf(
                        ContextMenuElement.ContextTextEntry(label = "Any author", onClick = { onAuthorFilter(null) })
                    ) + authors.map { author ->
                        ContextMenuElement.ContextTextEntry(label = author, onClick = { onAuthorFilter(author) })
                    }
                },
            )
            FilterPill(
                label = if (dateFilter == DateFilter.ANY) "Date" else dateFilter.label,
                isActive = dateFilter != DateFilter.ANY,
                items = {
                    DateFilter.entries.map { option ->
                        ContextMenuElement.ContextTextEntry(label = option.label, onClick = { onDateFilter(option) })
                    }
                },
            )

            Spacer(modifier = Modifier.weight(1f))

            InstantTooltip(text = "Search commits") {
                Icon(
                    painterResource(Res.drawable.search),
                    modifier = Modifier
                        .size(28.dp)
                        .clip(MaterialTheme.shapes.small)
                        .handMouseClickable { onShowSearch() }
                        .padding(6.dp),
                    contentDescription = "Search commits",
                    tint = MaterialTheme.colors.onBackgroundSecondary,
                )
            }
        }

        PanelDivider()
    }
}

@Composable
private fun FilterPill(
    label: String,
    isActive: Boolean,
    items: () -> List<ContextMenuElement>,
) {
    DropDownMenu(items = items, showIcons = false) {
        Row(
            modifier = Modifier
                .height(28.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(
                    if (isActive) MaterialTheme.colors.primary.copy(alpha = 0.16f) else Color.Transparent
                )
                .border(
                    1.dp,
                    if (isActive) MaterialTheme.colors.primary.copy(alpha = 0.5f)
                    else MaterialTheme.colors.onBackground.copy(alpha = 0.12f),
                    RoundedCornerShape(percent = 50),
                )
                // The dropdown wrapping the pill handles the click, a clickable here would swallow it
                .handOnHover()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.body2,
                fontWeight = FontWeight.Medium,
                color = if (isActive) MaterialTheme.colors.primaryVariant else MaterialTheme.colors.onBackgroundSecondary,
                maxLines = 1,
            )
            Icon(
                painter = painterResource(Res.drawable.dropdown),
                contentDescription = null,
                tint = MaterialTheme.colors.onBackgroundSecondary,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
fun UncommittedChangesLine(
    graphWidth: Dp,
    isSelected: Boolean,
    repositoryState: RepositoryState,
    statusSummary: StatusSummary,
) {
    Row(
        modifier = Modifier
            .height(MaterialTheme.linesHeight.logCommitHeight)
            .padding(start = graphWidth)
            .backgroundIf(isSelected, MaterialTheme.colors.backgroundSelected)
            .padding(DIVIDER_WIDTH.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val text = when {
            repositoryState.isRebasing -> "Pending changes to rebase"
            repositoryState.isMerging -> "Pending changes to merge"
            repositoryState.isCherryPicking -> "Pending changes to cherry-pick"
            repositoryState.isReverting -> "Pending changes to revert"
            else -> "Uncommitted changes"
        }

        Text(
            text = text,
            fontStyle = FontStyle.Italic,
            modifier = Modifier.padding(start = 16.dp),
            style = MaterialTheme.typography.body2,
            maxLines = 1,
            color = MaterialTheme.colors.onBackgroundSecondary,
        )

        Spacer(modifier = Modifier.weight(1f))

        LogStatusSummary(
            statusSummary = statusSummary,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
    }
}

@Composable
fun LogStatusSummary(statusSummary: StatusSummary, modifier: Modifier) {
    Row(
        modifier = modifier,
    ) {
        if (statusSummary.modifiedCount > 0) {
            SummaryEntry(
                count = statusSummary.modifiedCount,
                letter = "M",
                color = MaterialTheme.colors.modifyFile,
            )
        }

        if (statusSummary.addedCount > 0) {
            SummaryEntry(
                count = statusSummary.addedCount,
                letter = "A",
                color = MaterialTheme.colors.addFile,
            )
        }

        if (statusSummary.deletedCount > 0) {
            SummaryEntry(
                count = statusSummary.deletedCount,
                letter = "D",
                color = MaterialTheme.colors.deleteFile,
            )
        }

        if (statusSummary.conflictingCount > 0) {
            SummaryEntry(
                count = statusSummary.conflictingCount,
                letter = "U",
                color = MaterialTheme.colors.conflictFile,
            )
        }
    }
}

@Composable
fun SummaryEntry(
    count: Int, letter: String, color: Color,
) {
    Row(
        modifier = Modifier.padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = letter,
            fontFamily = monoTypography(),
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.caption,
            color = color,
        )

        Text(
            text = count.toString(),
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onBackground,
        )
    }
}

@Composable
private fun CommitLine(
    graphWidth: Dp,
    graphNode: GraphCommit,
    isSelected: Boolean,
    currentBranch: Branch?,
    isStash: Boolean,
    mergeTrackedBranches: Boolean,
    showInAmend: Boolean,
    matchesSearchFilter: Boolean?,
    showCreateNewBranch: () -> Unit,
    showCreateNewTag: () -> Unit,
    resetBranch: () -> Unit,
    onApplyStash: () -> Unit,
    onPopStash: () -> Unit,
    onDeleteStash: () -> Unit,
    onMergeBranch: (Branch) -> Unit,
    onDeleteBranch: (Branch) -> Unit,
    onDeleteRemoteBranch: (Branch) -> Unit,
    onCheckoutTag: (Tag) -> Unit,
    onDeleteTag: (Tag) -> Unit,
    onPushToRemoteBranch: (Branch) -> Unit,
    onPullFromRemoteBranch: (Branch) -> Unit,
    onRebaseBranch: (Branch) -> Unit,
    onRevCommitSelected: () -> Unit,
    onRebaseInteractive: () -> Unit,
    onShowStatusAmending: () -> Unit,
    onCheckoutCommit: () -> Unit,
    onRevertCommit: () -> Unit,
    onCherryPickCommit: () -> Unit,
    onCheckoutRemoteBranch: (Branch) -> Unit,
    onCheckoutBranch: (Branch) -> Unit,
    onChangeDefaultUpstreamBranch: (Branch) -> Unit,
    onRenameBranch: (Branch) -> Unit,
    onCopyBranchNameToClipboard: (Branch) -> Unit,
    horizontalScrollState: ScrollState,
    branches: List<Branch>,
    tags: List<Tag>,
) {
    val isLastCommitOfCurrentBranch = currentBranch?.hash == graphNode.hash

    ContextMenu(
        items = {
            if (isStash) {
                stashesContextMenuItems(
                    onApply = onApplyStash,
                    onPop = onPopStash,
                    onDelete = onDeleteStash,
                )
            } else {
                logContextMenu(
                    onShowStatusAmending = onShowStatusAmending,
                    onCheckoutCommit = onCheckoutCommit,
                    onCreateNewBranch = showCreateNewBranch,
                    onCreateNewTag = showCreateNewTag,
                    onRevertCommit = onRevertCommit,
                    onCherryPickCommit = onCherryPickCommit,
                    onRebaseInteractive = onRebaseInteractive,
                    onResetBranch = { resetBranch() },
                    isLastCommit = isLastCommitOfCurrentBranch,
                    showInAmend = showInAmend,
                )
            }
        },
    ) {
        Box(
            modifier = Modifier
                .height(MaterialTheme.linesHeight.logCommitHeight)
                .padding(horizontal = ROW_INSET.dp)
                .clip(MaterialTheme.shapes.small)
                .handMouseClickable { onRevCommitSelected() }
        ) {
            val colors = MaterialTheme.colors.graphColors
            val nodeColor = colors[graphNode.lane % colors.size]

            Box {
                Row(
                    modifier = Modifier
                        .clipToBounds()
                        .fillMaxHeight()
                        .fillMaxWidth()
                        .offset(-horizontalScrollState.value.dp)
                ) {
                    CommitsGraph(
                        modifier = Modifier
                            .fillMaxHeight(),
                        plotCommit = graphNode,
                        isStash = isStash,
                        nodeColor = nodeColor,
                        isSelected = isSelected,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .padding(start = graphWidth)
                    .fillMaxHeight()
                    .background(MaterialTheme.colors.background)
                    .backgroundIf(isSelected, MaterialTheme.colors.backgroundSelected)
            ) {

                if (matchesSearchFilter == true) {
                    Box(
                        modifier = Modifier
                            .padding(start = DIVIDER_WIDTH.dp)
                            .background(MaterialTheme.colors.secondary)
                            .fillMaxHeight()
                            .width(4.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 4.dp),
                ) {
                    CommitMessage(
                        graphCommit = graphNode,
                        nodeColor = nodeColor,
                        matchesSearchFilter = matchesSearchFilter,
                        currentBranch = currentBranch,
                        isStash = isStash,
                        mergeTrackedBranches = mergeTrackedBranches,
                        branches = branches,
                        tags = tags,
                        onCheckoutBranch = { ref ->
                            if (ref.isRemote) {
                                onCheckoutRemoteBranch(ref)
                            } else {
                                onCheckoutBranch(ref)
                            }
                        },
                        onMergeBranch = onMergeBranch,
                        onDeleteBranch = onDeleteBranch,
                        onDeleteRemoteBranch = onDeleteRemoteBranch,
                        onCheckoutTag = onCheckoutTag,
                        onDeleteTag = onDeleteTag,
                        onRebaseBranch = onRebaseBranch,
                        onPushRemoteBranch = onPushToRemoteBranch,
                        onPullRemoteBranch = onPullFromRemoteBranch,
                        onChangeDefaultUpstreamBranch = onChangeDefaultUpstreamBranch,
                        onRenameBranch = onRenameBranch,
                        onCopyBranchNameToClipboard = onCopyBranchNameToClipboard,
                    )
                }
            }
        }
    }
}

@Composable
fun CommitMessage(
    graphCommit: GraphCommit,
    currentBranch: Branch?,
    isStash: Boolean,
    mergeTrackedBranches: Boolean,
    tags: List<Tag>,
    branches: List<Branch>,
    nodeColor: Color,
    matchesSearchFilter: Boolean?,
    onCheckoutBranch: (ref: Branch) -> Unit,
    onMergeBranch: (ref: Branch) -> Unit,
    onDeleteBranch: (ref: Branch) -> Unit,
    onDeleteRemoteBranch: (ref: Branch) -> Unit,
    onRebaseBranch: (ref: Branch) -> Unit,
    onCheckoutTag: (tag: Tag) -> Unit,
    onDeleteTag: (tag: Tag) -> Unit,
    onPushRemoteBranch: (ref: Branch) -> Unit,
    onPullRemoteBranch: (ref: Branch) -> Unit,
    onChangeDefaultUpstreamBranch: (ref: Branch) -> Unit,
    onRenameBranch: (ref: Branch) -> Unit,
    onCopyBranchNameToClipboard: (ref: Branch) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxSize()
            .hoverable(
                // This modifier is added just to prevent committer tooltip is shown then it is underneath this message
                remember { MutableInteractionSource() },
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp)
        ) {
            if (!isStash) {
                // TODO Enable this once commits list is migrated to new structure
                for (tag in tags) {
                    TagChip(
                        tag = tag,
                        color = nodeColor,
                        onCheckoutTag = { onCheckoutTag(tag) },
                        onDeleteTag = { onDeleteTag(tag) },
                    )
                }

                val branchesToSkip = mutableListOf<Branch>()

                for (branch in branches) {
                    if (branchesToSkip.contains(branch)) {
                        continue
                    }

                    val trackingBranch = branch.trackingBranch
                    val trackingRemoteBranch = if (mergeTrackedBranches && branch.matchesTrackingBranchName && trackingBranch != null) {
                        val remoteBranchInSameCommit = branches.firstOrNull {
                            it.isRemote && it.remoteName == trackingBranch.remote &&
                                    it.simpleName == trackingBranch.branch
                        }

                        if (remoteBranchInSameCommit != null) {
                            branchesToSkip.add(remoteBranchInSameCommit)
                            remoteBranchInSameCommit
                        } else {
                            null
                        }
                    } else {
                        null
                    }

                    BranchChip(
                        branch = branch,
                        trackingRemoteBranch = trackingRemoteBranch,
                        color = nodeColor,
                        currentBranch = currentBranch,
                        isCurrentBranch = branch.isSameBranch(currentBranch),
                        onCheckoutBranch = { onCheckoutBranch(it) },
                        onMergeBranch = { onMergeBranch(it) },
                        onDeleteBranch = { onDeleteBranch(it) },
                        onDeleteRemoteBranch = { onDeleteRemoteBranch(it) },
                        onRebaseBranch = { onRebaseBranch(it) },
                        onPullRemoteBranch = { onPullRemoteBranch(it) },
                        onPushRemoteBranch = { onPushRemoteBranch(it) },
                        onChangeDefaultUpstreamBranch = { onChangeDefaultUpstreamBranch(it) },
                        onRenameBranch = { onRenameBranch(it) },
                        onCopyBranchNameToClipboard = { onCopyBranchNameToClipboard(it) },
                    )
                }
            }
        }

        val message = remember(graphCommit.hash) {
            graphCommit.commit.shortMessage
        }

        Text(
            text = message,
            modifier = Modifier
                .padding(start = 8.dp, end = 12.dp)
                .weight(1f),
            style = MaterialTheme.typography.body2,
            color = if (matchesSearchFilter == false) MaterialTheme.colors.onBackgroundSecondary else MaterialTheme.colors.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Text(
            text = graphCommit.commit.author.name.orEmpty(),
            modifier = Modifier.width(LOG_AUTHOR_COLUMN_WIDTH),
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onBackgroundSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        InstantTooltip(
            text = graphCommit.date.toSmartSystemString(allowRelative = false, showTime = true),
            modifier = Modifier.width(LOG_DATE_COLUMN_WIDTH),
            position = InstantTooltipPosition.RIGHT,
        ) {
            Text(
                text = graphCommit.date.toSmartSystemString(),
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onBackgroundSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Text(
            text = graphCommit.commit.shortHash,
            modifier = Modifier.width(LOG_HASH_COLUMN_WIDTH),
            style = MaterialTheme.typography.caption,
            fontFamily = monoTypography(),
            color = MaterialTheme.colors.onBackgroundSecondary,
            maxLines = 1,
        )
    }
}

private val LOG_AUTHOR_COLUMN_WIDTH = 120.dp
private val LOG_DATE_COLUMN_WIDTH = 96.dp
private val LOG_HASH_COLUMN_WIDTH = 76.dp

@Composable
fun DividerLog(modifier: Modifier, graphWidth: Dp) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    // Only shown while hovered, to tell the graph width can be dragged
    Box(
        modifier = Modifier
            .padding(start = graphWidth)
            .width(DIVIDER_WIDTH.dp)
            .then(modifier)
            .hoverable(interactionSource)
            .pointerHoverIcon(resizePointerIconEast)
    ) {
        if (isHovered) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(2.dp)
                    .background(color = MaterialTheme.colors.onBackground.copy(alpha = 0.2F))
                    .align(Alignment.Center)
            )
        }
    }
}


@Composable
fun CommitsGraph(
    modifier: Modifier = Modifier,
    plotCommit: GraphCommit,
    isStash: Boolean,
    nodeColor: Color,
    isSelected: Boolean,
) {
    val passingLanes = plotCommit.passingLanes
    val forkingOffLanes = plotCommit.forkingOffLanes
    val mergingLanes = plotCommit.mergingLanes
    val colors = MaterialTheme.colors.graphColors
    val density = LocalDensity.current.density
    val laneWidthWithDensity = remember(density) {
        LANE_WIDTH * density
    }

    Box(
        modifier = modifier
            .backgroundIf(isSelected, MaterialTheme.colors.backgroundSelected)
            .fillMaxHeight(),
        contentAlignment = Alignment.CenterStart,
    ) {

        val itemPosition = plotCommit.lane
        val arcsAngleMultiplier = 0.50F

        val hasMergeAndForkOff = forkingOffLanes.isNotEmpty() && mergingLanes.isNotEmpty()

        val laneHeightModifier = if (hasMergeAndForkOff) {
            2f
        } else {
            0f
        }

        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            clipRect {
                if (plotCommit.childCount > 0) {
                    drawLine(
                        color = colors[itemPosition % colors.size],
                        start = Offset(laneWidthWithDensity * (itemPosition + 1), this.center.y),
                        end = Offset(laneWidthWithDensity * (itemPosition + 1), 0f),
                        strokeWidth = 2f * density,
                    )
                }

                forkingOffLanes.forEach { plotLane ->

                    val x1 = laneWidthWithDensity * (itemPosition + 1)
                    val x2 = laneWidthWithDensity * (plotLane + 1) - (laneWidthWithDensity * arcsAngleMultiplier)
                    val x3 = laneWidthWithDensity * (plotLane + 1)
                    val y1 = this@clipRect.center.y - (laneHeightModifier * density)
                    val y2 = this@clipRect.center.y - (laneWidthWithDensity * arcsAngleMultiplier)
                    val y3 = 0F
                    val startAngle = 90F
                    val sweepAngleDegrees = -90F

                    val arcRect = Rect(x2, y2, x3, y1)

                    graphArc(x1, y1, x2, arcRect, startAngle, sweepAngleDegrees, x3, y3, colors[plotLane % colors.size], density)
                }

                mergingLanes.forEach { plotLane ->
                    val direction = if (plotLane < itemPosition) {
                        ARC_DIRECTION_LEFT
                    } else {
                        ARC_DIRECTION_RIGHT
                    }

                    val x1 = laneWidthWithDensity * (itemPosition + 1)
                    val x2 =
                        laneWidthWithDensity * (plotLane + 1) + (laneWidthWithDensity * arcsAngleMultiplier * direction * -1)
                    val x3 = laneWidthWithDensity * (plotLane + 1)
                    val y1 = this@clipRect.center.y + (laneHeightModifier * density)
                    val y2 = this@clipRect.center.y + (laneWidthWithDensity * arcsAngleMultiplier)
                    val y3 = this@clipRect.size.height
                    val startAngle = 270F
                    val sweepAngleDegrees = 90F * direction

                    val arcRect = if (plotLane < itemPosition) {
                        Rect(x3, y1, x2, y2)
                    } else {
                        Rect(x2, y1, x3, y2)
                    }

                    graphArc(x1, y1, x2, arcRect, startAngle, sweepAngleDegrees, x3, y3, colors[plotLane % colors.size], density)
                }

                if (plotCommit.commit.parentCount > 0) {
                    drawLine(
                        color = colors[itemPosition % colors.size],
                        start = Offset(laneWidthWithDensity * (itemPosition + 1), this.center.y),
                        end = Offset(laneWidthWithDensity * (itemPosition + 1), this.size.height),
                        strokeWidth = 2f * density,
                    )
                }

                passingLanes.forEach { plotLane ->
                    drawLine(
                        color = colors[plotLane % colors.size],
                        start = Offset(laneWidthWithDensity * (plotLane + 1), 0f),
                        end = Offset(laneWidthWithDensity * (plotLane + 1), this.size.height),
                        strokeWidth = 2f * density,
                    )
                }
            }
        }

        CommitNode(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = ((itemPosition + 1) * LANE_WIDTH - NODE_SIZE / 2f).dp),
            author = plotCommit.author,
            isMerge = plotCommit.commit.parentCount > 1,
            isStash = isStash,
            color = nodeColor,
        )
    }
}

private fun DrawScope.graphArc(
    x1: Float,
    y1: Float,
    x2: Float,
    arcRect: Rect,
    startAngle: Float,
    sweepAngleDegrees: Float,
    x3: Float,
    y3: Float,
    color: Color,
    density: Float
) {
    val path = Path().apply {
        moveTo(x1, y1)
        lineTo(x2, y1)
        arcTo(
            rect = arcRect,
            startAngleDegrees = startAngle,
            sweepAngleDegrees = sweepAngleDegrees,
            forceMoveTo = false
        )
        lineTo(x3, y3)
    }

    drawPath(
        path = path,
        color = color,
        style = Stroke(width = 2f * density)
    )
}

@Composable
fun CommitNode(
    modifier: Modifier = Modifier,
    author: Identity,
    isMerge: Boolean,
    isStash: Boolean,
    color: Color,
) {
    if (isStash) {
        // Hollow node, so a stash stands apart from the commits of its lane
        Box(
            modifier = modifier
                .size(NODE_SIZE.dp)
                .border(2.dp, color, shape = CircleShape)
                .clip(CircleShape)
                .background(MaterialTheme.colors.background),
        )
    } else {
        val shape = if (isMerge) {
            RoundedCornerShape(3.dp)
        } else {
            CircleShape
        }

        InstantTooltip(
            "${author.name} <${author.email}>",
            position = InstantTooltipPosition.RIGHT,
        ) {
            Box(
                modifier = modifier
                    .size(NODE_SIZE.dp)
                    .clip(shape)
                    .background(color)
            )
        }
    }
}

@Composable
fun UncommittedChangesGraphNode(
    modifier: Modifier = Modifier,
    hasPreviousCommits: Boolean,
    isSelected: Boolean,
) {
    val colors = MaterialTheme.colors.graphColors
    val density = LocalDensity.current.density

    val laneWidthWithDensity = remember(density) {
        LANE_WIDTH * density
    }
    Box(
        modifier = modifier
            .backgroundIf(isSelected, MaterialTheme.colors.backgroundSelected)
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            clipRect {
                val radius = NODE_SIZE / 2f * density

                if (hasPreviousCommits) drawLine(
                    color = colors[0],
                    start = Offset(laneWidthWithDensity, this.center.y + radius),
                    end = Offset(laneWidthWithDensity, this.size.height),
                    strokeWidth = 2f * density,
                )

                // Dashed ring: these changes are not a commit yet
                drawCircle(
                    color = colors[0],
                    radius = radius,
                    center = Offset(laneWidthWithDensity, this.center.y),
                    style = Stroke(
                        width = 2f * density,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f * density, 2f * density)),
                    ),
                )
            }
        }
    }
}

@Composable
fun BranchChip(
    modifier: Modifier = Modifier,
    trackingRemoteBranch: Branch?,
    isCurrentBranch: Boolean = false,
    branch: Branch,
    currentBranch: Branch?,
    onCheckoutBranch: (Branch) -> Unit,
    onMergeBranch: (Branch) -> Unit,
    onDeleteBranch: (Branch) -> Unit,
    onDeleteRemoteBranch: (Branch) -> Unit,
    onRebaseBranch: (Branch) -> Unit,
    onPushRemoteBranch: (Branch) -> Unit,
    onPullRemoteBranch: (Branch) -> Unit,
    onChangeDefaultUpstreamBranch: (Branch) -> Unit,
    onCopyBranchNameToClipboard: (Branch) -> Unit,
    onRenameBranch: (Branch) -> Unit,
    color: Color,
) {
    val contextMenuItemsList = {
        branchContextMenuItems(
            branch = branch,
            currentBranch = currentBranch,
            isCurrentBranch = isCurrentBranch,
            isLocal = branch.isLocal,
            onCheckoutBranch = onCheckoutBranch,
            onMergeBranch = onMergeBranch,
            onDeleteBranch = onDeleteBranch,
            onDeleteRemoteBranch = onDeleteRemoteBranch,
            onRebaseBranch = onRebaseBranch,
            onPushToRemoteBranch = onPushRemoteBranch,
            onPullFromRemoteBranch = onPullRemoteBranch,
            onChangeDefaultUpstreamBranch = onChangeDefaultUpstreamBranch,
            onRenameBranch = onRenameBranch,
            onCopyBranchNameToClipboard = onCopyBranchNameToClipboard,
        )
    }
    val remoteContextMenuItemsList = {
        if (trackingRemoteBranch != null) {
            branchContextMenuItems(
                branch = trackingRemoteBranch,
                currentBranch = currentBranch,
                isCurrentBranch = isCurrentBranch,
                isLocal = false,
                onCheckoutBranch = onCheckoutBranch,
                onMergeBranch = onMergeBranch,
                onDeleteBranch = onDeleteBranch,
                onDeleteRemoteBranch = onDeleteRemoteBranch,
                onRebaseBranch = onRebaseBranch,
                onPushToRemoteBranch = onPushRemoteBranch,
                onPullFromRemoteBranch = onPullRemoteBranch,
                onChangeDefaultUpstreamBranch = onChangeDefaultUpstreamBranch,
                onRenameBranch = onRenameBranch,
                onCopyBranchNameToClipboard = onCopyBranchNameToClipboard,
            )
        } else {
            emptyList()
        }
    }

    val endingContent: @Composable RowScope.() -> Unit = {
        val sizeMultiplierCurrentBranch by animateFloatAsState(if (isCurrentBranch) 1f else 0f)
        val sizeMultiplierTrackingRemoteBranch by animateFloatAsState(if (trackingRemoteBranch != null) 1f else 0f)

        Icon(
            painter = painterResource(Res.drawable.location),
            contentDescription = null,
            modifier = Modifier
                .padding(start = (4 * sizeMultiplierCurrentBranch).dp)
                .size((14 * sizeMultiplierCurrentBranch).dp),
            tint = MaterialTheme.colors.primaryVariant,
        )

        if (trackingRemoteBranch != null) {
            val tooltipText = buildAnnotatedString {
                val branchNameStyle = SpanStyle(fontWeight = FontWeight.SemiBold)
                val defaultStyle = SpanStyle(fontWeight = FontWeight.Normal)

                this.pushStyle(style = defaultStyle)
                this.append("Local branch ")

                this.pushStyle(style = branchNameStyle)
                this.append(branch.simpleName)

                this.pushStyle(style = SpanStyle(fontWeight = FontWeight.Normal))
                this.append(" is up to date with remote branch ")

                this.pushStyle(style = branchNameStyle)
                this.append(trackingRemoteBranch.simpleNameWithRemote)
            }

            InstantTooltip(
                text = tooltipText,
                position = InstantTooltipPosition.RIGHT,
            ) {
                ContextMenu(
                    items = remoteContextMenuItemsList,
                ) {
                    Icon(
                        modifier = Modifier
                            .padding(start = (5 * sizeMultiplierTrackingRemoteBranch).dp)
                            .size((14 * sizeMultiplierTrackingRemoteBranch).dp),
                        painter = painterResource(Res.drawable.cloud),
                        contentDescription = null,
                        tint = MaterialTheme.colors.onBackgroundSecondary,
                    )
                }
            }
        }
    }

    Chip(
        modifier = modifier.draggable(
            rememberDraggableState {

            },
            orientation = Orientation.Vertical,
        ),
        color = when {
            branch.isRemote -> MaterialTheme.colors.onBackgroundSecondary
            isCurrentBranch -> MaterialTheme.colors.graphColors[1]
            else -> MaterialTheme.colors.primary
        },
        isOutlined = branch.isRemote,
        text = branch.logName,
        icon = if (branch.isLocal) Res.drawable.branch else Res.drawable.cloud,
        onCheckoutRef = { onCheckoutBranch(branch) },
        contextMenuItemsList = contextMenuItemsList,
        endingContent = endingContent,
    )
}

val Branch.logName: String
    get() = when {
        this.name == Constants.HEAD -> {
            this.name
        }

        this.isRemote -> {
            name.replace("refs/remotes/", "")
        }

        else -> {
            val split = this.name.split("/")
            split.takeLast(split.size - LOCAL_PREFIX_LENGTH).joinToString("/")
        }
    }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TagChip(
    modifier: Modifier = Modifier,
    tag: Tag,
    onCheckoutTag: () -> Unit,
    onDeleteTag: () -> Unit,
    color: Color,
) {
    val contextMenuItemsList = {
        tagContextMenuItems(
            onCheckoutTag = onCheckoutTag,
            onDeleteTag = onDeleteTag,
        )
    }

    Chip(
        modifier,
        tag.simpleName,
        Res.drawable.tag,
        onCheckoutRef = onCheckoutTag,
        contextMenuItemsList = contextMenuItemsList,
        color = MaterialTheme.colors.secondary,
    )
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun Chip(
    modifier: Modifier = Modifier,
    text: String,
    icon: DrawableResource,
    color: Color,
    isOutlined: Boolean = false,
    onCheckoutRef: () -> Unit,
    contextMenuItemsList: () -> List<ContextMenuElement>,
    endingContent: @Composable RowScope.() -> Unit = {},
) {
    val shape = RoundedCornerShape(6.dp)

    // Tinted by the kind of ref, as in the mockup: tags, checked out branch, other local branches, remote branches
    val accent = color
    val contentColor = if (isOutlined || MaterialTheme.colors.isLight) accent else lerp(accent, Color.White, 0.3f)
    val chipBackground = if (isOutlined) MaterialTheme.colors.onBackground.copy(alpha = 0.05f) else accent.copy(alpha = 0.14f)
    val chipBorder = if (isOutlined) MaterialTheme.colors.onBackground.copy(alpha = 0.14f) else accent.copy(alpha = 0.4f)

    Box(
        modifier = Modifier
            .padding(horizontal = 3.dp)
            .clip(shape)
            .background(chipBackground)
            .border(width = 1.dp, color = chipBorder, shape = shape)
            .combinedClickable(onDoubleClick = onCheckoutRef, onClick = {})
            .handOnHover()
    ) {
        ContextMenu(
            items = contextMenuItemsList
        ) {
            Row(
                modifier = modifier
                    .height(22.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    modifier = Modifier.size(12.dp),
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = contentColor,
                )
                Text(
                    text = text,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = contentColor,
                    maxLines = 1,
                    modifier = Modifier.padding(start = 5.dp)
                )

                endingContent()
            }
        }
    }
}