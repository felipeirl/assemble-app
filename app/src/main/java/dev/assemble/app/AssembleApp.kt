package dev.assemble.app

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import dev.assemble.app.core.data.UserRepository
import dev.assemble.app.core.designsystem.component.AssembleBottomBar
import dev.assemble.app.core.designsystem.component.AssembleTab
import dev.assemble.app.core.designsystem.component.AvatarPreset
import dev.assemble.app.core.designsystem.theme.AssembleTheme
import dev.assemble.app.core.designsystem.theme.rememberAnimationsEnabled
import dev.assemble.app.core.model.MessageAuthor
import dev.assemble.app.feature.about.AboutScreen
import dev.assemble.app.feature.achievements.AchievementUnlockHost
import dev.assemble.app.feature.achievements.AchievementsRoute
import dev.assemble.app.feature.achievements.AchievementsViewModel
import dev.assemble.app.feature.character.CharacterPreviewRoute
import dev.assemble.app.feature.character.CharacterPreviewViewModel
import dev.assemble.app.feature.character.PreviewHero
import dev.assemble.app.core.ui.LocalSharedTransitionScope
import dev.assemble.app.feature.character.CharacterProfileRoute
import dev.assemble.app.feature.character.CharacterProfileViewModel
import dev.assemble.app.feature.chat.ChatListRoute
import dev.assemble.app.feature.chat.ChatListViewModel
import dev.assemble.app.feature.chat.ConversationRoute
import dev.assemble.app.feature.chat.ConversationViewModel
import dev.assemble.app.feature.chat.IncomingMessageToastHost
import dev.assemble.app.feature.discover.DiscoverRoute
import dev.assemble.app.feature.discover.DiscoverViewModel
import dev.assemble.app.feature.help.HelpScreen
import dev.assemble.app.feature.login.LoginRoute
import dev.assemble.app.feature.login.LoginViewModel
import dev.assemble.app.feature.onboarding.OnboardingRoute
import dev.assemble.app.feature.onboarding.isLastStep
import dev.assemble.app.feature.onboarding.onboardingStepAt
import dev.assemble.app.feature.onboarding.OnboardingViewModel
import dev.assemble.app.feature.profile.EditProfileRoute
import dev.assemble.app.feature.profile.EditProfileViewModel
import dev.assemble.app.feature.profile.ProfileRoute
import dev.assemble.app.feature.profile.ProfileViewModel
import dev.assemble.app.feature.settings.SettingsRoute
import dev.assemble.app.feature.settings.SettingsViewModel
import dev.assemble.app.navigation.About
import dev.assemble.app.navigation.Achievements
import dev.assemble.app.navigation.AppDrawer
import dev.assemble.app.navigation.CharacterPreview
import dev.assemble.app.navigation.diagonalTransition
import dev.assemble.app.navigation.rememberDiagonalRevealDecorator
import dev.assemble.app.navigation.CharacterProfile
import dev.assemble.app.navigation.ChatList
import dev.assemble.app.navigation.Conversation
import dev.assemble.app.navigation.Discover
import dev.assemble.app.navigation.DrawerItem
import dev.assemble.app.navigation.DrawerStats
import dev.assemble.app.navigation.EditProfile
import dev.assemble.app.navigation.Help
import dev.assemble.app.navigation.Login
import dev.assemble.app.navigation.Navigator
import dev.assemble.app.navigation.NoDiagonalReveal
import dev.assemble.app.navigation.Onboarding
import dev.assemble.app.navigation.Profile
import dev.assemble.app.navigation.Settings
import dev.assemble.app.navigation.TopLevelRoutes
import dev.assemble.app.navigation.rememberNavigationState
import dev.assemble.app.navigation.toEntries
import dev.assemble.app.core.designsystem.component.LocalUserPhoto
import dev.assemble.app.feature.account.AccountDeactivatedHost
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private enum class AppFlow { Entry, Onboarding, Main }

private const val TransitionMillis = 250

/** Raiz da UI: escolhe o fluxo pela sessão (entrada → onboarding → app) e hospeda a navegação. */
@Composable
fun AssembleApp(container: AppContainer) {
    val session by container.userRepository.session.collectAsStateWithLifecycle()
    val flow = when {
        !session.isLoggedIn -> AppFlow.Entry
        !session.hasCompletedOnboarding -> AppFlow.Onboarding
        else -> AppFlow.Main
    }
    val animationsEnabled = rememberAnimationsEnabled()
    Crossfade(
        targetState = flow,
        animationSpec = tween(if (animationsEnabled) TransitionMillis else 0),
        label = "appFlow",
    ) { current ->
        when (current) {
            AppFlow.Entry -> EntryFlow(container.userRepository)
            AppFlow.Onboarding -> OnboardingFlow(container.userRepository)
            AppFlow.Main -> MainFlow(container)
        }
    }
    if (session.isLoggedIn) AccountDeactivatedHost(container.userRepository)
}

