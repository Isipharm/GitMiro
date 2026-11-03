package com.hugodehu.gitmiro.ui.context_menu

import com.hugodehu.gitmiro.app.generated.resources.Res
import com.hugodehu.gitmiro.app.generated.resources.apply_stash
import com.hugodehu.gitmiro.app.generated.resources.message
import com.hugodehu.gitmiro.app.generated.resources.stash_context_menu_pop_stash
import com.hugodehu.gitmiro.app.generated.resources.stash_context_menu_stash_with_message
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

fun stashContextMenuItems(
    canStash: Boolean,
    canPopStash: Boolean,
    onStashWithMessage: () -> Unit,
    onPopStash: () -> Unit,
): List<ContextMenuElement> {
    return buildList {
        if (canStash) {
            add(
                ContextMenuElement.ContextTextEntry(
                    composableLabel = { stringResource(Res.string.stash_context_menu_stash_with_message) },
                    onClick = onStashWithMessage,
                    icon = { painterResource(Res.drawable.message) },
                )
            )
        }

        if (canPopStash) {
            add(
                ContextMenuElement.ContextTextEntry(
                    composableLabel = { stringResource(Res.string.stash_context_menu_pop_stash) },
                    onClick = onPopStash,
                    icon = { painterResource(Res.drawable.apply_stash) },
                )
            )
        }
    }
}
