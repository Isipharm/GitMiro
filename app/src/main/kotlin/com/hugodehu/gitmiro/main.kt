package com.hugodehu.gitmiro

import com.hugodehu.gitmiro.data.repositories.configuration.initPreferencesPath
import com.hugodehu.gitmiro.di.DaggerAppComponent
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security


suspend fun main(args: Array<String>) {
    if (args.contains("--graalvm")) {
        val currentDir = System.getProperty("user.dir")

        System.setProperty("java.home", currentDir)
    }

    Security.addProvider(BouncyCastleProvider())

    ignoreDisposedComposeSceneErrors()

    initPreferencesPath()

    val app: App = DaggerAppComponent
        .create()
        .app()

    app.start(args)
}

/**
 * Compose can deliver a mouse event to a popup that was just disposed, for example when switching tabs while a menu or
 * a tooltip is open, and fails with "RootNodeOwner is already disposed". It leaves the app working, so it is dropped
 * instead of being printed as a crash. Any other error goes to the handler that was in place.
 */
private fun ignoreDisposedComposeSceneErrors() {
    val previousHandler = Thread.getDefaultUncaughtExceptionHandler()

    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        val isDisposedScene = throwable is IllegalArgumentException &&
                throwable.message?.contains("RootNodeOwner is already disposed") == true

        if (!isDisposedScene) {
            if (previousHandler != null) {
                previousHandler.uncaughtException(thread, throwable)
            } else {
                throwable.printStackTrace()
            }
        }
    }
}