@Composable
private fun EntryFlow(userRepository: UserRepository) {
    val animationsEnabled = rememberAnimationsEnabled()
    val backStack = rememberNavBackStack(Login)
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
            rememberDiagonalRevealDecorator(animationsEnabled),
        ),
        transitionSpec = { diagonalTransition(animationsEnabled) },
        popTransitionSpec = { diagonalTransition(animationsEnabled) },
        predictivePopTransitionSpec = { diagonalTransition(animationsEnabled) },
        entryProvider = entryProvider {
            entry<Login> { LoginRoute(viewModel = viewModel { LoginViewModel(userRepository) }) }
        },
    )
}

@Composable
private fun OnboardingFlow(userRepository: UserRepository) {
    val animationsEnabled = rememberAnimationsEnabled()
    // Um ViewModel para os 5 passos: as escolhas sobrevivem ao ir e voltar entre eles.
    val onboardingViewModel = viewModel { OnboardingViewModel(userRepository) }
    val backStack = rememberNavBackStack(Onboarding(step = 0))
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
            rememberDiagonalRevealDecorator(animationsEnabled),
        ),
        transitionSpec = { diagonalTransition(animationsEnabled) },
        popTransitionSpec = { diagonalTransition(animationsEnabled) },
        predictivePopTransitionSpec = { diagonalTransition(animationsEnabled) },
        entryProvider = entryProvider {
            entry<Onboarding> { key ->
                val step = onboardingStepAt(key.step)
                OnboardingRoute(
                    step = step,
                    viewModel = onboardingViewModel,
                    onNext = { if (!step.isLastStep) backStack.add(Onboarding(key.step + 1)) },
                    onBack = { backStack.removeLastOrNull() },
                )
            }
        },
    )
}

