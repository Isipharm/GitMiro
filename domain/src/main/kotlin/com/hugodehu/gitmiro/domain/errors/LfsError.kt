package com.hugodehu.gitmiro.domain.errors

import io.ktor.http.HttpStatusCode

sealed interface LfsError {
    data class HttpError(val code: HttpStatusCode) : LfsError
}