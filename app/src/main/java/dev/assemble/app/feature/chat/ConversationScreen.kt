package dev.assemble.app.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.assemble.app.R
import dev.assemble.app.core.designsystem.component.AiLabelChip
import dev.assemble.app.core.designsystem.component.CharacterAvatar
import dev.assemble.app.core.designsystem.component.ChatAuthor
import dev.assemble.app.core.designsystem.component.ChatBubble
import dev.assemble.app.core.designsystem.component.StateView
import dev.assemble.app.core.designsystem.component.StateViewType
import dev.assemble.app.core.designsystem.component.TypingIndicator
import dev.assemble.app.core.designsystem.component.scaledTopBarHeight
import dev.assemble.app.core.designsystem.icon.AssembleIcons
import dev.assemble.app.core.designsystem.theme.AssembleTheme
import dev.assemble.app.core.model.Message
import dev.assemble.app.core.model.MessageAuthor
import dev.assemble.app.core.model.MessageStatus
import java.time.Instant

private const val SendingAlpha = 0.6f
private const val TypingItemKey = "typing"
private val TopBarAvatarSize = 36.dp
private val ErrorIconSize = 16.dp
private val MinTouchTarget = 48.dp

@Composable
fun ConversationRoute(
    viewModel: ConversationViewModel,
    onBack: () -> Unit,
    onOpenCharacter: (characterId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val hasUnread = (state as? ConversationUiState.Content)?.hasUnread == true
    LaunchedEffect(hasUnread) {
        if (hasUnread) viewModel.markRead()
    }
    ConversationScreen(
        state = state,
        draft = draft,
        onDraftChange = viewModel::onDraftChange,
        onSend = viewModel::send,
        onRetryMessage = viewModel::retry,
        onReload = viewModel::reload,
        onBack = onBack,
        onOpenCharacter = onOpenCharacter,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    state: ConversationUiState,
    draft: String,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onRetryMessage: (messageId: String) -> Unit,
    onReload: () -> Unit,
    onBack: () -> Unit,
    onOpenCharacter: (characterId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AssembleTheme.colors
    val content = state as? ConversationUiState.Content
    Scaffold(
        modifier = modifier,
        containerColor = colors.bg,
        topBar = {
            TopAppBar(
                expandedHeight = scaledTopBarHeight(),
                title = { if (content != null) ConversationTitle(content, onOpenCharacter) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(AssembleIcons.Back, contentDescription = stringResource(R.string.top_bar_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.bg,
                    navigationIconContentColor = colors.text,
                    titleContentColor = colors.text,
                ),
            )
        },
        bottomBar = {
            if (content != null) MessageInput(draft = draft, onDraftChange = onDraftChange, onSend = onSend)
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            when (state) {
                ConversationUiState.Loading -> StateView(StateViewType.Loading(), modifier = Modifier.fillMaxWidth())
                ConversationUiState.Error -> StateView(
                    StateViewType.Error(
                        title = stringResource(R.string.conversation_error_title),
                        message = stringResource(R.string.state_error_connection),
                        onRetry = onReload,
                    ),
                )
                ConversationUiState.Unavailable -> StateView(
                    StateViewType.Unavailable(
                        title = stringResource(R.string.character_unavailable_title),
                        message = stringResource(R.string.character_unavailable_message),
                    ),
                )
                is ConversationUiState.Content -> MessageList(state, onRetryMessage)
            }
        }
    }
}

@Composable
private fun ConversationTitle(content: ConversationUiState.Content, onOpenCharacter: (String) -> Unit) {
    val spacing = AssembleTheme.spacing
    Row(
        modifier = Modifier
            .defaultMinSize(minHeight = MinTouchTarget)
            .clickable(role = Role.Button, onClick = { onOpenCharacter(content.characterId) })
            .semantics(mergeDescendants = true) {}
            .padding(vertical = spacing.space1),
        horizontalArrangement = Arrangement.spacedBy(spacing.space3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CharacterAvatar(name = content.name, imageUrl = content.imageUrl, size = TopBarAvatarSize)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = content.name,
                style = AssembleTheme.typography.body.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            AiLabelChip()
        }
    }
}

/** Mais recente embaixo: lista invertida, com o "digitando" no fim. */
@Composable
private fun MessageList(content: ConversationUiState.Content, onRetryMessage: (String) -> Unit) {
    val spacing = AssembleTheme.spacing
    val newestFirst = content.messages.asReversed()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        reverseLayout = true,
        contentPadding = PaddingValues(spacing.space4),
        verticalArrangement = Arrangement.spacedBy(spacing.space3),
    ) {
        if (content.typing) {
            item(key = TypingItemKey) { TypingIndicator() }
        }
        items(newestFirst, key = { it.id }) { message -> MessageItem(message, onRetryMessage) }
    }
}

@Composable
private fun MessageItem(message: Message, onRetry: (String) -> Unit) {
    val author = if (message.author == MessageAuthor.User) ChatAuthor.User else ChatAuthor.Ai
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (author == ChatAuthor.User) Alignment.End else Alignment.Start,
    ) {
        ChatBubble(
            text = message.text,
            author = author,
            modifier = Modifier.alpha(if (message.status == MessageStatus.Sending) SendingAlpha else 1f),
        )
        if (message.status == MessageStatus.Failed) FailedMessageRetry(onClick = { onRetry(message.id) })
    }
}

/** Erro inline da mensagem que não saiu. */
@Composable
private fun FailedMessageRetry(onClick: () -> Unit) {
    val colors = AssembleTheme.colors
    TextButton(onClick = onClick) {
        Icon(AssembleIcons.Error, contentDescription = null, tint = colors.error, modifier = Modifier.size(ErrorIconSize))
        Text(
            text = stringResource(R.string.state_try_again),
            style = AssembleTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
            color = colors.error,
            modifier = Modifier.padding(start = AssembleTheme.spacing.space1),
        )
    }
}

@Composable
private fun MessageInput(draft: String, onDraftChange: (String) -> Unit, onSend: () -> Unit) {
    val colors = AssembleTheme.colors
    val spacing = AssembleTheme.spacing
    val canSend = draft.isNotBlank()
    Column(
        Modifier
            .background(colors.surface)
            .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime)),
    ) {
        HorizontalDivider(color = colors.border)
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.space4, vertical = spacing.space2),
            horizontalArrangement = Arrangement.spacedBy(spacing.space2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = onDraftChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text(stringResource(R.string.conversation_input_placeholder)) },
                shape = AssembleTheme.shapes.md,
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { if (canSend) onSend() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.actionAssemble,
                    unfocusedBorderColor = colors.border,
                    cursorColor = colors.actionAssemble,
                ),
            )
            IconButton(onClick = onSend, enabled = canSend) {
                Icon(
                    imageVector = AssembleIcons.Send,
                    contentDescription = stringResource(R.string.conversation_send),
                    tint = if (canSend) colors.accentText else colors.textMuted,
                )
            }
        }
    }
}

private val PreviewNow: Instant = Instant.parse("2026-09-30T15:00:00Z")

@PreviewLightDark
@Composable
private fun ConversationPreview() {
    AssembleTheme {
        ConversationScreen(
            state = ConversationUiState.Content(
                characterId = "spider-man",
                name = "Spider-Man",
                imageUrl = null,
                messages = listOf(
                    Message("1", "c", MessageAuthor.Character, "Hey! Friendly neighborhood check-in.", PreviewNow),
                    Message("2", "c", MessageAuthor.User, "What got you into science?", PreviewNow),
                    Message("3", "c", MessageAuthor.User, "Still there?", PreviewNow, status = MessageStatus.Failed),
                ),
                typing = true,
            ),
            draft = "",
            onDraftChange = {}, onSend = {}, onRetryMessage = {}, onReload = {}, onBack = {}, onOpenCharacter = {},
        )
    }
}
