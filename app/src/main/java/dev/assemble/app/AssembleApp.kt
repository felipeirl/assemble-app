package dev.assemble.app

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
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
import dev.assemble.app.feature.character.CharacterPreviewRoute
import dev.assemble.app.feature.character.CharacterPreviewViewModel
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
import dev.assemble.app.feature.splash.SplashScreen
import dev.assemble.app.navigation.About
import dev.assemble.app.navigation.AppDrawer
import dev.assemble.app.navigation.CharacterPreview
import dev.assemble.app.navigation.diagonalTransition
import dev.assemble.app.navigation.rememberDiagonalRevealDecorator
import dev.assemble.app.navigation.CharacterProfile
import dev.assemble.app.navigation.ChatList
import dev.assemble.app.navigation.Conversation
import dev.assemble.app.navigation.Discover
import dev.assemble.app.navigation.DrawerItem
import dev.assemble.app.navigation.EditProfile
import dev.assemble.app.navigation.Help
import dev.assemble.app.navigation.Login
import dev.assemble.app.navigation.Navigator
import dev.assemble.app.navigation.Onboarding
import dev.assemble.app.navigation.Profile
import dev.assemble.app.navigation.Settings
import dev.assemble.app.navigation.Splash
import dev.assemble.app.navigation.TopLevelRoutes
import dev.assemble.app.navigation.rememberNavigationState
import dev.assemble.app.navigation.toEntries
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
}

@Composable
private fun EntryFlow(userRepository: UserRepository) {
    val animationsEnabled = rememberAnimationsEnabled()
    val backStack = rememberNavBackStack(Splash)
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
            entry<Splash> { SplashScreen(onFinished = { backStack[backStack.lastIndex] = Login }) }
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

    BackHandler(enabled = drawerState.isOpen) { scope.launch { drawerState.close() } }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen || navigationState.isAtTopLevel,
        drawerContent = {
            AppDrawer(
                userName = profile.name,
                avatarPreset = AvatarPreset.fromIndex(profile.avatarPreset),
                onItemClick = { item ->
                    scope.launch { drawerState.close() }
                    when (item) {
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
                NavDisplay(
                    entries = navigationState.toEntries(
                        extraDecorator = rememberDiagonalRevealDecorator(animationsEnabled),
                        entryProvider {
                            entry<Discover> {
                                DiscoverRoute(
                                    viewModel = viewModel {
                                        DiscoverViewModel(
                                            characterRepository = container.characterRepository,
                                            userRepository = container.userRepository,
                                            connectionRepository = container.connectionRepository,
                                            assembleService = container.assembleService,
                                        )
                                    },
                                    onOpenMenu = openDrawer,
                                    onOpenPreview = { id -> navigator.navigate(CharacterPreview(id)) },
                                    onAdjustPreferences = { navigator.navigate(Profile) },
                                    onStartChat = { connectionId -> navigator.navigate(Conversation(connectionId)) },
                                )
                            }
                            entry<CharacterPreview> { key ->
                                CharacterPreviewRoute(
                                    viewModel = viewModel {
                                        CharacterPreviewViewModel(
                                            characterId = key.characterId,
                                            characterRepository = container.characterRepository,
                                            userRepository = container.userRepository,
                                            assembleService = container.assembleService,
                                        )
                                    },
                                    onBack = navigator::goBack,
                                )
                            }
                            entry<CharacterProfile> { key ->
                                CharacterProfileRoute(
                                    viewModel = viewModel {
                                        CharacterProfileViewModel(
                                            characterId = key.characterId,
                                            characterRepository = container.characterRepository,
                                            connectionRepository = container.connectionRepository,
                                            userRepository = container.userRepository,
                                        )
                                    },
                                    onBack = navigator::goBack,
                                    onOpenChat = { connectionId -> navigator.navigate(Conversation(connectionId)) },
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
                                        )
                                    },
                                    onOpenMenu = openDrawer,
                                    onEditProfile = { navigator.navigate(EditProfile) },
                                    onOpenCharacter = { id -> navigator.navigate(CharacterProfile(id)) },
                                )
                            }
                            entry<EditProfile> {
                                EditProfileRoute(
                                    viewModel = viewModel { EditProfileViewModel(container.userRepository) },
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
