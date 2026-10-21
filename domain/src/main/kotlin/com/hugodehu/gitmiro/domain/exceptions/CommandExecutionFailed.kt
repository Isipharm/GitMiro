package com.hugodehu.gitmiro.domain.exceptions

class CommandExecutionFailed(msg: String, cause: Exception) : GitnuroException(msg, cause) {
}