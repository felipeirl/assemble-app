package dev.assemble.app.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.assemble.app.R
import dev.assemble.app.core.designsystem.component.AssembleTopBar
import dev.assemble.app.core.designsystem.component.CharacterAvatar
import dev.assemble.app.core.designsystem.component.StateView
import dev.assemble.app.core.designsystem.component.StateViewType
import dev.assemble.app.core.designsystem.component.TopBarNavigation
import dev.assemble.app.core.designsystem.component.TopBarTitle
import dev.assemble.app.core.designsystem.icon.AssembleIcons
import dev.assemble.app.core.designsystem.theme.AssembleTheme
import java.time.Duration
import java.time.Instant

private val UnreadDotSize = 10.dp

@Composable
fun ChatListRoute(
    viewModel: ChatListViewModel,
    onOpenMenu: () -> Unit,
    onOpenConversation: (connectionId: String) -> Unit,
    onGoToDiscover: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ChatListScreen(
        state = state,
        onOpenMenu = onOpenMenu,
        onOpenConversation = onOpenConversation,
        onGoToDiscover = onGoToDiscover,
        onRetry = viewModel::retry,
        modifier = modifier,
    )
}

@Composable
fun ChatListScreen(
    state: ChatListUiState,
    onOpenMenu: () -> Unit,
    onOpenConversation: (connectionId: String) -> Unit,
    onGoToDiscover: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    now: Instant = Instant.now(),
) {
    Scaffold(
        modifier = modifier,
        containerColor = AssembleTheme.colors.bg,
        topBar = {
            AssembleTopBar(
                title = TopBarTitle.Text(stringResource(R.string.nav_chat)),
                navigation = TopBarNavigation.Menu(onOpenMenu),
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            when (state) {
                ChatListUiState.Loading -> StateView(StateViewType.Loading(), modifier = Modifier.fillMaxWidth())
                ChatListUiState.Error -> StateView(
                    StateViewType.Error(
                        title = stringResource(R.string.chat_list_error_title),
                        message = stringResource(R.string.state_error_connection),
                        onRetry = onRetry,
                    ),
                )
                ChatListUiState.Empty -> StateView(
                    StateViewType.Empty(
                        icon = AssembleIcons.Chat,
                        title = stringResource(R.string.chat_list_empty_title),
                        message = stringResource(R.string.chat_list_empty_message),
                        actionLabel = stringResource(R.string.chat_list_go_to_discover),
                        onAction = onGoToDiscover,
                    ),
                )
                is ChatListUiState.Content -> LazyColumn(Modifier.fillMaxSize()) {
                    items(state.conversations, key = { it.connectionId }) { conversation ->
                        ConversationRow(conversation, now = now, onClick = { onOpenConversation(conversation.connectionId) })
                        HorizontalDivider(color = AssembleTheme.colors.border)
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationRow(conversation: ConversationSummary, now: Instant, onClick: () -> Unit) {
    val colors = AssembleTheme.colors
    val spacing = AssembleTheme.spacing
    val unreadLabel = stringResource(R.string.chat_list_unread)
    val weight = if (conversation.unread) FontWeight.SemiBold else FontWeight.Normal
    val time = remember(conversation.lastActivity, now) { formatConversationTime(conversation.lastActivity, now) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { if (conversation.unread) stateDescription = unreadLabel }
            .padding(horizontal = spacing.space4, vertical = spacing.space3),
        horizontalArrangement = Arrangement.spacedBy(spacing.space3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CharacterAvatar(name = conversation.name, imageUrl = conversation.imageUrl)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.space1)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.space2)) {
                Text(
                    text = conversation.name,
                    style = AssembleTheme.typography.body.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                ScorePill(conversation.score)
            }
            conversation.lastMessage?.let {
                Text(
                    text = it,
                    style = AssembleTheme.typography.caption.copy(fontWeight = weight),
                    color = if (conversation.unread) colors.text else colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(spacing.space1)) {
            Text(text = time, style = AssembleTheme.typography.small, color = colors.textMuted)
            if (conversation.unread) {
                Box(Modifier.size(UnreadDotSize).background(colors.actionAssemble, AssembleTheme.shapes.pill))
            }
        }
    }
}

/** % exata da conexão em pill score (texto midnight; nunca score como cor de texto). */
@Composable
private fun ScorePill(score: Int) {
    Text(
        text = stringResource(R.string.score_ring_description, score),
        style = AssembleTheme.typography.small.copy(fontWeight = FontWeight.Bold),
        color = AssembleTheme.colors.onScore,
        modifier = Modifier
            .background(AssembleTheme.colors.score, AssembleTheme.shapes.pill)
            .padding(horizontal = AssembleTheme.spacing.space2, vertical = 2.dp),
    )
}

private val PreviewNow: Instant = Instant.parse("2026-09-30T15:00:00Z")

@PreviewLightDark
@Composable
private fun ChatListPreview() {
    AssembleTheme {
        ChatListScreen(
            state = ChatListUiState.Content(
                listOf(
                    ConversationSummary(
                        "c1", "storm", "Storm", null, 67,
                        "The wind carried your name here. Tell me what you're looking for.",
                        PreviewNow - Duration.ofHours(2), unread = true,
                    ),
                    ConversationSummary(
                        "c2", "spider-man", "Spider-Man", null, 82,
                        "Honestly? Curiosity and a very radioactive field trip.",
                        PreviewNow - Duration.ofDays(1), unread = false,
                    ),
                ),
            ),
            onOpenMenu = {}, onOpenConversation = {}, onGoToDiscover = {}, onRetry = {},
            now = PreviewNow,
        )
    }
}

@PreviewLightDark
@Composable
private fun ChatListEmptyPreview() {
    AssembleTheme {
        ChatListScreen(ChatListUiState.Empty, onOpenMenu = {}, onOpenConversation = {}, onGoToDiscover = {}, onRetry = {})
    }
}
