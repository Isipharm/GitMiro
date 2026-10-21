package com.hugodehu.gitmiro.ui.dialogs

import androidx.compose.runtime.Composable
import com.hugodehu.gitmiro.app.generated.resources.Res
import com.hugodehu.gitmiro.app.generated.resources.lock
import com.hugodehu.gitmiro.domain.credentials.CredentialsRequest
import com.hugodehu.gitmiro.ui.dialogs.base.PasswordDialog

@Composable
fun SshPasswordDialog(
    onReject: () -> Unit,
    onAccept: (password: String) -> Unit,
    credentialsRequest: CredentialsRequest.SshCredentialsRequest,
) {
    PasswordDialog(
        title = "Introduce your SSH key's password",
        subtitle = "Your SSH key is protected with a password",
        isRetry = credentialsRequest.isRetry,
        password = credentialsRequest.password,
        retryMessage = "Invalid password, please try again",
        icon = Res.drawable.lock,
        onDismiss = onReject,
        onAccept = onAccept,
    )
}