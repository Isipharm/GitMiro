package com.hugodehu.gitmiro.ui.diff

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.lerp
import com.hugodehu.gitmiro.ui.components.PanelDivider
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Checkbox
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TriStateCheckbox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hugodehu.gitmiro.domain.models.ConflictChoice
import com.hugodehu.gitmiro.domain.models.ConflictChunk
import com.hugodehu.gitmiro.domain.models.ConflictFile
import com.hugodehu.gitmiro.domain.models.ConflictLine
import com.hugodehu.gitmiro.extensions.handMouseClickable
import com.hugodehu.gitmiro.extensions.toStringWithSpaces
import com.hugodehu.gitmiro.theme.*
import com.hugodehu.gitmiro.ui.components.PrimaryButton
import com.hugodehu.gitmiro.ui.components.ScrollableLazyColumn
import com.hugodehu.gitmiro.ui.components.SecondaryButton
import com.hugodehu.gitmiro.ui.diff.syntax_highlighter.SyntaxHighlighter
import com.hugodehu.gitmiro.ui.diff.syntax_highlighter.getSyntaxHighlighterFromExtension
import kotlinx.coroutines.delay

private val CHECKBOX_GUTTER_WIDTH = 32.dp
private const val LOCKED_WARNING_DURATION_MS = 4_000L

/**
 * First line number of a chunk in the current and incoming versions of the file
 */
private data class ChunkLineNumbers(val current: Int, val incoming: Int)

/**
 * Lets the user pick, for each conflicting block, the current and/or incoming lines, with a preview of the resulting
 * file at the bottom. Once every block is decided, the result can also be edited by hand.
 */
