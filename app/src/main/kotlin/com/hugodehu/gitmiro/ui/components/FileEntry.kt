package com.hugodehu.gitmiro.ui.components

import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.hugodehu.gitmiro.theme.monoTypography
import androidx.compose.ui.unit.dp
import com.hugodehu.gitmiro.extensions.backgroundIf
import com.hugodehu.gitmiro.extensions.handMouseClickable
import com.hugodehu.gitmiro.extensions.onDoubleClick
import com.hugodehu.gitmiro.app.generated.resources.Res
import com.hugodehu.gitmiro.app.generated.resources.folder
import com.hugodehu.gitmiro.app.generated.resources.folder_open
import com.hugodehu.gitmiro.theme.backgroundSelected
import com.hugodehu.gitmiro.theme.linesHeight
import com.hugodehu.gitmiro.theme.onBackgroundSecondary
import com.hugodehu.gitmiro.ui.context_menu.ContextMenu
import com.hugodehu.gitmiro.ui.context_menu.ContextMenuElement
import org.jetbrains.compose.resources.painterResource

private const val TREE_START_PADDING = 12

/**
 * Entry of a changed file, flagged with the letter of its status (A, M, D...) drawn in the status color.
 */
@Composable
fun FileEntry(
    statusLetter: String,
    statusColor: Color,
    parentDirectoryPath: String,
    fileName: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit,
    depth: Int = 0,
    onGenerateContextMenu: () -> List<ContextMenuElement>,
    trailingAction: (@Composable BoxScope.(isHovered: Boolean) -> Unit)?,
) {
    FileEntry(
        leading = {
            Text(
                text = statusLetter,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .width(16.dp),
                textAlign = TextAlign.Center,
                fontFamily = monoTypography(),
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.caption,
                color = statusColor,
                maxLines = 1,
            )
        },
        parentDirectoryPath = parentDirectoryPath,
        fileName = fileName,
        isSelected = isSelected,
        onClick = onClick,
        onDoubleClick = onDoubleClick,
        depth = depth,
        onGenerateContextMenu = onGenerateContextMenu,
        trailingAction = trailingAction
    )
}

@Composable
fun FileEntry(
    icon: Painter,
    iconColor: Color,
    parentDirectoryPath: String,
    fileName: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit,
    depth: Int = 0,
    onGenerateContextMenu: () -> List<ContextMenuElement>,
    trailingAction: (@Composable BoxScope.(isHovered: Boolean) -> Unit)?,
) {
    FileEntry(
        leading = {
            Icon(
                painter = icon,
                contentDescription = null,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .size(16.dp),
                tint = iconColor,
            )
        },
        parentDirectoryPath = parentDirectoryPath,
        fileName = fileName,
        isSelected = isSelected,
        onClick = onClick,
        onDoubleClick = onDoubleClick,
        depth = depth,
        onGenerateContextMenu = onGenerateContextMenu,
        trailingAction = trailingAction
    )
}

@Composable
private fun FileEntry(
    leading: @Composable () -> Unit,
    parentDirectoryPath: String,
    fileName: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit,
    depth: Int = 0,
    onGenerateContextMenu: () -> List<ContextMenuElement>,
    trailingAction: (@Composable BoxScope.(isHovered: Boolean) -> Unit)?,
) {
    val hoverInteraction = remember { MutableInteractionSource() }
    val isHovered by hoverInteraction.collectIsHoveredAsState()

    Box(
        modifier = Modifier
            .handMouseClickable { onClick() }
            .onDoubleClick(onDoubleClick)
            .fillMaxWidth()
            .hoverable(hoverInteraction)
    ) {
        ContextMenu(
            items = {
                onGenerateContextMenu()
            },
        ) {
            Row(
                modifier = Modifier
                    .height(MaterialTheme.linesHeight.fileHeight)
                    .fillMaxWidth()
                    .backgroundIf(isSelected, MaterialTheme.colors.backgroundSelected)
                    .padding(start = (TREE_START_PADDING * depth).dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                leading()

                if (parentDirectoryPath.isNotEmpty()) {
                    Text(
                        text = parentDirectoryPath.removeSuffix("/"),
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = 1,
                        softWrap = false,
                        style = MaterialTheme.typography.body2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colors.onBackgroundSecondary,
                    )

                    Text(
                        text = "/",
                        maxLines = 1,
                        softWrap = false,
                        style = MaterialTheme.typography.body2,
                        overflow = TextOverflow.Visible,
                        color = MaterialTheme.colors.onBackgroundSecondary,
                    )
                }
                Text(
                    text = fileName,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(end = 16.dp),
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onBackground,
                )
            }
        }

        trailingAction?.invoke(this, isHovered)
    }
}

@Composable
fun DirectoryEntry(
    dirName: String,
    isExpanded: Boolean,
    depth: Int = 0,
    onClick: () -> Unit,
    onGenerateContextMenu: () -> List<ContextMenuElement>,
) {

    FileEntry(
        icon = painterResource(if (isExpanded) Res.drawable.folder_open else Res.drawable.folder),
        iconColor = MaterialTheme.colors.onBackground,
        isSelected = false,
        onClick = onClick,
        onDoubleClick = {},
        parentDirectoryPath = "",
        fileName = dirName,
        depth = depth,
        onGenerateContextMenu = onGenerateContextMenu,
        trailingAction = null,
    )
}