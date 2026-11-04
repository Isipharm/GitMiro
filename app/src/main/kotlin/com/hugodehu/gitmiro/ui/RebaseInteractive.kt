package com.hugodehu.gitmiro.ui

import com.hugodehu.gitmiro.extensions.handOnHover
import com.hugodehu.gitmiro.ui.components.AppSpinner
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hugodehu.gitmiro.app.generated.resources.*
import com.hugodehu.gitmiro.domain.models.RebaseInteractiveState
import com.hugodehu.gitmiro.domain.models.RebaseLine
import com.hugodehu.gitmiro.domain.models.ui.SelectedItem
import com.hugodehu.gitmiro.domain.repositories.DataState
import com.hugodehu.gitmiro.extensions.backgroundIf
import com.hugodehu.gitmiro.repositoryopen.RepositoryOpenViewModel
import com.hugodehu.gitmiro.theme.backgroundSelected
import com.hugodehu.gitmiro.theme.onBackgroundSecondary
import com.hugodehu.gitmiro.ui.components.AdjustableOutlinedTextField
import com.hugodehu.gitmiro.ui.components.PrimaryButton
import com.hugodehu.gitmiro.ui.components.GhostButton
import com.hugodehu.gitmiro.ui.components.PanelDivider
import com.hugodehu.gitmiro.theme.addFile
import com.hugodehu.gitmiro.theme.graphColors
import com.hugodehu.gitmiro.theme.monoTypography
import com.hugodehu.gitmiro.theme.secondarySurface
import com.hugodehu.gitmiro.extensions.handMouseClickable
import com.hugodehu.gitmiro.ui.context_menu.DropDownMenu
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import com.hugodehu.gitmiro.ui.components.ScrollableLazyColumn
import com.hugodehu.gitmiro.ui.context_menu.ContextMenuElement
import com.hugodehu.gitmiro.ui.context_menu.DropDownMenu
import com.hugodehu.gitmiro.ui.context_menu.DropDownMenuText
import com.hugodehu.gitmiro.ui.drag_sorting.VerticalDraggableItem
import com.hugodehu.gitmiro.ui.drag_sorting.rememberVerticalDragDropState
import com.hugodehu.gitmiro.ui.drag_sorting.verticalDragContainer
import com.hugodehu.gitmiro.viewmodels.RebaseAction
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun RebaseInteractive(
    viewModel: RepositoryOpenViewModel,
    state: RebaseInteractiveState,
) {
    val selectedItem = viewModel.selectedItem.collectAsState().value

    Box(
        modifier = Modifier
            .background(MaterialTheme.colors.surface)
            .fillMaxSize(),
    ) {
        when (state) {
            is RebaseInteractiveState.AwaitingInteraction -> RebaseStateLoaded(
                viewModel,
                state.data,
                selectedItem,
                onFocusLine = {
                    if (
                        selectedItem !is SelectedItem.CommitItem ||
                        !selectedItem.commit.hash.startsWith(it.commit)
                    ) {
                        viewModel.selectLine(it)
                    }
                },
                onCancel = {
                    viewModel.cancel()
                },
                onMoveCommit = { from, to ->
                    viewModel.moveCommit(from, to)
                }
            )

            DataState.Loading -> AppSpinner(modifier = Modifier.align(Alignment.Center), size = 32.dp)
            is RebaseInteractiveState.ProcessingCommits, RebaseInteractiveState.None -> {}
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RebaseStateLoaded(
    viewModel: RepositoryOpenViewModel,
    stepsList: List<RebaseLine>,
    selectedItem: SelectedItem,
    onFocusLine: (RebaseLine) -> Unit,
    onCancel: () -> Unit,
    onMoveCommit: (from: Int, to: Int) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize()
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colors.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(MaterialTheme.colors.secondarySurface)
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(Res.string.rebase_interactive_view_title),
                    color = MaterialTheme.colors.onBackground,
                    style = MaterialTheme.typography.body1,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Reorder the commits by dragging, and pick what happens to each one.",
                    color = MaterialTheme.colors.onBackgroundSecondary,
                    style = MaterialTheme.typography.body2,
                )
            }

            GhostButton(
                text = stringResource(Res.string.generic_button_cancel),
                onClick = onCancel,
                modifier = Modifier.height(36.dp),
            )

            PrimaryButton(
                enabled = true, // TODO Moving commits may also affect stepsList.any { it.rebaseAction != RebaseAction.PICK },
                onClick = {
                    viewModel.continueRebaseInteractive()
                },
                text = stringResource(Res.string.rebase_interactive_view_button_complete_rebase)
            )
        }
        PanelDivider()

        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 32.dp, end = 30.dp, top = 16.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            val labelStyle = MaterialTheme.typography.caption
            val labelColor = MaterialTheme.colors.onBackgroundSecondary

            Text("ACTION", style = labelStyle, fontWeight = FontWeight.SemiBold, color = labelColor, modifier = Modifier.width(96.dp))
            Text("COMMIT", style = labelStyle, fontWeight = FontWeight.SemiBold, color = labelColor, modifier = Modifier.weight(1f))
            Text("HASH", style = labelStyle, fontWeight = FontWeight.SemiBold, color = labelColor)
        }

        val listState = rememberLazyListState()
        val state = rememberVerticalDragDropState(listState) { fromIndex, toIndex ->
            println("P0: $fromIndex\nP1: $toIndex")
            onMoveCommit(fromIndex, toIndex)
        }

        ScrollableLazyColumn(
            modifier = Modifier
                .weight(1f)
                .verticalDragContainer(state, onDraggedItem = {
                    println("OnDragItem $it")
                }),
            state = listState,
        ) {
            itemsIndexed(
                stepsList,
                key = { _, line -> line.commit },
            ) { index, rebaseTodoLine ->
                VerticalDraggableItem(state, index) {
                    RebaseCommit(
                        rebaseLine = rebaseTodoLine,
                        isSelected = selectedItem is SelectedItem.CommitItem && selectedItem.commit.hash.startsWith(
                            rebaseTodoLine.commit
                        ),
                        isFirst = stepsList.first() == rebaseTodoLine,
                        onFocusLine = { onFocusLine(rebaseTodoLine) },
                        onActionChanged = { newAction ->
                            viewModel.onCommitActionChanged(rebaseTodoLine.commit, newAction)
                        },
                        onMessageChanged = { newMessage ->
                            viewModel.onCommitMessageChanged(rebaseTodoLine, newMessage)
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun RebaseCommit(
    rebaseLine: RebaseLine,
    isFirst: Boolean,
    isSelected: Boolean,
    onFocusLine: () -> Unit,
    onActionChanged: (RebaseLine.Action) -> Unit,
    onMessageChanged: (String) -> Unit,
) {
    val action = rebaseLine.action
    val focusRequester = remember { FocusRequester() }


    var newMessage by remember(rebaseLine.commit, action) {
        val message = if (rebaseLine.action == RebaseLine.Action.REWORD && rebaseLine.modifiedMessage != null) {
            rebaseLine.modifiedMessage.orEmpty()
        } else {
            rebaseLine.fullMessage
        }

        mutableStateOf(message)
    }

    Row(
        modifier = Modifier
            .padding(horizontal = 18.dp)
            .clip(MaterialTheme.shapes.medium)
            .heightIn(min = 48.dp)
            .fillMaxWidth()
            .clickable {
                onFocusLine()
            }
            .backgroundIf(isSelected, MaterialTheme.colors.primary.copy(alpha = 0.10f))
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ActionDropdown(
            action,
            isFirst = isFirst,
            onActionDropDownClicked = onFocusLine,
            onActionChanged = { onActionChanged(it) },
        )

        if (action == RebaseLine.Action.REWORD) {
            AdjustableOutlinedTextField(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 40.dp)
                    .focusRequester(focusRequester)
                    .onFocusChanged {
                        if (it.hasFocus && !isSelected) {
                            onFocusLine()
                        }
                    },
                value = newMessage,
                onValueChange = {
                    newMessage = it
                    onMessageChanged(it)
                },
                textStyle = MaterialTheme.typography.body2,
            )
        } else {
            Text(
                text = rebaseLine.shortMessage,
                style = MaterialTheme.typography.body2,
                color = if (action == RebaseLine.Action.DROP) {
                    MaterialTheme.colors.onBackgroundSecondary
                } else {
                    MaterialTheme.colors.onBackground
                },
                textDecoration = if (action == RebaseLine.Action.DROP) TextDecoration.LineThrough else null,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }

        Text(
            text = rebaseLine.commit.take(7),
            fontFamily = monoTypography(),
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onBackgroundSecondary,
        )
    }
}

@Composable
fun ActionDropdown(
    action: RebaseLine.Action,
    isFirst: Boolean,
    onActionDropDownClicked: () -> Unit,
    onActionChanged: (RebaseLine.Action) -> Unit,
) {
    val color = when (action) {
        RebaseLine.Action.PICK -> MaterialTheme.colors.addFile
        RebaseLine.Action.REWORD -> MaterialTheme.colors.graphColors[4]
        RebaseLine.Action.SQUASH, RebaseLine.Action.FIXUP -> MaterialTheme.colors.graphColors[3]
        RebaseLine.Action.EDIT -> MaterialTheme.colors.primaryVariant
        RebaseLine.Action.DROP -> MaterialTheme.colors.error
        RebaseLine.Action.COMMENT -> MaterialTheme.colors.onBackgroundSecondary
    }
    val dropDownItems = if (isFirst) firstItemActions else actions

    DropDownMenu(
        showIcons = false,
        items = {
            dropDownItems.map {
                ContextMenuElement.ContextTextEntry(it.displayName, onClick = { onActionChanged(it.value) })
            }
        }
    ) {
        Row(
            modifier = Modifier
                .width(96.dp)
                .height(28.dp)
                .clip(MaterialTheme.shapes.small)
                .border(1.dp, MaterialTheme.colors.onBackground.copy(alpha = 0.16f), MaterialTheme.shapes.small)
                .handOnHover(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = getActionDisplayName(action).lowercase(),
                color = color,
                style = MaterialTheme.typography.body2,
                fontWeight = FontWeight.Medium,
            )
            Icon(
                painter = painterResource(Res.drawable.dropdown),
                contentDescription = null,
                tint = MaterialTheme.colors.onBackgroundSecondary,
                modifier = Modifier.padding(start = 4.dp).size(14.dp),
            )
        }
    }
}

fun getActionDisplayName(action: RebaseLine.Action): String {
    return when (action) {
        RebaseLine.Action.PICK -> "Pick"
        RebaseLine.Action.REWORD -> "Reword"
        RebaseLine.Action.SQUASH -> "Squash"
        RebaseLine.Action.FIXUP -> "Fixup"
        RebaseLine.Action.EDIT -> "Edit"
        RebaseLine.Action.DROP -> "Drop"
        RebaseLine.Action.COMMENT -> "Comment"
    }
}

val firstItemActions = listOf(
    RebaseAction.PICK,
    RebaseAction.REWORD,
    RebaseAction.DROP,
)

val actions = listOf(
    RebaseAction.PICK,
    RebaseAction.REWORD,
    RebaseAction.SQUASH,
    RebaseAction.FIXUP,
    RebaseAction.EDIT,
    RebaseAction.DROP,
)