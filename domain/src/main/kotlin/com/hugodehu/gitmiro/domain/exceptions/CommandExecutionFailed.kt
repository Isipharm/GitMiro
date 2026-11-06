package com.hugodehu.gitmiro.domain.exceptions

class CommandExecutionFailed(msg: String, cause: Exception) : GitMiroException(msg, cause) {
}