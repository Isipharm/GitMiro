package com.hugodehu.gitmiro

object AppConstants {
    val openSourceProjects = listOf(
        Project("Google Dagger", "https://dagger.dev/", apache__2_0),
        Project("Compose Multiplatform", "https://www.jetbrains.com/lp/compose-mpp/", apache__2_0),
        Project("JGit", "https://www.eclipse.org/jgit/", edl),
        Project("JUnit 5", "https://junit.org/junit5/", edl),
        Project("Kotlin", "https://kotlinlang.org/", apache__2_0),
        Project(
            "Kotlinx.serialization",
            "https://kotlinlang.org/docs/serialization.html#example-json-serialization",
            apache__2_0
        ),
        Project("Mockk", "https://mockk.io/", apache__2_0),
        Project("LibSSH", "libssh.org/", lgpl__2_1),
    )


    // Remember to update build.gradle when changing this
    const val APP_NAME = "GitMiro"
    const val APP_DESCRIPTION =
        "GitMiro is a modified Gitnuro, a Git client to manage multiple repositories with a modern experience and a live visual representation of their state. It looked at so many Git repositories that it went \"miro\" (French slang for short-sighted)."
    const val APP_VERSION = "1.0.0"
    const val APP_VERSION_CODE = 1
    /**
     * URL of a latest.json (see the one at the root of the repository) that every user can read. Null disables the
     * update checks, as GitMiro has no public release channel yet and Gitnuro's one would announce Gitnuro releases.
     */
    val VERSION_CHECK_URL: String? = null
}


private val apache__2_0 = License("Apache 2.0", "https://www.apache.org/licenses/LICENSE-2.0")
private val edl = License("EDL", "https://www.eclipse.org/org/documents/edl-v10.php")
private val lgpl__2_1 = License("LGPL-2.1", "https://www.gnu.org/licenses/old-licenses/lgpl-2.1.en.html")

data class License(
    val name: String,
    val url: String,
)

data class Project(val name: String, val url: String, val license: License)