@Composable
private fun MainFlow(container: AppContainer) {
    val profile by container.userRepository.currentProfile.collectAsStateWithLifecycle()
    CompositionLocalProvider(LocalUserPhoto provides profile.photo) { MainFlowContent(container) }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun MainFlowContent(container: AppContainer) {
    val animationsEnabled = rememberAnimationsEnabled()
    val scope = rememberCoroutineScope()
    val navigationState = rememberNavigationState(startRoute = Discover, topLevelRoutes = TopLevelRoutes)
    val navigator = remember(navigationState) { Navigator(navigationState) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val openDrawer: () -> Unit = { scope.launch { drawerState.open() } }

    val profile by container.userRepository.currentProfile.collectAsStateWithLifecycle()
    val unreadChats by remember(container) {
        container.chatRepository.allMessages.map { messages ->
            messages.count { it.author == MessageAuthor.Character && !it.read }
        }
    }.collectAsStateWithLifecycle(initialValue = 0)
    val connectionCount by remember(container) {
        container.connectionRepository.observeConnections().map { it.size }
    }.collectAsStateWithLifecycle(initialValue = 0)
    val seenIds by container.userRepository.seenCharacterIds.collectAsStateWithLifecycle()

    BackHandler(enabled = drawerState.isOpen) { scope.launch { drawerState.close() } }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen || navigationState.isAtTopLevel,
        drawerContent = {
            AppDrawer(
                userName = profile.name,
                avatarPreset = AvatarPreset.fromIndex(profile.avatarPreset),
                stats = DrawerStats(connections = connectionCount, seen = seenIds.size),
                // Os itens entram em cascata assim que o menu começa a abrir.
                revealItems = drawerState.targetValue == DrawerValue.Open,
                onItemClick = { item ->
                    scope.launch { drawerState.close() }
                    when (item) {
                        DrawerItem.Achievements -> navigator.navigate(Achievements)
                        DrawerItem.Settings -> navigator.navigate(Settings)
                        DrawerItem.About -> navigator.navigate(About)
                        DrawerItem.Help -> navigator.navigate(Help)
                        DrawerItem.LogOut -> scope.launch { container.userRepository.logOut() }
                    }
                },
            )
        },
    ) {
        Box(Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = AssembleTheme.colors.bg,
                contentWindowInsets = WindowInsets(0),
                bottomBar = {
                    if (navigationState.isAtTopLevel) {
                        AssembleBottomBar(
                            selectedTab = navigationState.topLevelRoute.toTab(),
                            onTabSelected = { tab -> navigator.navigate(tab.toRoute()) },
                            unreadChats = unreadChats,
                        )
                    }
                },
            ) { padding ->
                SharedTransitionLayout {
                CompositionLocalProvider(LocalSharedTransitionScope provides this) {
                NavDisplay(
                    entries = navigationState.toEntries(
                        extraDecorator = rememberDiagonalRevealDecorator(animationsEnabled) { navigationState.lastWasTabSwitch },
                        entryProvider {
                            entry<Discover> {
                                DiscoverRoute(
                                    viewModel = viewModel {
                                        DiscoverViewModel(
                                            deckSource = container.deckSource,
                                            userRepository = container.userRepository,
                                            assembleService = container.assembleService,
                                        )
                                    },
                                    onOpenMenu = openDrawer,
                                    onOpenPreview = { card -> navigator.navigate(CharacterPreview(card.characterId, card.name, card.imageUrl)) },
                                    onAdjustPreferences = { navigator.navigate(Profile) },
                                    onStartChat = { connectionId -> navigator.navigate(Conversation(connectionId)) },
                                )
                            }
                            entry<CharacterPreview>(metadata = NoDiagonalReveal) { key ->
                                CharacterPreviewRoute(
                                    viewModel = viewModel {
                                        CharacterPreviewViewModel(
                                            characterId = key.characterId,
                                            details = container.characterDetails,
                                            assembleService = container.assembleService,
                                        )
                                    },
                                    hero = PreviewHero(key.characterId, key.name, key.imageUrl),
                                    onBack = navigator::goBack,
                                )
                            }
                            entry<CharacterProfile> { key ->
                                CharacterProfileRoute(
                                    viewModel = viewModel {
                                        CharacterProfileViewModel(
                                            characterId = key.characterId,
                                            details = container.characterDetails,
                                        )
                                    },
                                    onBack = navigator::goBack,
                                    onOpenChat = { connectionId -> navigator.navigate(Conversation(connectionId)) },
                                    onOpenCharacter = { id -> navigator.navigate(CharacterProfile(id)) },
                                )
                            }
                            entry<ChatList> {
                                ChatListRoute(
                                    viewModel = viewModel {
                                        ChatListViewModel(
                                            characterRepository = container.characterRepository,
                                            connectionRepository = container.connectionRepository,
                                            chatRepository = container.chatRepository,
                                        )
                                    },
                                    onOpenMenu = openDrawer,
                                    onOpenConversation = { id -> navigator.navigate(Conversation(id)) },
                                    onGoToDiscover = { navigator.navigate(Discover) },
                                )
                            }
                            entry<Conversation> { key ->
                                ConversationRoute(
                                    viewModel = viewModel {
                                        ConversationViewModel(
                                            connectionId = key.connectionId,
                                            characterRepository = container.characterRepository,
                                            connectionRepository = container.connectionRepository,
                                            chatRepository = container.chatRepository,
                                            applicationScope = container.applicationScope,
                                        )
                                    },
                                    onBack = navigator::goBack,
                                    onOpenCharacter = { characterId -> navigator.navigate(CharacterProfile(characterId)) },
                                )
                            }
                            entry<Profile> {
                                ProfileRoute(
                                    viewModel = viewModel {
                                        ProfileViewModel(
                                            characterRepository = container.characterRepository,
                                            connectionRepository = container.connectionRepository,
                                            userRepository = container.userRepository,
                                            achievementTracker = container.achievementTracker,
                                        )
                                    },
                                    onOpenMenu = openDrawer,
                                    onEditProfile = { navigator.navigate(EditProfile) },
                                    onOpenCharacter = { id -> navigator.navigate(CharacterProfile(id)) },
                                )
                            }
                            entry<EditProfile> {
                                EditProfileRoute(
                                    viewModel = viewModel {
                                        EditProfileViewModel(
                                            userRepository = container.userRepository,
                                            characterRepository = container.characterRepository,
                                            connectionRepository = container.connectionRepository,
                                            achievementTracker = container.achievementTracker,
                                        )
                                    },
                                    onBack = navigator::goBack,
                                )
                            }
                            entry<Settings> {
                                SettingsRoute(
                                    viewModel = viewModel {
                                        SettingsViewModel(
                                            userRepository = container.userRepository,
                                            connectionRepository = container.connectionRepository,
                                            chatRepository = container.chatRepository,
                                        )
                                    },
                                    onBack = navigator::goBack,
                                )
                            }
                            entry<About> { AboutScreen(onBack = navigator::goBack) }
                            entry<Help> { HelpScreen(onBack = navigator::goBack) }
                            entry<Achievements> {
                                AchievementsRoute(
                                    viewModel = viewModel { AchievementsViewModel(container.achievementTracker) },
                                    onBack = navigator::goBack,
                                )
                            }
                        },
                    ),
                    onBack = navigator::goBack,
                    modifier = Modifier
                        .padding(padding)
                        .consumeWindowInsets(padding),
                    transitionSpec = { diagonalTransition(animationsEnabled) },
                    popTransitionSpec = { diagonalTransition(animationsEnabled) },
                    predictivePopTransitionSpec = { diagonalTransition(animationsEnabled) },
                )
                }
                }
            }
            AchievementUnlockHost(
                tracker = container.achievementTracker,
                onOpen = { navigator.navigate(Achievements) },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(top = AssembleTheme.spacing.space2),
            )
            IncomingMessageToastHost(
                container = container,
                openConnectionId = (navigationState.currentRoute as? Conversation)?.connectionId,
                onOpenConversation = { id -> navigator.navigate(Conversation(id)) },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(top = AssembleTheme.spacing.space2),
            )
        }
    }
}

private fun NavKey.toTab(): AssembleTab = when (this) {
    ChatList -> AssembleTab.Chat
    Profile -> AssembleTab.Profile
    else -> AssembleTab.Discover
}

private fun AssembleTab.toRoute(): NavKey = when (this) {
    AssembleTab.Discover -> Discover
    AssembleTab.Chat -> ChatList
    AssembleTab.Profile -> Profile
}
