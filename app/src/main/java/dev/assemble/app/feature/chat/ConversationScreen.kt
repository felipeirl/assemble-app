package dev.assemble.app.feature.chat

import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import dev.assemble.app.core.feedback.LocalFeedback
import dev.assemble.app.core.feedback.Cue
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalResources
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
import dev.assemble.app.core.designsystem.component.BubblePopIn
import dev.assemble.app.core.designsystem.component.CharacterAvatar
import dev.assemble.app.core.designsystem.component.ChatAuthor
import dev.assemble.app.core.designsystem.component.ChatBubble
import dev.assemble.app.core.designsystem.component.StateView
import dev.assemble.app.core.designsystem.component.StateViewType
import dev.assemble.app.core.designsystem.component.TypingIndicator
import dev.assemble.app.core.designsystem.component.scaledTopBarHeight
import dev.assemble.app.core.designsystem.icon.AssembleIcons
import dev.assemble.app.core.designsystem.theme.AssembleTheme
import dev.assemble.app.core.designsystem.theme.rememberAnimationsEnabled
import dev.assemble.app.core.model.Message
import dev.assemble.app.core.model.MessageAuthor
import dev.assemble.app.core.model.MessageStatus
import java.time.Instant

private const val SendingAlpha = 0.6f
private const val TypingItemKey = "typing"

// Até este item a partir do fim, o usuário está "no fim da conversa" e acompanha o que chega.
private const val FollowNewestWithin = 2
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
        onSuggestion = viewModel::sendSuggestion,
        onRetryMessage = viewModel::retry,
        onRegenerate = viewModel::regenerate,
        onRewind = viewModel::rewindTo,
        onNoticeShown = viewModel::onNoticeShown,
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
    onSuggestion: (text: String) -> Unit = {},
    onRegenerate: () -> Unit = {},
    onRewind: (messageId: String) -> Unit = {},
    onNoticeShown: () -> Unit = {},
) {
    val colors = AssembleTheme.colors
    val content = state as? ConversationUiState.Content
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current
    var rewindTarget by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(content?.notice) {
        val notice = content?.notice ?: return@LaunchedEffect
        val text = when (notice) {
            ConversationNotice.ActionFailed -> resources.getString(R.string.conversation_action_failed)
        }
        snackbarHostState.showSnackbar(text)
        onNoticeShown()
    }
    Scaffold(
        modifier = modifier,
        containerColor = colors.bg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
            if (content != null) {
                MessageInput(
                    draft = draft,
                    onDraftChange = onDraftChange,
                    onSend = onSend,
                    // Some enquanto o personagem digita ou você já começou a escrever.
                    suggestions = if (content.typing || draft.isNotEmpty()) emptyList() else content.suggestions,
                    onSuggestion = onSuggestion,
                )
            }
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
                is ConversationUiState.Content -> MessageList(state, onRetryMessage, onRegenerate) { rewindTarget = it }
            }
        }
    }
    rewindTarget?.let { messageId ->
        RewindDialog(
            onConfirm = {
                rewindTarget = null
                onRewind(messageId)
            },
            onDismiss = { rewindTarget = null },
        )
    }
}

@Composable
private fun RewindDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val colors = AssembleTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = { Text(stringResource(R.string.conversation_rewind_title), color = colors.text) },
        text = { Text(stringResource(R.string.conversation_rewind_message), color = colors.textMuted) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.conversation_rewind_confirm), color = colors.error, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel), color = colors.text) }
        },
    )
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

/**
 * Mais recente embaixo: lista invertida, com o "digitando" no fim.
 * Só mensagens que chegam com a conversa aberta saltam; o histórico aparece parado.
 */
