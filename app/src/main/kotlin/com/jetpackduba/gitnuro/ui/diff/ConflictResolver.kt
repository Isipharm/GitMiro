package com.jetpackduba.gitnuro.ui.diff

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Checkbox
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TriStateCheckbox
import androidx.compose.runtime.*
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jetpackduba.gitnuro.domain.models.ConflictChoice
import com.jetpackduba.gitnuro.domain.models.ConflictChunk
import com.jetpackduba.gitnuro.domain.models.ConflictFile
import com.jetpackduba.gitnuro.domain.models.ConflictLine
import com.jetpackduba.gitnuro.extensions.handMouseClickable
import com.jetpackduba.gitnuro.extensions.toStringWithSpaces
import com.jetpackduba.gitnuro.theme.*
import com.jetpackduba.gitnuro.ui.components.PrimaryButton
import com.jetpackduba.gitnuro.ui.components.ScrollableLazyColumn
import com.jetpackduba.gitnuro.ui.diff.syntax_highlighter.SyntaxHighlighter
import com.jetpackduba.gitnuro.ui.diff.syntax_highlighter.getSyntaxHighlighterFromExtension

private val CHECKBOX_GUTTER_WIDTH = 32.dp

/**
 * First line number of a chunk in the current and incoming versions of the file
 */
private data class ChunkLineNumbers(val current: Int, val incoming: Int)

/**
 * Lets the user pick, for each conflicting block, the current and/or incoming lines, with a preview of the resulting
 * file at the bottom.
 */
@Composable
fun ConflictResolver(
    filePath: String,
    conflictFile: ConflictFile,
    onResolve: (Map<Int, ConflictChoice>) -> Unit,
) {
    val choices = remember(conflictFile) { mutableStateMapOf<Int, ConflictChoice>() }
    val conflicts = conflictFile.conflicts
    val decidedCount = conflicts.count { choices[it.index]?.isDecided == true }
    val syntaxHighlighter = remember(filePath) { getSyntaxHighlighterFromExtension(filePath.substringAfterLast(".")) }

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
                .background(MaterialTheme.colors.surface)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "$decidedCount of ${conflicts.size} conflicts resolved",
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onBackground,
                modifier = Modifier.weight(1f),
            )
            PrimaryButton(
                text = "Mark as resolved",
                enabled = conflictFile.isFullyDecided(choices),
                onClick = { onResolve(choices.toMap()) },
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colors.surface),
        ) {
            SideHeader(
                title = "Current (HEAD)",
                keptCount = keptCurrentCount,
                totalCount = conflicts.size,
                onKeepAllChanged = ::keepAllCurrent,
                modifier = Modifier.weight(1f),
            )
            SideHeader(
                title = "Incoming",
                keptCount = keptIncomingCount,
                totalCount = conflicts.size,
                onKeepAllChanged = ::keepAllIncoming,
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
                                color = MaterialTheme.colors.diffLineRemoved,
                                onKeptChanged = { choices[chunk.index] = choice.withCurrent(it) },
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                            )
                            ConflictSide(
                                lines = chunk.incoming,
                                firstLineNumber = lineNumbers.incoming,
                                lineNumberDigits = lineNumberDigits,
                                syntaxHighlighter = syntaxHighlighter,
                                isKept = choice.keepIncoming,
                                color = MaterialTheme.colors.diffLineAdded,
                                onKeptChanged = { choices[chunk.index] = choice.withIncoming(it) },
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                            )
                        }
                    }
                }
            }
        }

        SectionTitle(
            text = "Result",
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colors.surface)
                .padding(horizontal = 16.dp, vertical = 2.dp),
        )

        // Line numbers of the result depend on the choices, so they are computed on each recomposition
        val resultFirstLineNumbers = conflictFile.chunks.runningFold(1) { lineNumber, chunk ->
            lineNumber + when (chunk) {
                is ConflictChunk.Common -> chunk.lines.size
                is ConflictChunk.Conflict -> chunk.linesFor(choices[chunk.index]).size
            }
        }

        ScrollableLazyColumn(modifier = Modifier.weight(0.4f).fillMaxWidth()) {
            items(conflictFile.chunks.size) { index ->
                val firstLineNumber = resultFirstLineNumbers[index]

                when (val chunk = conflictFile.chunks[index]) {
                    is ConflictChunk.Common -> Row {
                        Spacer(Modifier.width(CHECKBOX_GUTTER_WIDTH))
                        CodeLines(chunk.lines, firstLineNumber, lineNumberDigits, syntaxHighlighter)
                    }

                    is ConflictChunk.Conflict -> {
                        val choice = choices[chunk.index]

                        if (choice?.isDecided == true) {
                            Row(modifier = Modifier.background(MaterialTheme.colors.secondarySurface)) {
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
}

/**
 * Title of a side with a checkbox to keep or drop that side in every conflicting block
 */
@Composable
private fun SideHeader(
    title: String,
    keptCount: Int,
    totalCount: Int,
    onKeepAllChanged: (Boolean) -> Unit,
    modifier: Modifier,
) {
    val state = when (keptCount) {
        0 -> ToggleableState.Off
        totalCount -> ToggleableState.On
        else -> ToggleableState.Indeterminate
    }
    // Partially kept goes to fully kept, as it's the most common reason to click it
    val onClick = { onKeepAllChanged(state != ToggleableState.On) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.handMouseClickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick,
        ),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.width(CHECKBOX_GUTTER_WIDTH)) {
            TriStateCheckbox(
                state = state,
                onClick = onClick,
                modifier = Modifier.padding(vertical = 4.dp).size(16.dp),
            )
        }
        SectionTitle(title)
    }
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
    color: Color,
    onKeptChanged: (Boolean) -> Unit,
    modifier: Modifier,
) {
    Row(modifier = modifier.background(color.copy(alpha = if (isKept) 0.6f else 0.25f))) {
        Box(
            contentAlignment = Alignment.TopCenter,
            modifier = Modifier
                .width(CHECKBOX_GUTTER_WIDTH)
                .fillMaxHeight()
                .handMouseClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { onKeptChanged(!isKept) },
                ),
        ) {
            Checkbox(
                checked = isKept,
                onCheckedChange = onKeptChanged,
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