@Composable
fun ConflictResolver(
    filePath: String,
    conflictFile: ConflictFile,
    onResolve: (content: ByteArray) -> Unit,
) {
    val choices = remember(conflictFile) { mutableStateMapOf<Int, ConflictChoice>() }
    val conflicts = conflictFile.conflicts
    val decidedCount = conflicts.count { choices[it.index]?.isDecided == true }
    val isFullyDecided = conflictFile.isFullyDecided(choices)
    val syntaxHighlighter = remember(filePath) { getSyntaxHighlighterFromExtension(filePath.substringAfterLast(".")) }

    // Once edited by hand, the result no longer follows the checkboxes, which are locked until the edits are discarded
    var editedText by remember(conflictFile) { mutableStateOf<String?>(null) }
    val isEditedByHand = editedText != null
    val canResolve = editedText?.let { !ConflictFile.hasConflictMarkers(it) } ?: isFullyDecided

    // Incremented on each click on a locked checkbox, to show the warning again for its whole duration
    var lockedClicksCount by remember { mutableIntStateOf(0) }
    var showLockedWarning by remember { mutableStateOf(false) }

    LaunchedEffect(lockedClicksCount) {
        if (lockedClicksCount > 0) {
            showLockedWarning = true
            delay(LOCKED_WARNING_DURATION_MS)
            showLockedWarning = false
        }
    }

    LaunchedEffect(isEditedByHand) {
        if (!isEditedByHand) showLockedWarning = false
    }

    val onLockedClick: () -> Unit = { lockedClicksCount++ }

    val chunksLineNumbers = remember(conflictFile) {
        var current = 1
        var incoming = 1

        conflictFile.chunks.map { chunk ->
            ChunkLineNumbers(current, incoming).also {
                when (chunk) {
                    is ConflictChunk.Common -> {
                        current += chunk.lines.size
                        incoming += chunk.lines.size
                    }

                    is ConflictChunk.Conflict -> {
                        current += chunk.current.size
                        incoming += chunk.incoming.size
                    }
                }
            }
        }
    }

    // The result can't be longer than keeping both sides of every conflict
    val lineNumberDigits = remember(conflictFile) {
        conflictFile.chunks.sumOf { chunk ->
            when (chunk) {
                is ConflictChunk.Common -> chunk.lines.size
                is ConflictChunk.Conflict -> chunk.current.size + chunk.incoming.size
            }
        }.toString().length
    }

    // Only changes one side, so all blocks of a side can be kept while keeping a few blocks of the other side
    fun keepAllCurrent(keep: Boolean) = conflicts.forEach {
        choices[it.index] = (choices[it.index] ?: ConflictChoice()).withCurrent(keep)
    }

    fun keepAllIncoming(keep: Boolean) = conflicts.forEach {
        choices[it.index] = (choices[it.index] ?: ConflictChoice()).withIncoming(keep)
    }

    val keptCurrentCount = conflicts.count { choices[it.index]?.keepCurrent == true }
    val keptIncomingCount = conflicts.count { choices[it.index]?.keepIncoming == true }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .background(MaterialTheme.colors.secondarySurface)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "$decidedCount of ${conflicts.size} resolved",
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.modifyFile,
                modifier = Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(MaterialTheme.colors.modifyFile.copy(alpha = 0.14f))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )

            Spacer(Modifier.weight(1f))

            ResolverGhostButton("Use current", enabled = !isEditedByHand) {
                keepAllCurrent(true)
                keepAllIncoming(false)
            }
            ResolverGhostButton("Use incoming", enabled = !isEditedByHand) {
                keepAllCurrent(false)
                keepAllIncoming(true)
            }
            ResolverGhostButton("Use both", enabled = !isEditedByHand) {
                keepAllCurrent(true)
                keepAllIncoming(true)
            }
        }
        PanelDivider()

        if (showLockedWarning) {
            Text(
                text = "The result has been edited by hand, checkbox changes can't be applied to it. " +
                        "Discard the edits to use the checkboxes again.",
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colors.conflictFile.copy(alpha = 0.4f))
                    .padding(horizontal = 16.dp, vertical = 6.dp),
            )
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            SideHeader(
                title = "Current (HEAD)",
                tint = MaterialTheme.colors.graphColors[1],
                keptCount = keptCurrentCount,
                totalCount = conflicts.size,
                isLocked = isEditedByHand,
                onKeepAllChanged = ::keepAllCurrent,
                onLockedClick = onLockedClick,
                modifier = Modifier.weight(1f),
            )
            SideHeader(
                title = "Incoming",
                tint = MaterialTheme.colors.addFile,
                keptCount = keptIncomingCount,
                totalCount = conflicts.size,
                isLocked = isEditedByHand,
                onKeepAllChanged = ::keepAllIncoming,
                onLockedClick = onLockedClick,
                modifier = Modifier.weight(1f),
            )
        }

        ScrollableLazyColumn(modifier = Modifier.weight(0.6f).fillMaxWidth()) {
            items(conflictFile.chunks.size) { index ->
                val lineNumbers = chunksLineNumbers[index]

                when (val chunk = conflictFile.chunks[index]) {
                    // The gutter keeps common lines aligned with the code of the conflicting blocks
                    is ConflictChunk.Common -> Row {
                        Spacer(Modifier.width(CHECKBOX_GUTTER_WIDTH))
                        CodeLines(chunk.lines, lineNumbers.current, lineNumberDigits, syntaxHighlighter, Modifier.weight(1f))
                        Spacer(Modifier.width(CHECKBOX_GUTTER_WIDTH))
                        CodeLines(chunk.lines, lineNumbers.incoming, lineNumberDigits, syntaxHighlighter, Modifier.weight(1f))
                    }

                    is ConflictChunk.Conflict -> {
                        val choice = choices[chunk.index] ?: ConflictChoice()

                        Row(modifier = Modifier.padding(vertical = 2.dp).height(IntrinsicSize.Min)) {
                            ConflictSide(
                                lines = chunk.current,
                                firstLineNumber = lineNumbers.current,
                                lineNumberDigits = lineNumberDigits,
                                syntaxHighlighter = syntaxHighlighter,
                                isKept = choice.keepCurrent,
                                isLocked = isEditedByHand,
                                color = MaterialTheme.colors.graphColors[1],
                                onKeptChanged = { choices[chunk.index] = choice.withCurrent(it) },
                                onLockedClick = onLockedClick,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                            )
                            ConflictSide(
                                lines = chunk.incoming,
                                firstLineNumber = lineNumbers.incoming,
                                lineNumberDigits = lineNumberDigits,
                                syntaxHighlighter = syntaxHighlighter,
                                isKept = choice.keepIncoming,
                                isLocked = isEditedByHand,
                                color = MaterialTheme.colors.addFile,
                                onKeptChanged = { choices[chunk.index] = choice.withIncoming(it) },
                                onLockedClick = onLockedClick,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                            )
                        }
                    }
                }
            }
        }

        PanelDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .background(MaterialTheme.colors.secondarySurface)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SectionTitle("Result")

            Text(
                text = when {
                    isEditedByHand -> "Edited by hand"
                    isFullyDecided -> "Editable"
                    else -> "Resolve every conflict to edit the result"
                },
                style = MaterialTheme.typography.caption,
                color = if (isEditedByHand) MaterialTheme.colors.conflictFile else MaterialTheme.colors.onBackgroundSecondary,
                modifier = Modifier.weight(1f),
            )

            if (isEditedByHand) {
                SecondaryButton(text = "Discard edits", onClick = { editedText = null })
            }

            PrimaryButton(
                text = "Mark as resolved",
                enabled = canResolve,
                onClick = {
                    // The checkboxes result keeps the exact original bytes, the edited text is encoded like the file
                    val content = editedText?.let { conflictFile.encode(it) } ?: conflictFile.resolve(choices)
                    onResolve(content)
                },
            )
        }
        PanelDivider()

        val resultModifier = Modifier.weight(0.4f).fillMaxWidth()

        if (isEditedByHand || isFullyDecided) {
            val resultText = editedText ?: conflictFile.resolvedText(choices)

            EditableResult(
                text = resultText,
                changedLines = remember(conflictFile, resultText) { conflictFile.changedLines(resultText) },
                minLineNumberDigits = lineNumberDigits,
                onTextChanged = { editedText = it },
                modifier = resultModifier,
            )
        } else {
            ResultPreview(conflictFile, choices, lineNumberDigits, syntaxHighlighter, resultModifier)
        }
    }
}

