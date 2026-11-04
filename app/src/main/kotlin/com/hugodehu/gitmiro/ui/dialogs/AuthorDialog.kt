package com.hugodehu.gitmiro.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hugodehu.gitmiro.theme.onBackgroundSecondary
import com.hugodehu.gitmiro.ui.components.AdjustableOutlinedTextField
import com.hugodehu.gitmiro.ui.components.GhostButton
import com.hugodehu.gitmiro.ui.components.PanelDivider
import com.hugodehu.gitmiro.ui.components.PrimaryButton
import com.hugodehu.gitmiro.ui.dialogs.base.MaterialDialog
import com.hugodehu.gitmiro.viewmodels.AuthorViewModel

@Composable
fun AuthorDialog(
    viewModel: AuthorViewModel,
    onDismiss: () -> Unit,
) {
    val authorInfo by viewModel.authorInfo.collectAsState()

    var globalName by remember(authorInfo) { mutableStateOf(authorInfo.globalIdentity.name) }
    var globalEmail by remember(authorInfo) { mutableStateOf(authorInfo.globalIdentity.email) }
    var name by remember(authorInfo) { mutableStateOf(authorInfo.repositoryIdentity.name) }
    var email by remember(authorInfo) { mutableStateOf(authorInfo.repositoryIdentity.email) }

    MaterialDialog(onCloseRequested = onDismiss) {
        Column(
            modifier = Modifier.width(500.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colors.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = (name?.takeIf { it.isNotBlank() } ?: globalName).orEmpty().take(1).uppercase(),
                        color = MaterialTheme.colors.onPrimary,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.body1,
                    )
                }

                Column(modifier = Modifier.padding(start = 14.dp)) {
                    Text(
                        text = "Git identity",
                        color = MaterialTheme.colors.onBackground,
                        style = MaterialTheme.typography.body1,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 3.dp),
                    )
                    Text(
                        text = "Name and e-mail written on the commits you create.",
                        color = MaterialTheme.colors.onBackgroundSecondary,
                        style = MaterialTheme.typography.body2,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionTitle("Global", "all your repositories")
                LabeledInput("Name", globalName.orEmpty(), hint = "") { globalName = it }
                LabeledInput("E-mail", globalEmail.orEmpty(), hint = "") { globalEmail = it }
            }

            PanelDivider()

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionTitle("This repository", "overrides the global identity")

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LabeledInput("Name", name.orEmpty(), hint = globalName.orEmpty(), modifier = Modifier.weight(1f)) { name = it }
                    LabeledInput("E-mail", email.orEmpty(), hint = globalEmail.orEmpty(), modifier = Modifier.weight(1f)) { email = it }
                }

                Text(
                    text = "Leave empty to use the global identity (shown in grey).",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onBackgroundSecondary,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GhostButton(text = "Cancel", onClick = onDismiss, modifier = Modifier.height(36.dp))
                PrimaryButton(
                    onClick = {
                        viewModel.saveAuthorInfo(
                            globalName,
                            globalEmail,
                            name,
                            email,
                        )
                        onDismiss()
                    },
                    text = "Save",
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            color = MaterialTheme.colors.onBackground,
            style = MaterialTheme.typography.body2,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = subtitle,
            color = MaterialTheme.colors.onBackgroundSecondary,
            style = MaterialTheme.typography.body2,
        )
    }
}

@Composable
private fun LabeledInput(
    title: String,
    value: String,
    hint: String,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.caption,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colors.onBackgroundSecondary,
        )

        AdjustableOutlinedTextField(
            value = value,
            hint = hint,
            modifier = Modifier.fillMaxWidth(),
            onValueChange = onValueChange,
            singleLine = true,
        )
    }
}
