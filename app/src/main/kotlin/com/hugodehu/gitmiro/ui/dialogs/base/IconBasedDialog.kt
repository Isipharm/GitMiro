package com.hugodehu.gitmiro.ui.dialogs.base

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.clip
import com.hugodehu.gitmiro.theme.onBackgroundSecondary
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hugodehu.gitmiro.app.generated.resources.Res
import com.hugodehu.gitmiro.app.generated.resources.generic_button_cancel
import com.hugodehu.gitmiro.app.generated.resources.tag
import com.hugodehu.gitmiro.theme.AppTheme
import com.hugodehu.gitmiro.ui.components.PrimaryButton
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Preview
@Composable
fun IconBasedDialogPreview() {
    AppTheme {
        IconBasedDialog(
            icon = painterResource(Res.drawable.tag),
            title = "Sample dialog",
            subtitle = "Subtitle example",
            primaryActionText = "Do it!",
            isPrimaryActionEnabled = true,
            beforeActionsFocusRequester = remember { FocusRequester() },
            actionsFocusRequester = remember { FocusRequester() },
            afterActionsFocusRequester = remember { FocusRequester() },
            onDismiss = {},
            onPrimaryActionClicked = {},
        ) {

        }
    }
}

@Composable
fun IconBasedDialog(
    icon: Painter,
    title: String,
    subtitle: String,
    primaryActionText: String,
    isPrimaryActionEnabled: Boolean = true,
    showCancelAction: Boolean = true,
    cancelActionText: String = stringResource(Res.string.generic_button_cancel),
    beforeActionsFocusRequester: FocusRequester?,
    actionsFocusRequester: FocusRequester?,
    afterActionsFocusRequester: FocusRequester?,
    onDismiss: () -> Unit,
    onPrimaryActionClicked: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val cancelButtonFocusRequester = remember { FocusRequester() }

    MaterialDialog(onCloseRequested = onDismiss) {
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.width(IntrinsicSize.Min).widthIn(min = 400.dp)
        ) {
            Row(
                modifier = Modifier.padding(bottom = 18.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colors.primary.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colors.primaryVariant,
                    )
                }

                Column(modifier = Modifier.padding(start = 14.dp)) {
                    Text(
                        text = title,
                        modifier = Modifier.padding(bottom = 3.dp),
                        color = MaterialTheme.colors.onBackground,
                        style = MaterialTheme.typography.body1,
                        fontWeight = FontWeight.SemiBold,
                    )

                    Text(
                        text = subtitle,
                        color = MaterialTheme.colors.onBackgroundSecondary,
                        style = MaterialTheme.typography.body2,
                    )
                }
            }

            content()

            Row(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .align(Alignment.End)
            ) {
                if (showCancelAction) {
                    PrimaryButton(
                        text = cancelActionText,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .border(
                                1.dp,
                                MaterialTheme.colors.onBackground.copy(alpha = 0.16f),
                                MaterialTheme.shapes.small,
                            )
                            .run {
                                if (
                                    actionsFocusRequester != null &&
                                    afterActionsFocusRequester != null
                                ) {
                                    this.focusRequester(cancelButtonFocusRequester)
                                        .focusProperties {
                                            this.previous = actionsFocusRequester
                                            this.next = afterActionsFocusRequester
                                        }
                                } else {
                                    this
                                }
                            },
                        onClick = onDismiss,
                        backgroundColor = Color.Transparent,
                        textColor = MaterialTheme.colors.onBackground,
                    )
                }
                PrimaryButton(
                    modifier = Modifier
                        .run {
                            if (
                                actionsFocusRequester != null &&
                                beforeActionsFocusRequester != null
                            ) {
                                this.focusRequester(cancelButtonFocusRequester)
                                    .focusProperties {
                                        this.previous = beforeActionsFocusRequester
                                        this.next = cancelButtonFocusRequester
                                    }
                            } else {
                                this
                            }
                        },
                    enabled = isPrimaryActionEnabled,
                    onClick = { onPrimaryActionClicked() },
                    text = primaryActionText
                )
            }
        }
    }
}