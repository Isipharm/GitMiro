package com.hugodehu.gitmiro.ui.dialogs

import androidx.compose.runtime.Composable
import com.hugodehu.gitmiro.domain.credentials.CredentialsRequest
import com.hugodehu.gitmiro.app.generated.resources.Res
import com.hugodehu.gitmiro.app.generated.resources.key
import com.hugodehu.gitmiro.ui.dialogs.base.PasswordDialog

@Composable
fun GpgPasswordDialog(
    gpgCredentialsRequest: CredentialsRequest.GpgCredentialsRequest,
    onReject: () -> Unit,
    onAccept: (password: String) -> Unit,
) {
    PasswordDialog(
        title = "Introduce your GPG key's password",
        subtitle = "Your GPG key is protected with a password",
        icon = Res.drawable.key,
        cancelButtonText = "Do not sign",
        isRetry = gpgCredentialsRequest.isRetry,
        password = gpgCredentialsRequest.password,
        retryMessage = "Invalid password, please try again",
        onDismiss = onReject,
        onAccept = onAccept,
    )
}