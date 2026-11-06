package com.hugodehu.gitmiro.domain.models

import com.hugodehu.gitmiro.domain.exceptions.GitMiroException

data class Error(
    val taskType: TaskType,
    val date: Long,
    val exception: Exception,
    val isUnhandled: Boolean,
)


fun newErrorNow(
    taskType: TaskType,
    exception: Exception,
): Error {
    return Error(
        taskType = taskType,
        date = System.currentTimeMillis(),
        exception = exception,
        isUnhandled = exception !is GitMiroException
    )
}