/**
 * Read only result shown while some conflicts are not decided yet
 */
@Composable
private fun ResultPreview(
    conflictFile: ConflictFile,
    choices: Map<Int, ConflictChoice>,
    lineNumberDigits: Int,
    syntaxHighlighter: SyntaxHighlighter,
    modifier: Modifier,
) {
    val firstLineNumbers = conflictFile.chunks.runningFold(1) { lineNumber, chunk ->
        lineNumber + when (chunk) {
            is ConflictChunk.Common -> chunk.lines.size
            is ConflictChunk.Conflict -> chunk.linesFor(choices[chunk.index]).size
        }
    }

    ScrollableLazyColumn(modifier = modifier) {
        items(conflictFile.chunks.size) { index ->
            val firstLineNumber = firstLineNumbers[index]

            when (val chunk = conflictFile.chunks[index]) {
                is ConflictChunk.Common -> Row {
                    Spacer(Modifier.width(CHECKBOX_GUTTER_WIDTH))
                    CodeLines(chunk.lines, firstLineNumber, lineNumberDigits, syntaxHighlighter)
                }

                is ConflictChunk.Conflict -> {
                    val choice = choices[chunk.index]

                    if (choice?.isDecided == true) {
                        Row(modifier = Modifier.background(MaterialTheme.colors.diffLineAdded)) {
                            Spacer(Modifier.width(CHECKBOX_GUTTER_WIDTH))
                            CodeLines(chunk.linesFor(choice), firstLineNumber, lineNumberDigits, syntaxHighlighter)
                        }
                    } else {
                        Text(
                            text = "Conflict ${chunk.index + 1} not resolved yet",
                            style = MaterialTheme.typography.body2,
                            color = MaterialTheme.colors.onBackground,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colors.conflictFile.copy(alpha = 0.3f))
                                .padding(horizontal = 16.dp, vertical = 2.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EditableResult(
    text: String,
    changedLines: Set<Int>,
    minLineNumberDigits: Int,
    onTextChanged: (String) -> Unit,
    modifier: Modifier,
) {
    val linesCount = text.lines().size
    val lineNumberDigits = maxOf(minLineNumberDigits, linesCount.toString().length)
    val textStyle = MaterialTheme.typography.body2.copy(fontFamily = monoTypography())
    val changedLineColor = MaterialTheme.colors.diffLineAdded
    var textLayout by remember { mutableStateOf<TextLayoutResult?>(null) }

    // Line numbers and text share the vertical scroll, the text scrolls horizontally so lines are not wrapped.
    // Changed lines are painted on the whole row width, using the text layout to know where each line is
    Row(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .drawBehind {
                val layout = textLayout ?: return@drawBehind

                for (line in changedLines) {
                    if (line >= layout.lineCount) continue

                    val top = layout.getLineTop(line)
                    drawRect(
                        color = changedLineColor,
                        topLeft = Offset(0f, top),
                        size = Size(size.width, layout.getLineBottom(line) - top),
                    )
                }
            }
    ) {
        Spacer(Modifier.width(CHECKBOX_GUTTER_WIDTH))
        Text(
            text = (1..linesCount).joinToString("\n") { it.toStringWithSpaces(lineNumberDigits) },
            style = textStyle,
            color = MaterialTheme.colors.onBackgroundSecondary,
            modifier = Modifier.padding(start = 8.dp, end = 4.dp),
        )
        Box(modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState())) {
            BasicTextField(
                value = text,
                onValueChange = onTextChanged,
                textStyle = textStyle.copy(color = MaterialTheme.colors.onBackground),
                cursorBrush = SolidColor(MaterialTheme.colors.onBackground),
                onTextLayout = { textLayout = it },
                modifier = Modifier.padding(start = 16.dp, end = 16.dp),
            )
        }
    }
}

/**
 * Title of a side with a checkbox to keep or drop that side in every conflicting block
 */
@Composable
private fun SideHeader(
    title: String,
    tint: Color,
    keptCount: Int,
    totalCount: Int,
    isLocked: Boolean,
    onKeepAllChanged: (Boolean) -> Unit,
    onLockedClick: () -> Unit,
    modifier: Modifier,
) {
    val state = when (keptCount) {
        0 -> ToggleableState.Off
        totalCount -> ToggleableState.On
        else -> ToggleableState.Indeterminate
    }
    // Partially kept goes to fully kept, as it's the most common reason to click it
    val onClick = { if (isLocked) onLockedClick() else onKeepAllChanged(state != ToggleableState.On) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(36.dp)
            .background(tint.copy(alpha = 0.10f))
            .handMouseClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.width(CHECKBOX_GUTTER_WIDTH)) {
            TriStateCheckbox(
                state = state,
                onClick = onClick,
                enabled = !isLocked,
                modifier = Modifier.size(16.dp),
            )
        }
        Box(Modifier.padding(end = 8.dp).size(8.dp).clip(CircleShape).background(tint))
        Text(
            text = title,
            style = MaterialTheme.typography.body2,
            fontWeight = FontWeight.Medium,
            color = lerp(tint, MaterialTheme.colors.onBackground, 0.4f),
        )
    }
}

@Composable
private fun ResolverGhostButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Text(
        text = text,
        style = MaterialTheme.typography.body2,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colors.onBackground.copy(alpha = if (enabled) 1f else 0.4f),
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .border(1.dp, MaterialTheme.colors.onBackground.copy(alpha = 0.16f), MaterialTheme.shapes.small)
            .handMouseClickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    )
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.body2,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colors.onBackground,
        modifier = modifier,
    )
}

/**
 * One side of a conflicting block. The checkbox gutter is clickable on the whole height of the block, not only on the
 * checkbox itself.
 */
@Composable
private fun ConflictSide(
    lines: List<ConflictLine>,
    firstLineNumber: Int,
    lineNumberDigits: Int,
    syntaxHighlighter: SyntaxHighlighter,
    isKept: Boolean,
    isLocked: Boolean,
    color: Color,
    onKeptChanged: (Boolean) -> Unit,
    onLockedClick: () -> Unit,
    modifier: Modifier,
) {
    Row(modifier = modifier.background(color.copy(alpha = if (isKept) 0.28f else 0.12f))) {
        Box(
            contentAlignment = Alignment.TopCenter,
            modifier = Modifier
                .width(CHECKBOX_GUTTER_WIDTH)
                .fillMaxHeight()
                .handMouseClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { if (isLocked) onLockedClick() else onKeptChanged(!isKept) },
                ),
        ) {
            Checkbox(
                checked = isKept,
                onCheckedChange = onKeptChanged,
                enabled = !isLocked,
                modifier = Modifier.padding(top = 4.dp).size(16.dp),
            )
        }

        if (lines.isEmpty()) {
            Text(
                text = "No lines",
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onBackgroundSecondary,
                modifier = Modifier.padding(vertical = 4.dp),
            )
        } else {
            CodeLines(lines, firstLineNumber, lineNumberDigits, syntaxHighlighter)
        }
    }
}

@Composable
private fun CodeLines(
    lines: List<ConflictLine>,
    firstLineNumber: Int,
    lineNumberDigits: Int,
    syntaxHighlighter: SyntaxHighlighter,
    modifier: Modifier = Modifier,
) {
    SelectionContainer(modifier = modifier) {
        Column {
            lines.forEachIndexed { index, line ->
                Row {
                    DisableSelection {
                        LineNumber(
                            text = (firstLineNumber + index).toStringWithSpaces(lineNumberDigits),
                            remarked = false,
                        )
                    }
                    DiffText(text = line.text, matchLine = null, syntaxHighlighter = syntaxHighlighter)
                }
            }
        }
    }
}
