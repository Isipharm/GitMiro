@file:OptIn(ExperimentalComposeUiApi::class)

package com.hugodehu.gitmiro.ui

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hugodehu.gitmiro.AppConstants
import com.hugodehu.gitmiro.extensions.*
import com.hugodehu.gitmiro.app.generated.resources.*
import com.hugodehu.gitmiro.domain.extensions.dirName
import com.hugodehu.gitmiro.domain.extensions.dirPath
import com.hugodehu.gitmiro.domain.extensions.lowercaseContains
import com.hugodehu.gitmiro.keybindings.KeybindingOption
import com.hugodehu.gitmiro.keybindings.matchesBinding
import com.hugodehu.gitmiro.theme.AppTheme
import com.hugodehu.gitmiro.theme.backgroundSelected
import com.hugodehu.gitmiro.theme.onBackgroundSecondary
import com.hugodehu.gitmiro.theme.textButtonColors
import com.hugodehu.gitmiro.ui.components.AdjustableOutlinedTextField
import com.hugodehu.gitmiro.ui.components.BottomInfoBar
import com.hugodehu.gitmiro.ui.components.GhostButton
import com.hugodehu.gitmiro.ui.components.PrimaryButton
import com.hugodehu.gitmiro.ui.components.tooltip.DelayedTooltip
import com.hugodehu.gitmiro.ui.components.tooltip.InstantTooltip
import com.hugodehu.gitmiro.ui.dialogs.AppInfoDialog
import com.hugodehu.gitmiro.updates.Update
import com.hugodehu.gitmiro.viewmodels.RepositoryTabViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource


@Composable
fun WelcomePage(
    repositoryTabViewModel: RepositoryTabViewModel,
    onShowCloneDialog: () -> Unit,
    onShowSettings: () -> Unit,
) {
    val recentlyOpenedRepositories by repositoryTabViewModel.appStateManager.latestOpenedRepositoriesPaths.collectAsState()
    val newUpdate by repositoryTabViewModel.update.collectAsState()

    WelcomeView(
        recentlyOpenedRepositories,
        newUpdate,
        onShowCloneDialog = onShowCloneDialog,
        onShowSettings = onShowSettings,
        onOpenRepository = {
            val repo = repositoryTabViewModel.openDirectoryPicker()

            if (repo != null) {
                repositoryTabViewModel.openRepository(repo)
            }
        },
        onStartRepository = {
            val dir = repositoryTabViewModel.openDirectoryPicker()

            if (dir != null) {
                repositoryTabViewModel.initLocalRepository(dir)
            }
        },
        onOpenKnownRepository = { repositoryTabViewModel.openRepository(it) },
        onOpenUrlInBrowser = { repositoryTabViewModel.openUrlInBrowser(it) },
        onRemoveRepositoryFromRecent = { repositoryTabViewModel.removeRepositoryFromRecent(it) }

    )
}

@Preview
@Composable
fun WelcomeViewPreview() {
    AppTheme(
        customTheme = null
    ) {
        val recentRepositories = (0..10).map {
            "/home/user/sample$it"
        }
        WelcomeView(
            recentlyOpenedRepositories = recentRepositories,
            newUpdate = null,
            onShowCloneDialog = {},
            onShowSettings = {},
            onOpenRepository = {},
            onOpenKnownRepository = {},
            onStartRepository = {},
            onOpenUrlInBrowser = {},
            onRemoveRepositoryFromRecent = {},
        )
    }
}

@Composable
fun WelcomeView(
    recentlyOpenedRepositories: List<String>,
    newUpdate: Update?,
    onShowCloneDialog: () -> Unit,
    onShowSettings: () -> Unit,
    onOpenRepository: () -> Unit,
    onOpenKnownRepository: (String) -> Unit,
    onStartRepository: () -> Unit,
    onOpenUrlInBrowser: (String) -> Unit,
    onRemoveRepositoryFromRecent: (String) -> Unit,
) {

    var showAdditionalInfo by remember { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }
    val welcomeViewFocusRequester = remember { FocusRequester() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .focusable(true)
            .focusRequester(welcomeViewFocusRequester)
            .background(MaterialTheme.colors.background)
            .onPreviewKeyEvent {
                when {
                    it.matchesBinding(KeybindingOption.OPEN_REPOSITORY) -> {
                        onOpenRepository()
                        true
                    }

                    it.matchesBinding(KeybindingOption.SETTINGS) -> {
                        onShowSettings()
                        true
                    }

                    else -> false
                }
            },
    ) {

        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier.width(520.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Open a repository",
                        style = MaterialTheme.typography.h4,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colors.onBackground,
                    )
                    Text(
                        text = "Pick a folder, clone from a URL or reopen a recent repository.",
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onBackgroundSecondary,
                    )
                }

                HomeButtons(
                    onOpenRepository = onOpenRepository,
                    onStartRepository = onStartRepository,
                    onShowCloneView = onShowCloneDialog,
                    onShowAdditionalInfo = { showAdditionalInfo = true },
                    onShowSettings = onShowSettings,
                    onOpenUrlInBrowser = onOpenUrlInBrowser,
                )

                RecentRepositories(
                    recentlyOpenedRepositories,
                    canRepositoriesBeRemoved = true,
                    onOpenKnownRepository = onOpenKnownRepository,
                    onRemoveRepositoryFromRecent = onRemoveRepositoryFromRecent,
                    searchFieldFocusRequester = searchFocusRequester,
                )
            }
        }

        BottomInfoBar(
            newUpdate = newUpdate,
            onOpenUrlInBrowser = onOpenUrlInBrowser,
        )
    }

    LaunchedEffect(recentlyOpenedRepositories.isEmpty()) {
        if (recentlyOpenedRepositories.isEmpty()) {
            welcomeViewFocusRequester.requestFocus()
        } else {
            searchFocusRequester.requestFocus()
        }
    }

    if (showAdditionalInfo) {
        AppInfoDialog(
            onClose = { showAdditionalInfo = false },
            onOpenUrlInBrowser = onOpenUrlInBrowser
        )
    }
}

