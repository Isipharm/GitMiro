package com.hugodehu.gitmiro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hugodehu.gitmiro.app.generated.resources.Res
import com.hugodehu.gitmiro.app.generated.resources.done
import com.hugodehu.gitmiro.app.generated.resources.warning
import com.hugodehu.gitmiro.domain.extensions.fileName
import com.hugodehu.gitmiro.domain.extensions.parentDirectoryPath
import com.hugodehu.gitmiro.domain.models.StatusEntry
import com.hugodehu.gitmiro.extensions.handMouseClickable
import com.hugodehu.gitmiro.theme.addFile
import com.hugodehu.gitmiro.theme.graphColors
import com.hugodehu.gitmiro.theme.onBackgroundSecondary
import com.hugodehu.gitmiro.theme.secondarySurface
import com.hugodehu.gitmiro.ui.components.PrimaryButton
import org.jetbrains.compose.resources.painterResource

/**
 * A file touched by the merge, rebase, cherry-pick or revert in progress.
 */
data class ConflictRow(val entry: StatusEntry, val isResolved: Boolean)

/**
 * Left column of the conflict resolution: every conflicted file with its state.
 *
 * @param conflictsCount number of conflicting blocks of a file, null when it can't be split in blocks
 */
@Composable
fun ConflictedFilesPanel(
    rows: List<ConflictRow>,
    selectedPath: String?,
    conflictsCount: suspend (String) -> Int?,
    onOpen: (StatusEntry) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        PanelLabel("CONFLICTED FILES", Modifier.padding(start = 8.dp, bottom = 8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(rows, key = { it.entry.filePath }) { row ->
                ConflictedFileRow(row, row.entry.filePath == selectedPath, conflictsCount, onOpen)
            }
        }
    }
}

@Composable
private fun ConflictedFileRow(
    row: ConflictRow,
    isSelected: Boolean,
    conflictsCount: suspend (String) -> Int?,
    onOpen: (StatusEntry) -> Unit,
) {
    val amber = MaterialTheme.colors.graphColors[3]
    val color = if (row.isResolved) MaterialTheme.colors.addFile else amber
    val count by produceState<Int?>(null, row.entry.filePath, row.isResolved) {
        value = if (row.isResolved) null else conflictsCount(row.entry.filePath)
    }

    val state = when {
        row.isResolved -> "Resolved"
        count == 1 -> "1 conflict"
        count != null -> "$count conflicts"
        else -> "Conflict"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(if (isSelected) MaterialTheme.colors.primary.copy(alpha = 0.14f) else androidx.compose.ui.graphics.Color.Transparent)
            .handMouseClickable { onOpen(row.entry) }
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            painter = painterResource(if (row.isResolved) Res.drawable.done else Res.drawable.warning),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp),
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = row.entry.filePath,
                style = MaterialTheme.typography.body2,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colors.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = state,
                style = MaterialTheme.typography.caption,
                color = color,
            )
        }
    }
}

/**
 * Right column shown while no file is open: what is left to do before the operation can continue.
 */
@Composable
fun ConflictsAttentionPanel(
    unresolved: List<StatusEntry>,
    resolvedCount: Int,
    isRebasing: Boolean,
    onOpen: (StatusEntry) -> Unit,
) {
    val amber = MaterialTheme.colors.graphColors[3]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        PanelLabel("NEEDS YOUR ATTENTION")

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colors.secondarySurface, MaterialTheme.shapes.large)
                .border(1.dp, MaterialTheme.colors.onBackground.copy(alpha = 0.12f), MaterialTheme.shapes.large)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val allResolved = unresolved.isEmpty()

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(
                    painter = painterResource(if (allResolved) Res.drawable.done else Res.drawable.warning),
                    contentDescription = null,
                    tint = if (allResolved) MaterialTheme.colors.addFile else amber,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = when {
                        allResolved -> "All conflicts resolved"
                        unresolved.size == 1 -> "1 file in conflict"
                        else -> "${unresolved.size} files in conflict"
                    },
                    style = MaterialTheme.typography.body1,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colors.onBackground,
                )
            }

            unresolved.forEach { entry ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .background(MaterialTheme.colors.background, MaterialTheme.shapes.small)
                        .handMouseClickable { onOpen(entry) }
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val dir = entry.parentDirectoryPath

                    if (dir.isNotEmpty()) {
                        Text(
                            text = dir,
                            style = MaterialTheme.typography.body2,
                            color = MaterialTheme.colors.onBackgroundSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, false),
                        )
                    }
                    Text(
                        text = entry.fileName,
                        style = MaterialTheme.typography.body2,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colors.onBackground,
                        maxLines = 1,
                    )
                }
            }

            if (!allResolved) {
                PrimaryButton(
                    text = "Resolve conflicts",
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    onClick = { onOpen(unresolved.first()) },
                )
            }
        }

        Text(
            text = when {
                unresolved.isNotEmpty() && isRebasing -> "Fixing the conflict stages the file. Then Continue replays the next commit."
                unresolved.isNotEmpty() -> "Fixing the conflict stages the file. Then Continue finishes the operation."
                isRebasing -> "Everything is staged. Continue replays the next commit."
                else -> "Everything is staged. Continue finishes the operation."
            },
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onBackgroundSecondary.copy(alpha = 0.9f),
        )

        if (resolvedCount > 0) {
            Text(
                text = "$resolvedCount resolved so far",
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onBackgroundSecondary,
            )
        }
    }
}

@Composable
private fun PanelLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.caption,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colors.onBackgroundSecondary,
        modifier = modifier,
    )
}
