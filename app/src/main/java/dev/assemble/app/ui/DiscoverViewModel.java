package dev.assemble.app.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.firestore.ListenerRegistration;

import dev.assemble.app.data.model.Decision;
import dev.assemble.app.data.remote.ContractDto;
import dev.assemble.app.data.repository.Callback;
import dev.assemble.app.data.repository.CharacterRepository;
import dev.assemble.app.data.repository.DecisionRepository;

/**
 * Discover (tela do Vetor; aqui só estado + dados).
 * DECISÕES REPETIDAS NÃO DUPLICAM CONEXÃO: submit é idempotente
 * (doc id = characterId); enquanto um submit está em voo, novos são ignorados.
 * Sem auto-abrir conversa (contrato §3 PENDENTE).
 */
public class DiscoverViewModel extends ViewModel {
    private final CharacterRepository characters;
    private final DecisionRepository decisions;

    private final MutableLiveData<ContractDto.CharacterPage> page = new MutableLiveData<>();
    private final MutableLiveData<Decision> lastDecision = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> busy = new MutableLiveData<>(false);

    private ListenerRegistration decisionsListener;
    private boolean submitting;

    public DiscoverViewModel(CharacterRepository characters, DecisionRepository decisions) {
        this.characters = characters;
        this.decisions = decisions;
    }

    public LiveData<ContractDto.CharacterPage> getPage() {
        return page;
    }

    public LiveData<Decision> getLastDecision() {
        return lastDecision;
    }

    public LiveData<String> getError() {
        return error;
    }

    public LiveData<Boolean> getBusy() {
        return busy;
    }

    public void loadPage(String cursor) {
        busy.postValue(true);
        characters.loadPage(cursor, new Callback<ContractDto.CharacterPage>() {
            @Override
            public void onSuccess(ContractDto.CharacterPage value) {
                busy.postValue(false);
                page.postValue(value);
            }

            @Override
            public void onError(Exception e) {
                busy.postValue(false);
                error.postValue(e.getMessage());
            }
        });
    }

    /** PASS | ASSEMBLE. Ignora chamadas repetidas durante o voo (sem duplicar). */
    public void submit(String characterId, String choice) {
        if (submitting) {
            return;
        }
        submitting = true;
        busy.postValue(true);
        decisions.submit(characterId, choice, new Callback<Decision>() {
            @Override
            public void onSuccess(Decision value) {
                submitting = false;
                busy.postValue(false);
                lastDecision.postValue(value);
            }

            @Override
            public void onError(Exception e) {
                submitting = false;
                busy.postValue(false);
                error.postValue(e.getMessage());
            }
        });
    }

    /** Um listener por tela; remover antes de recriar (sem duplicados). */
    public void observeDecisions(String uid) {
        stopObserving();
        decisionsListener = decisions.observe(uid, (snapshot, e) -> {
            if (e != null) {
                error.postValue(e.getMessage());
            }
        });
    }

    public void stopObserving() {
        if (decisionsListener != null) {
            decisionsListener.remove();
            decisionsListener = null;
        }
    }

    @Override
    protected void onCleared() {
        stopObserving();
    }
}
