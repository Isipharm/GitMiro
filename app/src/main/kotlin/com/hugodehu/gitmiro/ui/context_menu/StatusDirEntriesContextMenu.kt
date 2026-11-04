package com.hugodehu.gitmiro.ui.context_menu

import com.hugodehu.gitmiro.app.generated.resources.*
import com.hugodehu.gitmiro.domain.models.EntryType
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

fun statusDirEntriesContextMenuItems(
    entryType: EntryType,
    onStageChanges: () -> Unit,
    onDiscardDirectoryChanges: () -> Unit,
    onOpenInFolder: () -> Unit,
): List<ContextMenuElement> {
    return mutableListOf<ContextMenuElement>().apply {
        addContextMenu(
            composableLabel = {
                if (entryType == EntryType.STAGED) {
                    stringResource(Res.string.status_dir_entries_context_menu_unstage_changes)
                } else {
                    stringResource(Res.string.status_dir_entries_context_menu_stage_changes)
                }
            },
            icon = {
                if (entryType == EntryType.STAGED) {
                    painterResource(Res.drawable.remove_done)
                } else {
                    painterResource(Res.drawable.done)
                }
            },
            onClick = onStageChanges,
        )

        addContextMenu(
            composableLabel = { stringResource(Res.string.status_dir_entries_context_menu_open_in_folder) },
            icon = { painterResource(Res.drawable.folder_open) },
            onClick = onOpenInFolder,
        )

        if (entryType == EntryType.UNSTAGED) {
            add(ContextMenuElement.ContextSeparator)

            addContextMenu(
                composableLabel = { stringResource(Res.string.status_dir_entries_context_menu_discard_changes) },
                icon = { painterResource(Res.drawable.undo) },
                onClick = onDiscardDirectoryChanges,
                isDanger = true,
            )
        }
    }
}
