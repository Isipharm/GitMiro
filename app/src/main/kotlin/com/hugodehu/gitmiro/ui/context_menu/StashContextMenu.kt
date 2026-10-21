package com.hugodehu.gitmiro.ui.context_menu

import com.hugodehu.gitmiro.app.generated.resources.Res
import com.hugodehu.gitmiro.app.generated.resources.message
import com.hugodehu.gitmiro.app.generated.resources.stash_context_menu_stash_with_message
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

fun stashContextMenuItems(
    onStashWithMessage: () -> Unit,
): List<ContextMenuElement> {
    return mutableListOf(
        ContextMenuElement.ContextTextEntry(
            composableLabel = { stringResource(Res.string.stash_context_menu_stash_with_message) },
            onClick = onStashWithMessage,
            icon = { painterResource(Res.drawable.message) },
        ),
    )
}
