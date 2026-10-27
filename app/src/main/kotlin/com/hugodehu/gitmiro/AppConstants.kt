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
    const val APP_VERSION = "1.0.7"
    const val APP_VERSION_CODE = 8
    /**
     * latest.json (same format as the one at the root of the repository) checked for updates. It must be readable
     * without authentication, so it lives in a public Azure blob instead of this private repository. The release
     * workflow uploads it when the GITMIRO_LATEST_JSON_SAS_URL secret is set. Null disables the update checks.
     */
    val VERSION_CHECK_URL: String? = "https://leodevresources.blob.core.windows.net/public-ressources/GitMiro/latest.json"
    /**
     * Private repository whose releases hold the release notes and the installer that updates download
     */
    const val GITHUB_REPOSITORY = "Isipharm/GitMiro"
}


private val apache__2_0 = License("Apache 2.0", "https://www.apache.org/licenses/LICENSE-2.0")
private val edl = License("EDL", "https://www.eclipse.org/org/documents/edl-v10.php")
private val lgpl__2_1 = License("LGPL-2.1", "https://www.gnu.org/licenses/old-licenses/lgpl-2.1.en.html")

data class License(
    val name: String,
    val url: String,
)

data class Project(val name: String, val url: String, val license: License)