@Composable
fun HomeButtons(
    onOpenRepository: () -> Unit,
    onStartRepository: () -> Unit,
    onShowCloneView: () -> Unit,
    onShowAdditionalInfo: () -> Unit,
    onShowSettings: () -> Unit,
    onOpenUrlInBrowser: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryButton(
                text = stringResource(Res.string.home_button_open_repository),
                onClick = onOpenRepository,
                modifier = Modifier.height(40.dp),
            )
            GhostButton(
                text = stringResource(Res.string.home_button_clone_repository),
                onClick = onShowCloneView,
                modifier = Modifier.height(40.dp),
            )
            GhostButton(
                text = stringResource(Res.string.home_button_start_repository),
                onClick = onStartRepository,
                modifier = Modifier.height(40.dp),
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LinkText(stringResource(Res.string.home_button_settings), onShowSettings)
            LinkText(stringResource(Res.string.home_button_additional_information), onShowAdditionalInfo)
            LinkText(stringResource(Res.string.home_button_report_bug)) {
                onOpenUrlInBrowser("https://github.com/JetpackDuba/Gitnuro/issues")
            }
            LinkText(stringResource(Res.string.home_button_source_code)) {
                onOpenUrlInBrowser("https://github.com/JetpackDuba/Gitnuro")
            }
        }
    }
}

@Composable
private fun LinkText(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        style = MaterialTheme.typography.caption,
        color = MaterialTheme.colors.onBackgroundSecondary,
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .handMouseClickable(onClick = onClick)
            .padding(vertical = 4.dp),
    )
}

@Composable
fun RecentRepositories(
    recentlyOpenedRepositories: List<String>,
    canRepositoriesBeRemoved: Boolean,
    onRemoveRepositoryFromRecent: (String) -> Unit,
    onOpenKnownRepository: (String) -> Unit,
    searchFieldFocusRequester: FocusRequester,
) {
    Column(modifier = Modifier.fillMaxWidth().heightIn(max = 340.dp)) {
        if (recentlyOpenedRepositories.isEmpty()) {
            Text(
                stringResource(Res.string.home_recent_repositories_list_empty),
                color = MaterialTheme.colors.onBackgroundSecondary,
                style = MaterialTheme.typography.body1,
                modifier = Modifier.padding(top = 16.dp)
            )
        } else {
            RecentRepositoriesList(
                recentlyOpenedRepositories = recentlyOpenedRepositories,
                canRepositoriesBeRemoved = canRepositoriesBeRemoved,
                onRemoveRepositoryFromRecent = onRemoveRepositoryFromRecent,
                onOpenKnownRepository = onOpenKnownRepository,
                searchFieldFocusRequester = searchFieldFocusRequester,
            )
        }
    }
}