@Composable
private fun MessageList(
    content: ConversationUiState.Content,
    onRetryMessage: (String) -> Unit,
    onRegenerate: () -> Unit,
    onRewindRequest: (String) -> Unit,
) {
    val spacing = AssembleTheme.spacing
    val newestFirst = content.messages.asReversed()
    // Só a última resposta do personagem pode ser gerada de novo, e não enquanto ele digita.
    val regenerableId = content.messages.lastOrNull()
        ?.takeIf { it.author == MessageAuthor.Character && it.status == MessageStatus.Sent && !content.typing }?.id
    val animationsEnabled = rememberAnimationsEnabled()
    val shownIds = remember { content.messages.mapTo(mutableSetOf()) { it.id } }
    val feedback = LocalFeedback.current
    val listState = rememberLazyListState()
    // Na lista invertida, o Compose segura o item que estava embaixo e o novo nasce fora da tela.
    // Desce até o mais recente quando o próprio usuário envia, ou quando ele já estava no fim.
    val newestKey: Any? = if (content.typing) TypingItemKey else newestFirst.firstOrNull()?.id
    LaunchedEffect(newestKey) {
        if (newestKey == null) return@LaunchedEffect
        val sentByUser = !content.typing && newestFirst.first().author == MessageAuthor.User
        if (sentByUser || listState.firstVisibleItemIndex <= FollowNewestWithin) {
            listState.animateScrollToItem(0)
        }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        reverseLayout = true,
        contentPadding = PaddingValues(spacing.space4),
        verticalArrangement = Arrangement.spacedBy(spacing.space3),
    ) {
        if (content.typing) {
            item(key = TypingItemKey) {
                BubblePopIn(ChatAuthor.Ai, animate = animationsEnabled, modifier = Modifier.animateItem()) {
                    TypingIndicator()
                }
            }
        }
        items(newestFirst, key = { it.id }) { message ->
            val isNew = remember(message.id) { shownIds.add(message.id) }
            // Só mensagens que chegam com a conversa aberta dão retorno; o histórico não.
            if (isNew) {
                LaunchedEffect(message.id) {
                    feedback.play(if (message.author == MessageAuthor.User) Cue.MessageOut else Cue.MessageIn)
                }
            }
            var wasSending by remember(message.id) { mutableStateOf(message.status == MessageStatus.Sending) }
            LaunchedEffect(message.status) {
                val rejected = message.status == MessageStatus.Failed || message.status == MessageStatus.Blocked
                if (rejected && wasSending) feedback.play(Cue.Error)
                wasSending = message.status == MessageStatus.Sending
            }
            BubblePopIn(message.author.toChatAuthor(), animate = animationsEnabled && isNew, modifier = Modifier.animateItem()) {
                MessageItem(message, message.id == regenerableId, onRetryMessage, onRegenerate, onRewindRequest)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageItem(
    message: Message,
    canRegenerate: Boolean,
    onRetry: (String) -> Unit,
    onRegenerate: () -> Unit,
    onRewindRequest: (String) -> Unit,
) {
    val author = message.author.toChatAuthor()
    var menuOpen by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (author == ChatAuthor.User) Alignment.End else Alignment.Start,
    ) {
        // Recusada pelo servidor, a mensagem volta sem texto: só o aviso aparece.
        if (message.text.isNotBlank()) {
            val dimmed = message.status == MessageStatus.Sending || message.status == MessageStatus.Blocked
            // Segurar numa resposta do personagem abre o menu (voltar a conversa, gerar outra).
            val menuModifier = if (author == ChatAuthor.Ai) {
                Modifier.combinedClickable(
                    onClick = {},
                    onLongClick = { menuOpen = true },
                    onLongClickLabel = stringResource(R.string.conversation_message_options),
                )
            } else {
                Modifier
            }
            Box {
                ChatBubble(
                    text = message.text,
                    author = author,
                    modifier = Modifier.alpha(if (dimmed) SendingAlpha else 1f).then(menuModifier),
                )
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    if (canRegenerate) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.conversation_regenerate)) },
                            onClick = {
                                menuOpen = false
                                onRegenerate()
                            },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.conversation_rewind)) },
                        onClick = {
                            menuOpen = false
                            onRewindRequest(message.id)
                        },
                    )
                }
            }
        }
        when (message.status) {
            MessageStatus.Failed -> FailedMessageRetry(onClick = { onRetry(message.id) })
            MessageStatus.Blocked -> BlockedMessageNotice()
            MessageStatus.Sending, MessageStatus.Sent -> Unit
        }
        if (canRegenerate) RegenerateButton(onRegenerate)
    }
}

/** Atalho para gerar outra resposta, logo abaixo da última do personagem. */
@Composable
private fun RegenerateButton(onClick: () -> Unit) {
    val colors = AssembleTheme.colors
    TextButton(onClick = onClick) {
        Icon(AssembleIcons.Reset, contentDescription = null, tint = colors.textMuted, modifier = Modifier.size(ErrorIconSize))
        Text(
            text = stringResource(R.string.conversation_regenerate),
            style = AssembleTheme.typography.caption,
            color = colors.textMuted,
            modifier = Modifier.padding(start = AssembleTheme.spacing.space1),
        )
    }
}

/** Mensagem recusada pelo filtro de segurança: não tem nova tentativa, só o aviso. */
@Composable
private fun BlockedMessageNotice() {
    val colors = AssembleTheme.colors
    Row(
        modifier = Modifier.padding(vertical = AssembleTheme.spacing.space1),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(AssembleIcons.Error, contentDescription = null, tint = colors.textMuted, modifier = Modifier.size(ErrorIconSize))
        Text(
            text = stringResource(R.string.chat_message_blocked),
            style = AssembleTheme.typography.caption,
            color = colors.textMuted,
            modifier = Modifier.padding(start = AssembleTheme.spacing.space1),
        )
    }
}

private fun MessageAuthor.toChatAuthor(): ChatAuthor =
    if (this == MessageAuthor.User) ChatAuthor.User else ChatAuthor.Ai

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
private fun MessageInput(
    draft: String,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    suggestions: List<ReplySuggestion>,
    onSuggestion: (String) -> Unit,
) {
    val colors = AssembleTheme.colors
    val spacing = AssembleTheme.spacing
    val canSend = draft.isNotBlank()
    Column(
        Modifier
            .background(colors.surface)
            .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime)),
    ) {
        HorizontalDivider(color = colors.border)
        if (suggestions.isNotEmpty()) SuggestionChips(suggestions, onSuggestion)
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
                typing = false,
                suggestions = listOf(
                    ReplySuggestion(R.string.chat_suggest_mission),
                    ReplySuggestion(R.string.chat_suggest_free_time),
                    ReplySuggestion(R.string.chat_suggest_advice),
                ),
            ),
            draft = "",
            onDraftChange = {}, onSend = {}, onRetryMessage = {}, onReload = {}, onBack = {}, onOpenCharacter = {},
        )
    }
}
