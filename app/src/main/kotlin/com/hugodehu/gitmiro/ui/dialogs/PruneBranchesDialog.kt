package com.hugodehu.gitmiro.ui.dialogs

import com.hugodehu.gitmiro.ui.components.AppCheckbox
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hugodehu.gitmiro.app.generated.resources.Res
import com.hugodehu.gitmiro.app.generated.resources.cut
import com.hugodehu.gitmiro.app.generated.resources.generic_button_cancel
import com.hugodehu.gitmiro.domain.models.Branch
import com.hugodehu.gitmiro.extensions.handMouseClickable
import com.hugodehu.gitmiro.theme.onBackgroundSecondary
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import com.hugodehu.gitmiro.theme.monoTypography
import com.hugodehu.gitmiro.ui.components.GhostButton
import com.hugodehu.gitmiro.ui.dialogs.base.DialogHeader
import com.hugodehu.gitmiro.ui.components.PrimaryButton
import com.hugodehu.gitmiro.ui.components.ScrollableLazyColumn
import com.hugodehu.gitmiro.ui.dialogs.base.MaterialDialog
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Lists the local branches whose remote branch is gone, all selected, and deletes the selected ones.
 */
@Composable
fun PruneBranchesDialog(
    branches: List<Branch>,
    onDelete: (List<Branch>) -> Unit,
    onDismiss: () -> Unit,
) {
    var selected by remember(branches) { mutableStateOf(branches.toSet()) }

    MaterialDialog(onCloseRequested = onDismiss) {
        Column(modifier = Modifier.width(500.dp)) {
            DialogHeader(
                icon = painterResource(Res.drawable.cut),
                title = if (branches.isEmpty()) "Nothing to prune" else "Prune branches",
                subtitle = if (branches.isEmpty()) {
                    "Every local branch with a remote branch still has it."
                } else {
                    "These branches no longer exist on the server. The selected ones are deleted locally, even if " +
                            "their commits aren't merged, as happens with squash merges."
                },
                modifier = Modifier.padding(bottom = 18.dp),
            )

            if (branches.isNotEmpty()) {
                ScrollableLazyColumn(
                    modifier = Modifier
                        .heightIn(max = 300.dp)
                        .fillMaxWidth(),
                ) {
                    items(branches) { branch ->
                        val isSelected = branch in selected

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .padding(bottom = 4.dp)
                                .fillMaxWidth()
                                .height(40.dp)
                                .clip(MaterialTheme.shapes.small)
                                .background(
                                    if (isSelected) MaterialTheme.colors.primary.copy(alpha = 0.10f) else Color.Transparent
                                )
                                .handMouseClickable {
                                    selected = if (isSelected) selected - branch else selected + branch
                                }
                                .padding(horizontal = 12.dp),
                        ) {
                            AppCheckbox(checked = isSelected, onCheckedChange = {})

                            Text(
                                text = branch.simpleName,
                                fontFamily = monoTypography(),
                                color = if (isSelected) {
                                    MaterialTheme.colors.onBackground
                                } else {
                                    MaterialTheme.colors.onBackgroundSecondary
                                },
                                style = MaterialTheme.typography.body2,
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .padding(top = 18.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = if (branches.isEmpty()) "" else "${selected.size} of ${branches.size} selected",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onBackgroundSecondary,
                    modifier = Modifier.weight(1f),
                )

                if (branches.isEmpty()) {
                    PrimaryButton(text = "OK", onClick = onDismiss)
                } else {
                    GhostButton(
                        text = stringResource(Res.string.generic_button_cancel),
                        onClick = onDismiss,
                        modifier = Modifier.height(36.dp),
                    )
                    PrimaryButton(
                        text = "Prune ${selected.size} branch${if (selected.size == 1) "" else "es"}",
                        enabled = selected.isNotEmpty(),
                        onClick = { onDelete(branches.filter { it in selected }) },
                    )
                }
            }
        }
    }
}
