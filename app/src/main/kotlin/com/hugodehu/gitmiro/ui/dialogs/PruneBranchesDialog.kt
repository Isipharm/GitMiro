package com.hugodehu.gitmiro.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Checkbox
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
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(480.dp),
        ) {
            Icon(
                painterResource(Res.drawable.cut),
                contentDescription = null,
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .size(64.dp),
                tint = MaterialTheme.colors.onBackground,
            )

            Text(
                text = if (branches.isEmpty()) "Nothing to prune" else "Prune local branches",
                modifier = Modifier.padding(bottom = 8.dp),
                color = MaterialTheme.colors.onBackground,
                style = MaterialTheme.typography.body1,
                fontWeight = FontWeight.SemiBold,
            )

            Text(
                text = if (branches.isEmpty()) {
                    "Every local branch with a remote branch still has it."
                } else {
                    "The remote branch of these local branches was deleted, usually after their pull request " +
                            "was merged. The selected branches are deleted even if their commits aren't merged " +
                            "into another branch, as happens with squash merges."
                },
                color = MaterialTheme.colors.onBackground,
                style = MaterialTheme.typography.body2,
                textAlign = TextAlign.Center,
            )

            if (branches.isNotEmpty()) {
                ScrollableLazyColumn(
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .heightIn(max = 300.dp)
                        .fillMaxWidth(),
                ) {
                    items(branches) { branch ->
                        val isSelected = branch in selected

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .handMouseClickable {
                                    selected = if (isSelected) selected - branch else selected + branch
                                },
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = null,
                                modifier = Modifier.padding(8.dp).size(16.dp),
                            )

                            Text(
                                text = branch.simpleName,
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
                    .padding(top = 24.dp)
                    .align(Alignment.End),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (branches.isEmpty()) {
                    PrimaryButton(text = "OK", onClick = onDismiss)
                } else {
                    PrimaryButton(
                        text = stringResource(Res.string.generic_button_cancel),
                        onClick = onDismiss,
                        backgroundColor = Color.Transparent,
                        textColor = MaterialTheme.colors.onBackground,
                    )
                    PrimaryButton(
                        text = "Delete ${selected.size} branch${if (selected.size == 1) "" else "es"}",
                        enabled = selected.isNotEmpty(),
                        onClick = { onDelete(branches.filter { it in selected }) },
                        backgroundColor = MaterialTheme.colors.error,
                        backgroundDisabled = MaterialTheme.colors.error.copy(alpha = 0.5f),
                        textColor = MaterialTheme.colors.onError,
                    )
                }
            }
        }
    }
}
