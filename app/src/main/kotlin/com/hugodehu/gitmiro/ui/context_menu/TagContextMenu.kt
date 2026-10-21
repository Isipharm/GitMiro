package com.hugodehu.gitmiro.ui.context_menu

import com.hugodehu.gitmiro.app.generated.resources.*
import com.hugodehu.gitmiro.app.generated.resources.Res
import com.hugodehu.gitmiro.app.generated.resources.delete
import com.hugodehu.gitmiro.app.generated.resources.start
import com.hugodehu.gitmiro.app.generated.resources.tag_context_menu_checkout_tag_commit
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

fun tagContextMenuItems(
    onCheckoutTag: () -> Unit,
    onDeleteTag: () -> Unit,
): List<ContextMenuElement> {
    return mutableListOf(
        ContextMenuElement.ContextTextEntry(
            composableLabel = { stringResource(Res.string.tag_context_menu_checkout_tag_commit) },
            icon = { painterResource(Res.drawable.start) },
            onClick = onCheckoutTag
        ),
        ContextMenuElement.ContextTextEntry(
            composableLabel = { stringResource(Res.string.tag_context_menu_delete_tag) },
            icon = { painterResource(Res.drawable.delete) },
            onClick = onDeleteTag
        )
    )
}