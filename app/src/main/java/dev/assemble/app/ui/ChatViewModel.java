package dev.assemble.app.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

import dev.assemble.app.data.model.ChatMessage;
import dev.assemble.app.data.repository.Callback;
import dev.assemble.app.data.repository.ChatRepository;

/**
 * Chat (tela do Vetor; aqui só estado + dados).
 * Envia texto do usuário; resposta de IA chega via listener (gravada pelo backend).
 * Cliente nunca cria resposta de IA localmente.
 */
public class ChatViewModel extends ViewModel {
    private final ChatRepository chat;

    private final MutableLiveData<List<ChatMessage>> messages = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> sending = new MutableLiveData<>(false);

    private ListenerRegistration messagesListener;

    public ChatViewModel(ChatRepository chat) {
        this.chat = chat;
    }

    public LiveData<List<ChatMessage>> getMessages() {
        return messages;
    }

    public LiveData<String> getError() {
        return error;
    }

    public LiveData<Boolean> getSending() {
        return sending;
    }

    public void send(String characterId, String text) {
        sending.postValue(true);
        chat.send(characterId, text, new Callback<ChatMessage>() {
            @Override
            public void onSuccess(ChatMessage value) {
                sending.postValue(false);
                // Resposta de IA vem pelo listener; nada a criar aqui.
            }

            @Override
            public void onError(Exception e) {
                sending.postValue(false);
                error.postValue(e.getMessage());
            }
        });
    }

    /** Um listener por conversa; remover no ciclo de vida (sem duplicados). */
    public void observe(String uid, String characterId) {
        stopObserving();
        messagesListener = chat.observe(uid, characterId, (snapshot, e) -> {
            if (e != null) {
                error.postValue(e.getMessage());
                return;
            }
            messages.postValue(ChatRepository.toMessages(snapshot));
        });
    }

    public void stopObserving() {
        if (messagesListener != null) {
            messagesListener.remove();
            messagesListener = null;
        }
    }

    @Override
    protected void onCleared() {
        stopObserving();
    }
}
