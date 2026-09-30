package dev.assemble.app

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import dev.assemble.app.core.data.UserRepository
import dev.assemble.app.core.designsystem.component.AssembleBottomBar
import dev.assemble.app.core.designsystem.component.AssembleTab
import dev.assemble.app.core.designsystem.theme.AssembleTheme
import dev.assemble.app.core.model.MessageAuthor
import dev.assemble.app.core.model.UserProfile
import dev.assemble.app.feature.about.AboutScreen
import dev.assemble.app.feature.character.CharacterPreviewScreen
import dev.assemble.app.feature.character.CharacterProfileScreen
import dev.assemble.app.feature.chat.ChatListScreen
import dev.assemble.app.feature.chat.ConversationScreen
import dev.assemble.app.feature.discover.DiscoverScreen
import dev.assemble.app.feature.help.HelpScreen
import dev.assemble.app.feature.login.LoginScreen
import dev.assemble.app.feature.onboarding.OnboardingScreen
import dev.assemble.app.feature.profile.EditProfileScreen
import dev.assemble.app.feature.profile.ProfileScreen
import dev.assemble.app.feature.settings.SettingsScreen
import dev.assemble.app.feature.splash.SplashScreen
import dev.assemble.app.navigation.About
import dev.assemble.app.navigation.AppDrawer
import dev.assemble.app.navigation.CharacterPreview
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.IOException

private enum class AppFlow { Entry, Onboarding, Main }

private const val TransitionMillis = 250
private const val SlideDivisor = 5

/** Raiz da UI: escolhe o fluxo pela sessão (entrada → onboarding → app) e hospeda a navegação. */
@Composable
fun AssembleApp(container: AppContainer) {
    val session by container.userRepository.session.collectAsStateWithLifecycle()
    val flow = when {
        !session.isLoggedIn -> AppFlow.Entry
        !session.hasCompletedOnboarding -> AppFlow.Onboarding
        else -> AppFlow.Main
    }
    Crossfade(targetState = flow, animationSpec = tween(TransitionMillis), label = "appFlow") { current ->
        when (current) {
            AppFlow.Entry -> EntryFlow(container.userRepository)
            AppFlow.Onboarding -> OnboardingFlow(container.userRepository)
            AppFlow.Main -> MainFlow(container)
        }
    }
}

@Composable
private fun EntryFlow(userRepository: UserRepository) {
    val scope = rememberCoroutineScope()
    val backStack = rememberNavBackStack(Splash)
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        transitionSpec = { forwardTransition() },
        popTransitionSpec = { backTransition() },
        predictivePopTransitionSpec = { backTransition() },
        entryProvider = entryProvider {
            entry<Splash> { SplashScreen(onFinished = { backStack[backStack.lastIndex] = Login }) }
            entry<Login> { LoginScreen(onContinue = { scope.launchPlaceholderAction { userRepository.logIn() } }) }
        },
    )
}

@Composable
private fun OnboardingFlow(userRepository: UserRepository) {
    val scope = rememberCoroutineScope()
    val backStack = rememberNavBackStack(Onboarding(step = 0))
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        transitionSpec = { forwardTransition() },
        popTransitionSpec = { backTransition() },
        predictivePopTransitionSpec = { backTransition() },
        entryProvider = entryProvider {
            entry<Onboarding> { key ->
                OnboardingScreen(
                    step = key.step,
                    onNext = { backStack.add(Onboarding(key.step + 1)) },
                    onBack = { backStack.removeLastOrNull() },
                    onFinish = {
                        scope.launchPlaceholderAction {
                            userRepository.completeOnboarding(userRepository.preferences.value)
                        }
                    },
                )
            }
        },
    )
}

@Composable
private fun MainFlow(container: AppContainer) {
    val scope = rememberCoroutineScope()
    val navigationState = rememberNavigationState(startRoute = Discover, topLevelRoutes = TopLevelRoutes)
    val navigator = remember(navigationState) { Navigator(navigationState) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val openDrawer: () -> Unit = { scope.launch { drawerState.open() } }

    val userName by remember(container) {
        container.userRepository.observeProfile()
            .map<UserProfile, String?> { it.name }
            .catch { emit(null) }
    }.collectAsStateWithLifecycle(initialValue = null)
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
                userName = userName,
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
                    entryProvider {
                        entry<Discover> {
                            DiscoverScreen(
                                onOpenMenu = openDrawer,
                                onOpenPreview = { id -> navigator.navigate(CharacterPreview(id)) },
                            )
                        }
                        entry<CharacterPreview> { key ->
                            CharacterPreviewScreen(characterId = key.characterId, onBack = navigator::goBack)
                        }
                        entry<CharacterProfile> { key ->
                            CharacterProfileScreen(
                                characterId = key.characterId,
                                onBack = navigator::goBack,
                                onOpenChat = { navigator.navigate(Conversation("connection-${key.characterId}")) },
                            )
                        }
                        entry<ChatList> {
                            ChatListScreen(
                                onOpenMenu = openDrawer,
                                onOpenConversation = { id -> navigator.navigate(Conversation(id)) },
                            )
                        }
                        entry<Conversation> { key ->
                            ConversationScreen(
                                connectionId = key.connectionId,
                                onBack = navigator::goBack,
                                onOpenCharacter = {
                                    navigator.navigate(CharacterProfile(key.connectionId.removePrefix("connection-")))
                                },
                            )
                        }
                        entry<Profile> {
                            ProfileScreen(
                                onOpenMenu = openDrawer,
                                onEditProfile = { navigator.navigate(EditProfile) },
                                onOpenCharacter = { id -> navigator.navigate(CharacterProfile(id)) },
                            )
                        }
                        entry<EditProfile> { EditProfileScreen(onBack = navigator::goBack) }
                        entry<Settings> { SettingsScreen(onBack = navigator::goBack) }
                        entry<About> { AboutScreen(onBack = navigator::goBack) }
                        entry<Help> { HelpScreen(onBack = navigator::goBack) }
                    },
                ),
                onBack = navigator::goBack,
                modifier = Modifier
                    .padding(padding)
                    .consumeWindowInsets(padding),
                transitionSpec = { forwardTransition() },
                popTransitionSpec = { backTransition() },
                predictivePopTransitionSpec = { backTransition() },
            )
        }
    }
}

// Fallback do prompt (fade + slide). O corte diagonal com clipPath fica para a revisão de animações.
private fun <T> AnimatedContentTransitionScope<T>.forwardTransition(): ContentTransform =
    (slideInHorizontally(tween(TransitionMillis)) { it / SlideDivisor } + fadeIn(tween(TransitionMillis))) togetherWith
        fadeOut(tween(TransitionMillis))

private fun <T> AnimatedContentTransitionScope<T>.backTransition(): ContentTransform =
    fadeIn(tween(TransitionMillis)) togetherWith
        (slideOutHorizontally(tween(TransitionMillis)) { it / SlideDivisor } + fadeOut(tween(TransitionMillis)))

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

/** Ações dos placeholders da etapa 5. Falha simulada é ignorada aqui; as telas reais (etapa 6) mostram o erro. */
private fun CoroutineScope.launchPlaceholderAction(action: suspend () -> Unit) {
    launch {
        try {
            action()
        } catch (_: IOException) {
            // Placeholder: sem UI de erro até a etapa 6.
        }
    }
}
