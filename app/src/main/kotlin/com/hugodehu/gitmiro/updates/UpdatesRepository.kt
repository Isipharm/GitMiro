package com.hugodehu.gitmiro.updates

import com.hugodehu.gitmiro.AppConstants
import com.hugodehu.gitmiro.common.printError
import com.hugodehu.gitmiro.common.printLog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.minutes

private val updateJson = Json {
    this.ignoreUnknownKeys = true
}

// The JDK client verifies certificates, unlike the app's Ktor client
private val httpClient = HttpClient.newHttpClient()

private const val TAG = "UpdatesRepository"

/**
 * The update announced by [AppConstants.VERSION_CHECK_URL] when it's newer than the running app, null otherwise.
 */
suspend fun fetchAvailableUpdate(): Update? = withContext(Dispatchers.IO) {
    val versionCheckUrl = AppConstants.VERSION_CHECK_URL ?: return@withContext null

    printLog(TAG, "Checking for new updates in $versionCheckUrl")

    val response = httpClient.send(HttpRequest.newBuilder(URI(versionCheckUrl)).build(), BodyHandlers.ofString())
    if (response.statusCode() != 200) {
        throw IOException("Update check failed (HTTP ${response.statusCode()})")
    }

    updateJson.decodeFromString<Update>(response.body())
        .takeIf { it.appCode > AppConstants.APP_VERSION_CODE }
}

@Singleton
class UpdatesRepository @Inject constructor() {
    val hasUpdatesFlow = flow {
        if (AppConstants.VERSION_CHECK_URL == null) {
            return@flow
        }

        while (currentCoroutineContext().isActive) {
            try {
                fetchAvailableUpdate()?.let { emit(it) }
            } catch (ex: CancellationException) {
                throw ex
            } catch (ex: Exception) {
                // Offline or blob unreachable: try again at the next check instead of stopping the checks
                printError(TAG, "Update check failed: ${ex.message}", ex)
            }

            delay(5.minutes)
        }
    }
}
