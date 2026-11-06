package com.hugodehu.gitmiro.domain.exceptions

abstract class GitMiroException(message: String, cause: Exception? = null) : Exception(message, cause)