@Composable
fun RecentRepositoriesList(
    recentlyOpenedRepositories: List<String>,
    canRepositoriesBeRemoved: Boolean,
    searchFieldFocusRequester: FocusRequester,
    onRemoveRepositoryFromRecent: (String) -> Unit,
    onOpenKnownRepository: (String) -> Unit,
    onExitClicked: () -> Unit = {},
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var filter by remember {
        mutableStateOf("")
    }

    var focusedItemIndex by remember { mutableStateOf(0) }

    var isSearchFocused by remember { mutableStateOf(false) }

    val filteredRepositories = remember(filter, recentlyOpenedRepositories) {
        if (filter.isBlank()) {
            recentlyOpenedRepositories
        } else {
            recentlyOpenedRepositories.filter { repository ->
                repository.lowercaseContains(filter)
            }
        }
    }

    Column(
        modifier = Modifier.onPreviewKeyEvent {
            if (it.type != KeyEventType.KeyDown) {
                return@onPreviewKeyEvent false
            }

            when {
                it.matchesBinding(KeybindingOption.DOWN) -> {
                    if (focusedItemIndex < filteredRepositories.lastIndex) {
                        focusedItemIndex += 1
                        scope.launch { listState.animateScrollToItem(focusedItemIndex) }
                    }
                    true
                }

                it.matchesBinding(KeybindingOption.UP) -> {
                    if (focusedItemIndex > 0) {
                        focusedItemIndex -= 1
                        scope.launch { listState.animateScrollToItem(focusedItemIndex) }
                    }
                    true
                }

                it.matchesBinding(KeybindingOption.SIMPLE_ACCEPT) -> {
                    val repo = filteredRepositories.getOrNull(focusedItemIndex)
                    if (repo != null && isSearchFocused) {
                        onOpenKnownRepository(repo)
                    }
                    true
                }

                it.matchesBinding(KeybindingOption.EXIT) -> {
                    if (filter.isEmpty()) {
                        onExitClicked()
                        true
                    } else {
                        false
                    }
                }

                else -> {
                    false
                }
            }
        }
    ) {
        AdjustableOutlinedTextField(
            modifier = Modifier
                .focusRequester(searchFieldFocusRequester)
                .onFocusChanged { isSearchFocused = it.isFocused }
                .onPreviewKeyEvent { keyEvent ->
                    if (keyEvent.matchesBinding(KeybindingOption.EXIT) && keyEvent.type == KeyEventType.KeyDown) {
                        filter = ""
                        true
                    } else
                        false
                },
            value = filter,
            onValueChange = { filter = it },
            singleLine = true,
            hint = stringResource(Res.string.home_recent_repositories_search_label),
            trailingIcon = {
                if (filter.isNotEmpty()) {
                    IconButton(
                        onClick = { filter = "" },
                        modifier = Modifier
                            .size(16.dp)
                            .handOnHover(),
                    ) {
                        Icon(
                            painterResource(Res.drawable.close),
                            contentDescription = null,
                            tint = if (filter.isEmpty()) MaterialTheme.colors.onBackgroundSecondary else MaterialTheme.colors.onBackground
                        )
                    }
                }
            }
        )

        LaunchedEffect(filteredRepositories) {
            if (filter.isNotEmpty() && filteredRepositories.isNotEmpty()) {
                focusedItemIndex = 0
            }
        }

        Text(
            text = "RECENT",
            style = MaterialTheme.typography.caption,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colors.onBackgroundSecondary,
            modifier = Modifier.padding(start = 8.dp, top = 14.dp, bottom = 6.dp),
        )

        Box(modifier = Modifier.padding(top = 4.dp)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth(),
            ) {
                itemsIndexed(items = filteredRepositories) { index, repo ->
                    val repoDirName = repo.dirName
                    val repoDirPath = repo.dirPath
                    val hoverInteraction = remember { MutableInteractionSource() }
                    val isHovered by hoverInteraction.collectIsHoveredAsState()

                    LaunchedEffect(isHovered) {
                        if (isHovered) {
                            focusedItemIndex = index
                        }
                    }

                    Row(
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.small)
                            .fillMaxWidth()
                            .hoverable(hoverInteraction)
                            .handMouseClickable { onOpenKnownRepository(repo) }
                            .backgroundIf(
                                isSearchFocused && index == focusedItemIndex,
                                MaterialTheme.colors.primary.copy(alpha = 0.10f)
                            )
                            .height(44.dp)
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                text = repoDirName,
                                style = MaterialTheme.typography.body2,
                                maxLines = 1,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colors.onBackground,
                                overflow = TextOverflow.Ellipsis,
                            )

                            InstantTooltip(repoDirPath) {
                                Text(
                                    text = if (repoDirPath.startsWith(System.getProperty("user.home"))) {
                                        "~${repoDirPath.removePrefix(System.getProperty("user.home"))}"
                                    } else {
                                        repoDirPath
                                    },
                                    style = MaterialTheme.typography.caption,
                                    fontFamily = com.hugodehu.gitmiro.theme.monoTypography(),
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier,
                                    maxLines = 1,
                                    color = MaterialTheme.colors.onBackgroundSecondary
                                )
                            }
                        }


                        val buttonAlpha = if (canRepositoriesBeRemoved && isHovered) {
                            1f
                        } else {
                            0f
                        }

                        DelayedTooltip(
                            text = stringResource(Res.string.home_recent_repositories_remove_repository),
                        ) {
                            IconButton(
                                onClick = { onRemoveRepositoryFromRecent(repo) },
                                enabled = canRepositoriesBeRemoved && isHovered,
                                modifier = Modifier.alpha(buttonAlpha)
                                    .size(24.dp)
                                    .handOnHover(),
                            ) {
                                Icon(
                                    painterResource(Res.drawable.close),
                                    contentDescription = stringResource(Res.string.home_recent_repositories_remove_repository),
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colors.onBackgroundSecondary
                                )
                            }
                        }
                    }
                }
            }

            VerticalScrollbar(
                rememberScrollbarAdapter(listState),
                modifier = Modifier.align(Alignment.CenterEnd)
                    .fillMaxHeight(),
            )
        }
    }
}
